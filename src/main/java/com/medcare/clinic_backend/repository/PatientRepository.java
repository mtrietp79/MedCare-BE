package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Integer> {

    @EntityGraph(attributePaths = "account")
    @Query("""
            SELECT p
            FROM Patient p
            JOIN p.account a
            WHERE (:active IS NULL OR a.isActive = :active)
            """)
    Page<Patient> findAdminPatients(@Param("active") Boolean active, Pageable pageable);

    @EntityGraph(attributePaths = "account")
    @Query("""
            SELECT p
            FROM Patient p
            JOIN p.account a
            WHERE (LOWER(COALESCE(p.fullName, '')) LIKE LOWER(:keywordPattern)
                   OR LOWER(COALESCE(p.email, '')) LIKE LOWER(:keywordPattern)
                   OR LOWER(COALESCE(p.phone, '')) LIKE LOWER(:keywordPattern))
              AND (:active IS NULL OR a.isActive = :active)
            """)
    Page<Patient> searchAdminPatientsByKeyword(@Param("keywordPattern") String keywordPattern,
                                               @Param("active") Boolean active,
                                               Pageable pageable);

    @Query("""
            SELECT p
            FROM Patient p
            JOIN FETCH p.account a
            WHERE p.id = :id
            """)
    Optional<Patient> findByIdWithAccount(@Param("id") Integer id);

    Optional<Patient> findByAccount_Username(String username);

    Optional<Patient> findFirstByEmailIgnoreCase(String email);

    Optional<Patient> findFirstByPhone(String phone);

    long countByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    boolean existsByPhoneAndIdNot(String phone, Integer id);

    boolean existsByNationalIdAndIdNot(String nationalId, Integer id);

    @Query("""
            SELECT COUNT(p)
            FROM Patient p
            JOIN p.account a
            WHERE a.isActive = true
            """)
    long countActivePatients();

    @Query("""
            SELECT COUNT(p)
            FROM Patient p
            JOIN p.account a
            WHERE a.isActive = false
            """)
    long countLockedPatients();

    @Query("""
            SELECT COUNT(p)
            FROM Patient p
            JOIN p.account a
            WHERE a.createdAt >= :from
              AND a.createdAt < :to
            """)
    long countNewPatientsBetween(@Param("from") java.time.LocalDateTime from,
                                 @Param("to") java.time.LocalDateTime to);
}
