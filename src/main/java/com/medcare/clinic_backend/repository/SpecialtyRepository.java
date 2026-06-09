package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {
    List<Specialty> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String nameKeyword, String descriptionKeyword);

    List<Specialty> findByIsActiveTrue();

    @Query("""
            SELECT s
            FROM Specialty s
            WHERE (:active IS NULL OR s.isActive = :active)
            """)
    Page<Specialty> findAdminSpecialties(@Param("active") Boolean active, Pageable pageable);

    @Query("""
            SELECT s
            FROM Specialty s
            WHERE (:active IS NULL OR s.isActive = :active)
              AND (LOWER(s.name) LIKE LOWER(:keywordPattern)
                   OR LOWER(COALESCE(s.description, '')) LIKE LOWER(:keywordPattern))
            """)
    Page<Specialty> searchAdminSpecialtiesByKeyword(@Param("keywordPattern") String keywordPattern,
                                                    @Param("active") Boolean active,
                                                    Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Specialty s SET s.isActive = true")
    int activateAll();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Specialty s SET s.isActive = false")
    int deactivateAll();
}
