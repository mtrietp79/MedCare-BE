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
    private Integer appointmentId;

    @Column(name = "service_package_booking_id")
    private Integer servicePackageBookingId;

    @Column(name = "invoice_id")
    private Integer invoiceId;

    @Column(name = "vnp_txn_ref")
    private String vnpTxnRef;

    @Column(name = "vnp_transaction_no")
    private String vnpTransactionNo;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "response_code", length = 20)
    private String responseCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
