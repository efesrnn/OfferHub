package com.offerhub.identity.config;

import com.offerhub.identity.entity.Role;
import com.offerhub.identity.entity.StaffUser;
import com.offerhub.identity.repository.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Sistemde hic ADMIN yoksa ilk admini burada olustururuz - aksi halde admin acacak
 * kimse olmaz (personel olusturma endpoint'i zaten ADMIN rolu istiyor, tavuk-yumurta).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed-email}")
    private String adminEmail;

    @Value("${admin.seed-password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (staffUserRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        StaffUser admin = StaffUser.builder()
                .firstName("Admin")
                .lastName("Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .mustChangePassword(true)
                .build();

        staffUserRepository.save(admin);

        log.info(">>> ILK ADMIN HESABI OLUSTURULDU -> e-posta: {}, sifre: {} (ilk girişte değiştirilmesi zorunlu)",
                adminEmail, adminPassword);
    }
}
