package com.offerhub.identity.service;

import com.offerhub.identity.entity.Subscriber;

public interface PhoneVerificationStrategy {

    /**
     * Dogrulama surecini baslatir.
     * MOCK: kod uretir, hash'ler, subscriber uzerine yazar (henuz DB'ye kaydetmez).
     * FIREBASE: hicbir sey yapmaz - mobil, Firebase SDK'siyla kendi basina halleder.
     */
    void initiate(Subscriber subscriber);

    /**
     * Verilen credential'i dogrular, dogrulanan gercek telefon numarasini dondurur.
     * MOCK: credential = OTP kodu, DB'deki hash ile karsilastirir.
     * FIREBASE: credential = Firebase ID token, token'in icinden telefonu cikarir.
     * Gecersizse InvalidOtpException firlatir.
     */
    String verify(String phone, String credential);
}