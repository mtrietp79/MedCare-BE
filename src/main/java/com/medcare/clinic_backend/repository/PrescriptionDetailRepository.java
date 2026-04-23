package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.PrescriptionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionDetailRepository extends JpaRepository<PrescriptionDetail, Integer> {

    List<PrescriptionDetail> findByMedicalRecordId(Integer recordId);

    List<PrescriptionDetail> findByMedicalRecordIdAndMedicalRecordDoctorId(Integer recordId, Integer doctorId);
}
