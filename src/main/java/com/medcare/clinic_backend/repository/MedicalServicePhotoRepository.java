package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.MedicalServicePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MedicalServicePhotoRepository extends JpaRepository<MedicalServicePhoto, Integer> {
    Optional<MedicalServicePhoto> findByMedicalServiceId(Integer medicalServiceId);

    @Query("select p.id from MedicalServicePhoto p where p.medicalService.id = :medicalServiceId")
    Optional<Integer> findIdByMedicalServiceId(Integer medicalServiceId);

    boolean existsByMedicalServiceId(Integer medicalServiceId);

    void deleteByMedicalServiceId(Integer medicalServiceId);
}
