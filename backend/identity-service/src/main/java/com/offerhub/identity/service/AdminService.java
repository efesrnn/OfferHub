package com.offerhub.identity.service;

import com.offerhub.identity.dto.StaffCreateRequest;
import com.offerhub.identity.dto.StaffCreateResponse;
import com.offerhub.identity.entity.Role;
import com.offerhub.identity.entity.StaffUser;
import com.offerhub.identity.exception.DuplicateResourceException;
import com.offerhub.identity.repository.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminService {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 10;

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffCreateResponse createStaff(StaffCreateRequest request) {
        if (staffUserRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Bu e-posta adresi zaten kullaniliyor");
        }

        String tempPassword = generateTempPassword();

        StaffUser staff = StaffUser.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .role(Role.valueOf(request.getRole()))
                .specialties(request.getSpecialties())
                .regions(request.getRegions())
                .mustChangePassword(true)
                .build();

        StaffUser saved = staffUserRepository.save(staff);

        // MOCK: gercek SMTP entegrasyonu yerine konsola loglaniyor (OTP'deki
        // LogOtpSender ile ayni yaklasim). Ileride gercek maile cevrilebilir.
        log.info("[TEMP PASSWORD] {} icin gecici sifre: {}", saved.getEmail(), tempPassword);

        return new StaffCreateResponse(saved.getId().toString(), true);
    }

    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
