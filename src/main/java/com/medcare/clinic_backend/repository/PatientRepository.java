package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Integer> {

    // Spring Boot sẽ tự hiểu: Tìm Patient -> Váo bên trong Account -> Tìm bằng Username
    Optional<Patient> findByAccount_Username(String username);
}