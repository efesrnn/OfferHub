package com.offerhub.identity.service;

import com.offerhub.identity.entity.Subscriber;
import com.offerhub.identity.exception.InvalidOtpException;
import com.offerhub.identity.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
@Component("MOCK")
public class MockPhoneVerification implements PhoneVerificationStrategy {

    private final OtpSender otpSender;
    private final PasswordEncoder passwordEncoder;
    private final SubscriberRepository subscriberRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void initiate(Subscriber subscriber) {
        String code = generateCode();
        subscriber.setOtpCodeHash(passwordEncoder.encode(code));
        subscriber.setOtpExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        otpSender.send(subscriber.getPhone(), code);
    }

    @Override
    public String verify(String phone, String credential) {
        Subscriber subscriber = subscriberRepository.findByPhone(phone)
                .orElseThrow(() -> new InvalidOtpException("Telefon numarasi bulunamadi"));

        if (subscriber.getOtpExpiresAt() == null || subscriber.getOtpExpiresAt().isBefore(Instant.now())) {
            throw new InvalidOtpException("OTP kodunun suresi dolmus");
        }

        if (!passwordEncoder.matches(credential, subscriber.getOtpCodeHash())) {
            throw new InvalidOtpException("OTP kodu hatali");
        }

        return phone;
    }

    private String generateCode() {
        int code = 1000 + RANDOM.nextInt(9000); // 1000-9999 arasi 4 haneli
        return String.valueOf(code);
    }
}