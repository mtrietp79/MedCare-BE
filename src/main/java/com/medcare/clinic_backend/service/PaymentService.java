package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Transactional
    public String processVnpayReturn(Map<String, String> vnpParams, Integer appointmentId) {
        String responseCode = vnpParams.get("vnp_ResponseCode");

        // 1. Lưu lại lịch sử giao dịch vào TransactionLog
        TransactionLog log = new TransactionLog();
        log.setAppointmentId(appointmentId);
        log.setVnpTxnRef(vnpParams.get("vnp_TxnRef"));
        log.setVnpTransactionNo(vnpParams.get("vnp_TransactionNo"));
        log.setBankCode(vnpParams.get("vnp_BankCode"));

        // VNPay nhân 100 số tiền nên mình phải chia lại cho 100
        if (vnpParams.get("vnp_Amount") != null) {
            double amount = Double.parseDouble(vnpParams.get("vnp_Amount")) / 100;
            log.setAmount(amount);
        }

        log.setResponseCode(responseCode);
        transactionLogRepository.save(log);

        // 2. Nếu thành công (mã 00), cập nhật trạng thái thanh toán của Lịch hẹn
        if ("00".equals(responseCode)) {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

            if (appointment != null) {
                appointment.setPaymentStatus("PAID_ONLINE");
                appointmentRepository.save(appointment);
                return "THANH TOÁN THÀNH CÔNG! Trạng thái lịch hẹn đã được cập nhật.";
            } else {
                return "THANH TOÁN THÀNH CÔNG! Nhưng không tìm thấy Lịch hẹn ID: " + appointmentId;
            }
        }

        return "THANH TOÁN THẤT BẠI HOẶC BỊ HỦY BỎ. Mã lỗi VNPay: " + responseCode;
    }
}