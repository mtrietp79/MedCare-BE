package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Integer> {

    boolean existsByVnpTransactionNo(String vnpTransactionNo);

    TransactionLog findTopByServicePackageBookingIdAndResponseCodeOrderByCreatedAtDesc(
            Integer servicePackageBookingId,
            String responseCode
    );

    TransactionLog findTopByAppointmentIdAndResponseCodeOrderByCreatedAtDesc(
            Integer appointmentId,
            String responseCode
    );

    TransactionLog findTopByInvoiceIdAndResponseCodeOrderByCreatedAtDesc(
            Integer invoiceId,
            String responseCode
    );

    List<TransactionLog> findByAppointmentIdInOrderByCreatedAtDesc(List<Integer> appointmentIds);

    List<TransactionLog> findByInvoiceIdInOrderByCreatedAtDesc(List<Integer> invoiceIds);

    List<TransactionLog> findByServicePackageBookingIdInOrderByCreatedAtDesc(List<Integer> servicePackageBookingIds);
}
