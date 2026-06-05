package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalServiceRepository extends JpaRepository<MedicalService, Integer> {
    List<MedicalService> findByActiveTrueOrderByIdDesc();

    List<MedicalService> findBySpecialty_IdAndActiveTrueOrderByIdDesc(Integer specialtyId);

    List<MedicalService> findBySpecialty_IdOrderByIdDesc(Integer specialtyId);

    List<MedicalService> findByActiveTrueAndNameContainingIgnoreCaseOrderByIdDesc(String name);

    List<MedicalService> findBySpecialty_IdAndActiveTrueAndNameContainingIgnoreCaseOrderByIdDesc(Integer specialtyId, String name);

    List<MedicalService> findByNameContainingIgnoreCaseOrderByIdDesc(String name);

    List<MedicalService> findBySpecialty_IdAndNameContainingIgnoreCaseOrderByIdDesc(Integer specialtyId, String name);

    @Modifying
    @Query("""
            UPDATE MedicalService ms
            SET ms.assignedDoctor = null
            WHERE ms.assignedDoctor.id = :doctorId
            """)
    void clearAssignedDoctorByDoctorId(@Param("doctorId") Integer doctorId);
}
