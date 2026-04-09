package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Integer> {

    // Hàm này giúp lấy ra "Quyển sổ khám bệnh" của 1 bệnh nhân cụ thể
    // OrderByExaminationDateDesc: Sắp xếp ngày khám giảm dần (Lần khám mới nhất lên đầu)
    List<MedicalRecord> findByPatientIdOrderByExaminationDateDesc(Integer patientId);
}