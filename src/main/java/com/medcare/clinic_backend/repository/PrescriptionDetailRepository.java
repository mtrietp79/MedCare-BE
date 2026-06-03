package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.PrescriptionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionDetailRepository extends JpaRepository<PrescriptionDetail, Integer> {

    List<PrescriptionDetail> findByMedicalRecordId(Integer recordId);

    List<PrescriptionDetail> findByMedicalRecordIdAndMedicalRecordDoctorId(Integer recordId, Integer doctorId);

    List<PrescriptionDetail> findByMedicalRecordIdIn(List<Integer> recordIds);

    @Query("""
            select m.id, m.name, m.unit, pd.quantity, pd.dosage, pd.note, m.price
            from PrescriptionDetail pd
            join pd.medicine m
            join pd.medicalRecord mr
            where mr.id = :recordId and mr.patient.id = :patientId
            order by pd.id asc
            """)
    List<Object[]> findPatientMedicineRowsByRecordId(
            @Param("recordId") Integer recordId,
            @Param("patientId") Integer patientId
    );
}
