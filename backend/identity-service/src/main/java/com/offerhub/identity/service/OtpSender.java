package com.offerhub.identity.service;

public interface OtpSender {
    void send(String phone, String code);
}