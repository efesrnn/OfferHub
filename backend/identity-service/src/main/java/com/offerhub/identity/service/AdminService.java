package com.offerhub.identity.service;

import com.offerhub.identity.dto.StaffCreateRequest;
import com.offerhub.identity.dto.StaffCreateResponse;
import com.offerhub.identity.dto.StaffResponse;
import com.offerhub.identity.entity.Role;
import com.offerhub.identity.entity.StaffUser;
import com.offerhub.identity.exception.DuplicateResourceException;
import com.offerhub.identity.exception.NotFoundException;
import com.offerhub.identity.exception.ValidationException;
import com.offerhub.identity.repository.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminService {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 10;

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public StaffCreateResponse createStaff(StaffCreateRequest request, String ipAddress) {
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

        auditLogService.record(saved.getId().toString(), "STAFF_CREATED", "SUCCESS", ipAddress,
                saved.getFirstName() + " " + saved.getLastName() + " (" + saved.getRole() + ") olusturuldu");

        return new StaffCreateResponse(saved.getId().toString(), true, tempPassword);
    }

    public List<StaffResponse> searchStaff(String query) {
        List<StaffUser> results = (query == null || query.isBlank())
                ? staffUserRepository.findAll()
                : staffUserRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        query, query, query);
        return results.stream().map(StaffResponse::from).toList();
    }

    public StaffResponse findStaff(String staffId) {
        return StaffResponse.from(loadStaff(staffId));
    }

    public StaffResponse updateRole(String staffId, String newRole, String ipAddress) {
        StaffUser staff = loadStaff(staffId);

        Role role;
        try {
            role = Role.valueOf(newRole);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Gecersiz rol: " + newRole);
        }
        if (staff.getRole() == role) {
            throw new ValidationException("Personel zaten bu role sahip");
        }

        Role previousRole = staff.getRole();
        staff.setRole(role);
        StaffUser saved = staffUserRepository.save(staff);

        auditLogService.record(saved.getId().toString(), "ROLE_UPDATED", "SUCCESS", ipAddress,
                previousRole + " -> " + role);

        return StaffResponse.from(saved);
    }

    private StaffUser loadStaff(String staffId) {
        try {
            return staffUserRepository.findById(java.util.UUID.fromString(staffId))
                    .orElseThrow(() -> new NotFoundException("Personel bulunamadi"));
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Gecersiz personel id");
        }
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
