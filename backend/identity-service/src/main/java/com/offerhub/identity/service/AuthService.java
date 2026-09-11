package com.offerhub.identity.service;

import com.offerhub.identity.dto.*;
import com.offerhub.identity.entity.RefreshToken;
import com.offerhub.identity.entity.StaffUser;
import com.offerhub.identity.entity.Subscriber;
import com.offerhub.identity.exception.AccountLockedException;
import com.offerhub.identity.exception.DuplicateResourceException;
import com.offerhub.identity.exception.InvalidCredentialsException;
import com.offerhub.identity.exception.InvalidOtpException;
import com.offerhub.identity.exception.NotFoundException;
import com.offerhub.identity.repository.RefreshTokenRepository;
import com.offerhub.identity.repository.StaffUserRepository;
import com.offerhub.identity.repository.SubscriberRepository;
import com.offerhub.identity.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;
    private static final long RESET_CODE_VALID_MINUTES = 15;
    private static final SecureRandom RESET_CODE_RANDOM = new SecureRandom();

    private final SubscriberRepository subscriberRepository;
    private final Map<String, PhoneVerificationStrategy> verificationStrategies;
    private final JwtService jwtService;
    private final StaffUserRepository staffUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public RegisterResponse register(RegisterRequest request) {
        if (subscriberRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new DuplicateResourceException("Bu telefon numarası zaten kayıtlı.");
        }

        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName(request.getFirstName());
        subscriber.setLastName(request.getLastName());
        subscriber.setPhone(request.getPhone());
        subscriber.setEmail(request.getEmail());

        PhoneVerificationStrategy strategy = resolveStrategy(request.getAuthMode());
        strategy.initiate(subscriber);

        Subscriber saved = subscriberRepository.save(subscriber);

        return new RegisterResponse(saved.getId().toString(), true);
    }

    public OtpRequestResponse requestOtp(OtpRequestRequest request) {
        Subscriber subscriber = subscriberRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new InvalidOtpException("Telefon numarasi bulunamadi"));

        PhoneVerificationStrategy strategy = resolveStrategy(request.getAuthMode());
        strategy.initiate(subscriber);
        subscriberRepository.save(subscriber);

        return new OtpRequestResponse(true);
    }

    private PhoneVerificationStrategy resolveStrategy(AuthMode mode) {
        PhoneVerificationStrategy strategy = verificationStrategies.get(mode.name());
        if (strategy == null) {
            throw new IllegalStateException("Desteklenmeyen auth modu: " + mode);
        }
        return strategy;
    }

    public AuthDataResponse verifyOtp(OtpVerifyRequest request) {
        PhoneVerificationStrategy strategy = resolveStrategy(request.getAuthMode());
        String verifiedPhone = strategy.verify(request.getPhone(), request.getCredential());

        Subscriber subscriber = subscriberRepository.findByPhone(verifiedPhone)
                .orElseThrow(() -> new InvalidOtpException("Abone bulunamadi"));

        TokenPair tokens = issueTokens(subscriber.getId(), "SUBSCRIBER");
        AuthUserResponse user = new AuthUserResponse(
                subscriber.getId().toString(), subscriber.getFirstName(), subscriber.getLastName(),
                "SUBSCRIBER", List.of(), List.of(), false);

        return new AuthDataResponse(tokens.accessToken(), tokens.refreshToken(), jwtService.getAccessTokenExpirySeconds(), user);
    }
    public AuthDataResponse staffLogin(StaffLoginRequest request, String ipAddress) {
        var staffOpt = staffUserRepository.findByEmail(request.getEmail());
        if (staffOpt.isEmpty()) {
            auditLogService.record(request.getEmail(), "LOGIN_FAILED", "FAILED", ipAddress, "Personel bulunamadi");
            throw new InvalidCredentialsException("E-posta veya sifre hatali");
        }
        StaffUser staff = staffOpt.get();

        if (staff.getLockedUntil() != null && staff.getLockedUntil().isAfter(java.time.Instant.now())) {
            auditLogService.record(staff.getId().toString(), "LOGIN_FAILED", "FAILED", ipAddress, "Hesap kilitli");
            throw new AccountLockedException("Hesap gecici olarak kilitli", staff.getLockedUntil());
        }

        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            boolean justLocked = registerFailedAttempt(staff);
            auditLogService.record(staff.getId().toString(), "LOGIN_FAILED", "FAILED", ipAddress, "Sifre hatali");
            if (justLocked) {
                auditLogService.record(staff.getId().toString(), "ACCOUNT_LOCKED", "FAILED", ipAddress,
                        "5 basarisiz denemeden sonra hesap kilitlendi");
            }
            throw new InvalidCredentialsException("E-posta veya sifre hatali");
        }

        staff.setFailedLoginAttempts(0);
        staff.setLockedUntil(null);
        staffUserRepository.save(staff);

        auditLogService.record(staff.getId().toString(), "LOGIN_SUCCESS", "SUCCESS", ipAddress, null);

        TokenPair tokens = issueTokens(staff.getId(), staff.getRole().name());

        AuthUserResponse user = new AuthUserResponse(
                staff.getId().toString(), staff.getFirstName(), staff.getLastName(), staff.getRole().name(),
                staff.getSpecialties(), staff.getRegions(), staff.isMustChangePassword()
        );

        return new AuthDataResponse(tokens.accessToken(), tokens.refreshToken(), jwtService.getAccessTokenExpirySeconds(), user);
    }

    /**
     * Spends the presented refresh token and issues a fresh access/refresh pair.
     *
     * Rotation: the presented token is marked revoked in the same call that issues its
     * replacement, so it cannot be used a second time even by its rightful owner.
     *
     * Reuse detection: if the presented token was already revoked - meaning this exact
     * call already happened once before - every other refresh token still active for that
     * user is revoked too. A legitimate client never presents the same refresh token
     * twice, so a second presentation means two different holders have it, and the only
     * safe response is to end every session and force a fresh login.
     */
    @Transactional
    public AuthDataResponse refresh(String refreshToken, String ipAddress) {
        Claims claims = parseRefreshToken(refreshToken);

        UUID tokenId = parseTokenId(claims);
        UUID userId = UUID.fromString(claims.getSubject());
        String role = claims.get("role", String.class);

        RefreshToken stored = refreshTokenRepository.findById(tokenId).orElse(null);
        if (stored == null || !stored.getUserId().equals(userId)) {
            throw new InvalidCredentialsException("Gecersiz refresh token");
        }

        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(userId);
            auditLogService.record(userId.toString(), "REFRESH_TOKEN_REUSE", "FAILED", ipAddress,
                    "Iptal edilmis bir refresh token yeniden kullanilmaya calisildi, kullanicinin tum oturumlari sonlandirildi");
            throw new InvalidCredentialsException("Refresh token gecersiz kilindi, tekrar giris yapin");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException("Refresh token suresi dolmus");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        TokenPair tokens = issueTokens(userId, role);
        AuthUserResponse user = resolveUser(userId, role);

        return new AuthDataResponse(tokens.accessToken(), tokens.refreshToken(), jwtService.getAccessTokenExpirySeconds(), user);
    }

    /**
     * Best-effort: an already-expired or malformed token has nothing to revoke, and a
     * client logging out does not need to be told that - the outcome it wants (this token
     * no longer works) is already true either way.
     */
    @Transactional
    public void logout(String refreshToken, String ipAddress) {
        Claims claims;
        try {
            claims = jwtService.parseToken(refreshToken).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            return;
        }

        if (!"refresh".equals(claims.get("type", String.class))) {
            return;
        }

        try {
            UUID tokenId = parseTokenId(claims);
            refreshTokenRepository.findById(tokenId).ifPresent(stored -> {
                stored.setRevoked(true);
                refreshTokenRepository.save(stored);
                auditLogService.record(stored.getUserId().toString(), "LOGOUT", "SUCCESS", ipAddress, null);
            });
        } catch (InvalidCredentialsException ex) {
            // Token had no usable jti - nothing to revoke.
        }
    }

    private Claims parseRefreshToken(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseToken(refreshToken).getPayload();
        } catch (ExpiredJwtException ex) {
            throw new InvalidCredentialsException("Refresh token suresi dolmus");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Gecersiz refresh token");
        }

        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new InvalidCredentialsException("Bu bir refresh token degil");
        }
        return claims;
    }

    private UUID parseTokenId(Claims claims) {
        try {
            return UUID.fromString(claims.getId());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidCredentialsException("Gecersiz refresh token");
        }
    }

    private AuthUserResponse resolveUser(UUID userId, String role) {
        if ("SUBSCRIBER".equals(role)) {
            Subscriber subscriber = subscriberRepository.findById(userId)
                    .orElseThrow(() -> new InvalidCredentialsException("Kullanici bulunamadi"));
            return new AuthUserResponse(
                    userId.toString(), subscriber.getFirstName(), subscriber.getLastName(),
                    role, List.of(), List.of(), false);
        }
        StaffUser staff = staffUserRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Kullanici bulunamadi"));
        return new AuthUserResponse(
                userId.toString(), staff.getFirstName(), staff.getLastName(), role,
                staff.getSpecialties(), staff.getRegions(), staff.isMustChangePassword());
    }

    /**
     * Mints an access token plus a brand-new refresh token, persisting the refresh token's
     * row before handing it back - the row has to exist before the token could possibly be
     * presented to /refresh.
     */
    private TokenPair issueTokens(UUID userId, String role) {
        UUID tokenId = UUID.randomUUID();
        String refreshToken = jwtService.generateRefreshToken(userId.toString(), role, tokenId.toString());

        refreshTokenRepository.save(RefreshToken.builder()
                .id(tokenId)
                .userId(userId)
                .role(role)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenExpirySeconds()))
                .revoked(false)
                .build());

        String accessToken = jwtService.generateAccessToken(userId.toString(), role);
        return new TokenPair(accessToken, refreshToken);
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }

    /**
     * Sifre unuttum akisinin ilk adimi. Aboneler icin telefona giden OTP'nin (bkz.
     * MockPhoneVerification) personel/e-posta karsiligi: 6 haneli bir kod uretilir, hash'i
     * ve son gecerlilik zamani StaffUser uzerinde tutulur, gercek gonderim yerine (SMTP
     * entegrasyonu yok) log'a yazilir - projedeki OTP simulasyonuyla ayni yaklasim.
     */
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        StaffUser staff = staffUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("Bu e-posta ile kayitli bir personel bulunamadi"));

        String code = generateResetCode();
        staff.setResetCodeHash(passwordEncoder.encode(code));
        staff.setResetCodeExpiresAt(Instant.now().plus(RESET_CODE_VALID_MINUTES, ChronoUnit.MINUTES));
        staffUserRepository.save(staff);

        log.info(">>> SIFRE SIFIRLAMA KODU GONDERILDI (simulasyon) -> e-posta: {}, kod: {}",
                staff.getEmail(), code);

        return new ForgotPasswordResponse(true);
    }

    /**
     * Kodu ve yeni sifreyi dogrulayip sifreyi degistirir. Basarili sifirlama, o hesabin
     * tum aktif refresh token'larini iptal eder - kod calindiysa dahi eski oturumlar
     * gecersiz kilinmis olur, tipki sifre degistirmede oldugu gibi.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        StaffUser staff = staffUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("Bu e-posta ile kayitli bir personel bulunamadi"));

        if (staff.getResetCodeExpiresAt() == null || staff.getResetCodeExpiresAt().isBefore(Instant.now())) {
            throw new InvalidOtpException("Kodun suresi dolmus, yeniden talep edin");
        }
        if (staff.getResetCodeHash() == null || !passwordEncoder.matches(request.getCode(), staff.getResetCodeHash())) {
            throw new InvalidOtpException("Kod hatali");
        }

        staff.setPassword(passwordEncoder.encode(request.getNewPassword()));
        staff.setResetCodeHash(null);
        staff.setResetCodeExpiresAt(null);
        staff.setFailedLoginAttempts(0);
        staff.setLockedUntil(null);
        staffUserRepository.save(staff);

        refreshTokenRepository.revokeAllForUser(staff.getId());
    }

    private String generateResetCode() {
        int code = 100000 + RESET_CODE_RANDOM.nextInt(900000); // 6 haneli
        return String.valueOf(code);
    }

    public void changePassword(String staffId, ChangePasswordRequest request) {
        StaffUser staff = staffUserRepository.findById(java.util.UUID.fromString(staffId))
                .orElseThrow(() -> new InvalidCredentialsException("Kullanici bulunamadi"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), staff.getPassword())) {
            throw new InvalidCredentialsException("Mevcut sifre hatali");
        }

        staff.setPassword(passwordEncoder.encode(request.getNewPassword()));
        staff.setMustChangePassword(false);
        staffUserRepository.save(staff);
    }

    private boolean registerFailedAttempt(StaffUser staff) {
        int attempts = staff.getFailedLoginAttempts() + 1;
        staff.setFailedLoginAttempts(attempts);
        boolean justLocked = false;
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            staff.setLockedUntil(java.time.Instant.now().plus(LOCK_DURATION_MINUTES, java.time.temporal.ChronoUnit.MINUTES));
            justLocked = true;
        }
        staffUserRepository.save(staff);
        return justLocked;
    }
}