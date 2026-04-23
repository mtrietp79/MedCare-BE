package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    Optional<Invoice> findByMedicalRecordId(Integer recordId);

    Optional<Invoice> findByMedicalRecordIdAndMedicalRecordDoctorId(Integer recordId, Integer doctorId);

    List<Invoice> findByMedicalRecordDoctorIdOrderByCreatedAtDesc(Integer doctorId);
}
