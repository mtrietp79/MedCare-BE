package com.medcare.clinic_backend.repository;

// Thêm dòng import này
import com.medcare.clinic_backend.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {
}