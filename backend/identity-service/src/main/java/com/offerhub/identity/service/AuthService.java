package com.offerhub.identity.service;

import com.offerhub.identity.dto.*;
import com.offerhub.identity.entity.StaffUser;
import com.offerhub.identity.entity.Subscriber;
import com.offerhub.identity.exception.AccountLockedException;
import com.offerhub.identity.exception.DuplicateResourceException;
import com.offerhub.identity.exception.InvalidCredentialsException;
import com.offerhub.identity.exception.InvalidOtpException;
import com.offerhub.identity.repository.StaffUserRepository;
import com.offerhub.identity.repository.SubscriberRepository;
import com.offerhub.identity.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Service
public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final SubscriberRepository subscriberRepository;
    private final Map<String, PhoneVerificationStrategy> verificationStrategies;
    private final JwtService jwtService;
    private final StaffUserRepository staffUserRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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

        String subscriberId = subscriber.getId().toString();
        String accessToken = jwtService.generateAccessToken(subscriberId, "SUBSCRIBER");
        String refreshToken = jwtService.generateRefreshToken(subscriberId, "SUBSCRIBER");

        AuthUserResponse user = new AuthUserResponse(subscriberId, "SUBSCRIBER", List.of(), List.of(), false);

        return new AuthDataResponse(accessToken, refreshToken, jwtService.getAccessTokenExpirySeconds(), user);
    }
    public AuthDataResponse staffLogin(StaffLoginRequest request) {
        StaffUser staff = staffUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("E-posta veya sifre hatali"));

        if (staff.getLockedUntil() != null && staff.getLockedUntil().isAfter(java.time.Instant.now())) {
            throw new AccountLockedException("Hesap gecici olarak kilitli", staff.getLockedUntil());
        }

        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            registerFailedAttempt(staff);
            throw new InvalidCredentialsException("E-posta veya sifre hatali");
        }

        staff.setFailedLoginAttempts(0);
        staff.setLockedUntil(null);
        staffUserRepository.save(staff);

        String staffId = staff.getId().toString();
        String accessToken = jwtService.generateAccessToken(staffId, staff.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(staffId, staff.getRole().name());

        AuthUserResponse user = new AuthUserResponse(
                staffId, staff.getRole().name(), staff.getSpecialties(), staff.getRegions(), staff.isMustChangePassword()
        );

        return new AuthDataResponse(accessToken, refreshToken, jwtService.getAccessTokenExpirySeconds(), user);
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

    private void registerFailedAttempt(StaffUser staff) {
        int attempts = staff.getFailedLoginAttempts() + 1;
        staff.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            staff.setLockedUntil(java.time.Instant.now().plus(LOCK_DURATION_MINUTES, java.time.temporal.ChronoUnit.MINUTES));
        }
        staffUserRepository.save(staff);
    }
}