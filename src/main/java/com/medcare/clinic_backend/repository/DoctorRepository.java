package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Doctor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Doctor d WHERE d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Integer id);

    Optional<Doctor> findByAccount_Username(String username);

    List<Doctor> findBySpecialty_Id(Integer specialtyId);

    List<Doctor> findByFullNameContainingIgnoreCase(String name);

    List<Doctor> findBySpecialty_IdAndFullNameContainingIgnoreCase(Integer specialtyId, String name);

    long countBySpecialty_Id(Integer specialtyId);

    List<Doctor> findByFullNameContainingIgnoreCaseOrSpecialty_NameContainingIgnoreCase(String fullNameKeyword, String specialtyKeyword);

    boolean existsByAccountId(Integer accountId);
}
