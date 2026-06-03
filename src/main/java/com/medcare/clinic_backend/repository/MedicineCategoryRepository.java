package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.MedicineCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineCategoryRepository extends JpaRepository<MedicineCategory, Integer> {
    List<MedicineCategory> findAllByOrderByNameAsc();

    List<MedicineCategory> findByIsActiveTrueOrderByNameAsc();

    Optional<MedicineCategory> findByNameIgnoreCase(String name);
}
