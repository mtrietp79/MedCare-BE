package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Medicine;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {

    @Override
    @EntityGraph(attributePaths = "medicineCategory")
    List<Medicine> findAll();

    @Override
    @EntityGraph(attributePaths = "medicineCategory")
    Optional<Medicine> findById(Integer id);

    long countByMedicineCategory_Id(Integer medicineCategoryId);

    boolean existsByMedicineCategory_Id(Integer medicineCategoryId);
}
