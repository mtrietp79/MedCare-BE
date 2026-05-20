package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalServiceRepository extends JpaRepository<MedicalService, Integer> {
    List<MedicalService> findByActiveTrueOrderByIdDesc();

    List<MedicalService> findBySpecialtyIdAndActiveTrueOrderByIdDesc(Integer specialtyId);

    List<MedicalService> findBySpecialtyIdOrderByIdDesc(Integer specialtyId);
}
