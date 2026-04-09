package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalServiceRepository extends JpaRepository<MedicalService, Integer> {
}