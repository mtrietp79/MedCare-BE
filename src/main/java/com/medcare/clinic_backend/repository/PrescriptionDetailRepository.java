package com.medcare.clinic_backend.repository;

// Thêm 2 dòng import này
import com.medcare.clinic_backend.entity.PrescriptionDetail;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrescriptionDetailRepository extends JpaRepository<PrescriptionDetail, Integer> {

    // Tìm toàn bộ danh sách thuốc của một hồ sơ bệnh án
    List<PrescriptionDetail> findByMedicalRecordId(Integer recordId);
}