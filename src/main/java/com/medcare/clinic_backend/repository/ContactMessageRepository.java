package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.ContactMessage;
import com.medcare.clinic_backend.entity.ContactMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Integer> {

    @Query("""
            SELECT c
            FROM ContactMessage c
            WHERE (:status IS NULL OR c.status = :status)
            """)
    Page<ContactMessage> findAdminMessages(@Param("status") ContactMessageStatus status, Pageable pageable);

    @Query("""
            SELECT c
            FROM ContactMessage c
            WHERE (:status IS NULL OR c.status = :status)
              AND (LOWER(c.fullName) LIKE LOWER(:keywordPattern)
                   OR LOWER(c.email) LIKE LOWER(:keywordPattern)
                   OR LOWER(COALESCE(c.phone, '')) LIKE LOWER(:keywordPattern)
                   OR LOWER(COALESCE(c.subject, '')) LIKE LOWER(:keywordPattern)
                   OR LOWER(c.message) LIKE LOWER(:keywordPattern))
            """)
    Page<ContactMessage> searchByKeyword(@Param("keywordPattern") String keywordPattern,
                                         @Param("status") ContactMessageStatus status,
                                         Pageable pageable);

    long countByStatus(ContactMessageStatus status);
}