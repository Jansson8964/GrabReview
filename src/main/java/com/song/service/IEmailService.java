package com.song.service;

public interface IEmailService {
    void sendEmail(String to, String subject, String content);

    String generateVerificationCode();

}
