package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    // Hàm hỗ trợ tìm Hóa đơn dựa theo ID của Hồ sơ bệnh án
    Invoice findByMedicalRecordId(Integer recordId);
}