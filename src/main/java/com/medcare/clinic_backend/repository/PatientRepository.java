package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Integer> {

    Optional<Patient> findByAccount_Username(String username);

    boolean existsByPhoneAndIdNot(String phone, Integer id);

    boolean existsByNationalIdAndIdNot(String nationalId, Integer id);
}
