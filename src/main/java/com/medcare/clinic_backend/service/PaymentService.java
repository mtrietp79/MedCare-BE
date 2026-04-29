package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.config.VNPayConfig;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Value("${vnpay.hashSecret}")
    private String secretKey;

    public long resolvePaymentAmount(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay lich hen ID: " + appointmentId
                ));

        if (appointment.getConsultationFee() == null || appointment.getConsultationFee() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen chua co phi kham hop le de thanh toan.");
        }

        return Math.round(appointment.getConsultationFee());
    }

    @Transactional
    public String processVnpayReturn(Map<String, String> vnpParams, Integer appointmentId) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VNPay hash secret chua duoc cau hinh.");
        }
        if (appointmentId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu appointmentId.");
        }

        String responseCode = vnpParams.getOrDefault("vnp_ResponseCode", "");
        String vnpTxnRef = vnpParams.get("vnp_TxnRef");
        String vnpAmountRaw = vnpParams.get("vnp_Amount");
        String receivedSecureHash = vnpParams.get("vnp_SecureHash");

        if (receivedSecureHash == null || receivedSecureHash.isBlank()) {
            saveTransactionLog(vnpParams, appointmentId, "MISSING_SIGNATURE", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu chu ky bao mat tu VNPay.");
        }

        if (vnpTxnRef == null || !vnpTxnRef.equals(String.valueOf(appointmentId))) {
            saveTransactionLog(vnpParams, appointmentId, "INVALID_TXN_REF", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thong tin giao dich khong khop voi lich hen.");
        }

        if (!isValidVnpaySignature(vnpParams, receivedSecureHash)) {
            saveTransactionLog(vnpParams, appointmentId, "INVALID_SIGNATURE", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chu ky giao dich VNPay khong hop le.");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay lich hen ID: " + appointmentId
                ));

        if (appointment.getConsultationFee() == null || appointment.getConsultationFee() <= 0) {
            saveTransactionLog(vnpParams, appointmentId, "INVALID_APPOINTMENT_FEE", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen chua co phi kham hop le.");
        }

        long expectedAmount = Math.round(appointment.getConsultationFee() * 100);
        long paidAmount = parseVnpAmount(vnpAmountRaw);
        if (expectedAmount != paidAmount) {
            saveTransactionLog(vnpParams, appointmentId, "AMOUNT_MISMATCH", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So tien thanh toan khong khop voi phi kham.");
        }

        saveTransactionLog(vnpParams, appointmentId, responseCode, parseAmount(vnpAmountRaw));

        if ("00".equals(responseCode)) {
            if (!"PAID_ONLINE".equalsIgnoreCase(appointment.getPaymentStatus())) {
                appointment.setPaymentStatus("PAID_ONLINE");
                appointmentRepository.save(appointment);
            }
            return "THANH TOAN THANH CONG! Trang thai lich hen da duoc cap nhat.";
        }

        return "THANH TOAN THAT BAI HOAC BI HUY BO. Ma loi VNPay: " + responseCode;
    }

    private void saveTransactionLog(Map<String, String> vnpParams, Integer appointmentId, String responseCode, double amount) {
        String vnpTransactionNo = vnpParams.get("vnp_TransactionNo");
        if (vnpTransactionNo != null
                && !vnpTransactionNo.isBlank()
                && transactionLogRepository.existsByVnpTransactionNo(vnpTransactionNo)) {
            return;
        }

        TransactionLog log = new TransactionLog();
        log.setAppointmentId(appointmentId);
        log.setVnpTxnRef(vnpParams.get("vnp_TxnRef"));
        log.setVnpTransactionNo(vnpTransactionNo);
        log.setBankCode(vnpParams.get("vnp_BankCode"));
        log.setAmount(amount);
        log.setResponseCode(responseCode);
        transactionLogRepository.save(log);
    }

    private boolean isValidVnpaySignature(Map<String, String> vnpParams, String expectedHash) {
        Map<String, String> filteredParams = new HashMap<>();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith("vnp_")) {
                continue;
            }
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) {
                continue;
            }
            filteredParams.put(key, entry.getValue());
        }

        List<String> fieldNames = new ArrayList<>(filteredParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        boolean first = true;
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = filteredParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    hashData.append('&');
                }
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                first = false;
            }
        }

        String calculatedHash = VNPayConfig.hmacSHA512(secretKey, hashData.toString());
        return calculatedHash.equalsIgnoreCase(expectedHash);
    }

    private long parseVnpAmount(String vnpAmountRaw) {
        if (vnpAmountRaw == null || vnpAmountRaw.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong nhan duoc so tien tu VNPay.");
        }
        try {
            return Long.parseLong(vnpAmountRaw);
        } catch (NumberFormatException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So tien VNPay khong hop le.");
        }
    }

    private double parseAmount(String vnpAmountRaw) {
        if (vnpAmountRaw == null || vnpAmountRaw.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(vnpAmountRaw) / 100.0;
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }
}
