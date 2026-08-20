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

        // --- GERCEK SAGLAYICI ORNEGI (Ileti Merkezi) - su an calismiyor, sadece referans ---
        // Gercek SMS gondermek istersek: key/hash artik yukaridaki @Value alanlarindan gelir,
        // application.yaml -> SMS_API_KEY / SMS_API_HASH environment degiskenlerinden okunur,
        // koda hicbir zaman gomulmez. docker-compose.yml'a da DB_PASSWORD gibi ekstra bir
        // environment satiri eklemek yeterli olur.
        //
        // RestTemplate restTemplate = new RestTemplate();
        // String url = "https://api.iletimerkezi.com/v1/send-sms/get/"
        //     + "?key=" + apiKey
        //     + "&hash=" + apiHash
        //     + "&text=" + URLEncoder.encode("OfferHub dogrulama kodunuz: " + code, "UTF-8")
        //     + "&receipents=" + phone;
        // restTemplate.getForObject(url, String.class);
    }
}