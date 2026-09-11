package com.offerhub.identity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogOtpSender implements OtpSender {

    @Value("${sms.provider.api-key}")
    private String apiKey;

    @Value("${sms.provider.api-hash}")
    private String apiHash;

    @Override
    public void send(String phone, String code) {
        log.info(">>> OTP GONDERILDI (simulasyon) -> telefon: {}, kod: {}", phone, code);

    }
}