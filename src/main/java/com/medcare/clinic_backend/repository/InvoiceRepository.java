package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    Optional<Invoice> findByMedicalRecordId(Integer recordId);

    Optional<Invoice> findByMedicalRecordIdAndMedicalRecordDoctorId(Integer recordId, Integer doctorId);

    List<Invoice> findByMedicalRecordDoctorIdOrderByCreatedAtDesc(Integer doctorId);

    List<Invoice> findByMedicalRecordPatientIdOrderByCreatedAtDesc(Integer patientId);

    Optional<Invoice> findByIdAndMedicalRecordPatientId(Integer id, Integer patientId);

    Optional<Invoice> findByMedicalRecordIdAndMedicalRecordPatientId(Integer recordId, Integer patientId);

    boolean existsByAppointmentId(Integer appointmentId);

    void deleteByMedicalRecordIdIn(List<Integer> recordIds);

    @Query("""
            select i.medicalRecord.id, i.id, i.status, i.totalAmount
            from Invoice i
            where i.medicalRecord.id in :recordIds and i.medicalRecord.patient.id = :patientId
            """)
    List<Object[]> findPatientInvoiceSummaryRowsByRecordIds(
            @Param("recordIds") List<Integer> recordIds,
            @Param("patientId") Integer patientId
    );

    @Query("""
            select i.id, i.status, i.medicineFee, i.serviceFee, i.totalAmount, i.createdAt
            from Invoice i
            where i.medicalRecord.id = :recordId and i.medicalRecord.patient.id = :patientId
            """)
    Optional<Object[]> findPatientInvoiceDetailRowByRecordId(
            @Param("recordId") Integer recordId,
            @Param("patientId") Integer patientId
    );
}
