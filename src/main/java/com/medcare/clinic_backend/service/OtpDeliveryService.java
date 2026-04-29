package com.medcare.clinic_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(OtpDeliveryService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider-url:}")
    private String smsProviderUrl;

    @Value("${app.sms.api-key:}")
    private String smsApiKey;

    @Value("${app.otp.phone-dev-mode:false}")
    private boolean phoneDevMode;

    @Value("${app.otp.expose-dev-otp:false}")
    private boolean exposeDevOtp;

    public Map<String, String> sendPasswordResetOtp(String destination, String otp, boolean emailChannel) {
        if (emailChannel) {
            sendEmailOtp(destination, otp);
            return Map.of(
                    "message", "OTP da duoc gui den Gmail cua ban.",
                    "channel", "EMAIL"
            );
        }

        if (smsEnabled && smsProviderUrl != null && !smsProviderUrl.isBlank()) {
            sendSmsOtp(destination, otp);
            return Map.of(
                    "message", "OTP da duoc gui den so dien thoai cua ban.",
                    "channel", "SMS"
            );
        }

        if (phoneDevMode) {
            log.info("Phone OTP requested in dev mode for {}", destination);
            Map<String, String> response = new LinkedHashMap<>();
            response.put("channel", "PHONE_DEV");
            if (exposeDevOtp) {
                response.put("message", "Che do do an: chua gui SMS that. OTP duoc tra ve de test.");
                response.put("otp", otp);
            } else {
                response.put("message", "Che do do an: chua gui SMS that. OTP khong tra ve response.");
            }
            return response;
        }

        return Map.of(
                "message", "He thong chua cau hinh SMS. Vui long dung Gmail de khoi phuc mat khau.",
                "channel", "SMS_UNAVAILABLE"
        );
    }

    private void sendEmailOtp(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Ma OTP khoi phuc mat khau - MedCare");
        message.setText("Ma OTP khoi phuc mat khau cua ban la: " + otp + ". Hieu luc trong 5 phut.");
        mailSender.send(message);
    }

    private void sendSmsOtp(String phoneNumber, String otp) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("to", phoneNumber);
        payload.put("otp", otp);
        payload.put("message", "Ma OTP khoi phuc mat khau MedCare cua ban la " + otp + ". Hieu luc trong 5 phut.");
        if (smsApiKey != null && !smsApiKey.isBlank()) {
            payload.put("apiKey", smsApiKey);
        }
        restTemplate.postForEntity(smsProviderUrl, payload, Void.class);
    }
}
