package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transaction_logs")
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "appointment_id")
    private Integer appointmentId; // Lưu ID lịch hẹn được thanh toán

    @Column(name = "vnp_txn_ref")
    private String vnpTxnRef; // Mã giao dịch của hệ thống mình gửi đi

    @Column(name = "vnp_transaction_no")
    private String vnpTransactionNo; // Mã giao dịch do VNPay sinh ra

    @Column(name = "bank_code", length = 50)
    private String bankCode; // Mã ngân hàng (NCB, VCB...)

    @Column(name = "amount")
    private Double amount; // Số tiền

    @Column(name = "response_code", length = 10)
    private String responseCode; // Mã phản hồi (00 là thành công)

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}