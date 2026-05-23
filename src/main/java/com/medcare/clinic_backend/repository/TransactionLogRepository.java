package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Integer> {

    boolean existsByVnpTransactionNo(String vnpTransactionNo);

    TransactionLog findTopByServicePackageBookingIdAndResponseCodeOrderByCreatedAtDesc(
            Integer servicePackageBookingId,
            String responseCode
    );

    TransactionLog findTopByInvoiceIdAndResponseCodeOrderByCreatedAtDesc(
            Integer invoiceId,
            String responseCode
    );
}
