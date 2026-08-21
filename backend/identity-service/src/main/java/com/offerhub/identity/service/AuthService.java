package com.offerhub.identity.service;

import com.offerhub.identity.dto.*;
import com.offerhub.identity.entity.Subscriber;
import com.offerhub.identity.exception.DuplicateResourceException;
import com.offerhub.identity.exception.InvalidOtpException;
import com.offerhub.identity.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final SubscriberRepository subscriberRepository;
    private final Map<String, PhoneVerificationStrategy> verificationStrategies;

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

    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        PhoneVerificationStrategy strategy = resolveStrategy(request.getAuthMode());
        String verifiedPhone = strategy.verify(request.getPhone(), request.getCredential());

        Subscriber subscriber = subscriberRepository.findByPhone(verifiedPhone)
                .orElseThrow(() -> new InvalidOtpException("Abone bulunamadı"));

        return new OtpVerifyResponse(
                subscriber.getId().toString(),
                subscriber.getFirstName(),
                subscriber.getPhone()
        );
    }
}