package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.MedicalServicePrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalServicePrescriptionItemRepository extends JpaRepository<MedicalServicePrescriptionItem, Integer> {
}
