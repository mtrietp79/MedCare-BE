package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.MedicalServicePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalServicePhotoRepository extends JpaRepository<MedicalServicePhoto, Integer> {
    Optional<MedicalServicePhoto> findByMedicalServiceId(Integer medicalServiceId);

    boolean existsByMedicalServiceId(Integer medicalServiceId);

    void deleteByMedicalServiceId(Integer medicalServiceId);
}
