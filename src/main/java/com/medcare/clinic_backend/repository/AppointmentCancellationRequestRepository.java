package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.AppointmentCancellationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentCancellationRequestRepository extends JpaRepository<AppointmentCancellationRequest, Integer> {

    boolean existsByAppointmentIdAndStatusIn(Integer appointmentId, Collection<String> statuses);

    Optional<AppointmentCancellationRequest> findTopByAppointmentIdOrderByCreatedAtDesc(Integer appointmentId);

    List<AppointmentCancellationRequest> findByAppointmentIdInOrderByCreatedAtDesc(List<Integer> appointmentIds);

    List<AppointmentCancellationRequest> findByPatientIdOrderByCreatedAtDesc(Integer patientId);

    @Query("""
            SELECT r
            FROM AppointmentCancellationRequest r
            JOIN FETCH r.appointment a
            JOIN FETCH r.patient p
            LEFT JOIN FETCH a.doctor d
            WHERE r.id = :id
            """)
    Optional<AppointmentCancellationRequest> findDetailedById(@Param("id") Integer id);

    @Query("""
            SELECT r
            FROM AppointmentCancellationRequest r
            JOIN r.appointment a
            JOIN r.patient p
            LEFT JOIN a.doctor d
            WHERE (:status IS NULL OR r.status = :status)
              AND (
                    :keywordPattern IS NULL
                    OR LOWER(p.fullName) LIKE :keywordPattern
                    OR LOWER(COALESCE(p.email, '')) LIKE :keywordPattern
                    OR LOWER(COALESCE(a.appointmentCode, '')) LIKE :keywordPattern
                    OR LOWER(COALESCE(d.fullName, '')) LIKE :keywordPattern
                    OR LOWER(COALESCE(r.cancelReason, '')) LIKE :keywordPattern
              )
            """)
    Page<AppointmentCancellationRequest> findAdminList(@Param("status") String status,
                                                       @Param("keywordPattern") String keywordPattern,
                                                       Pageable pageable);

    @Query("""
            SELECT COUNT(r)
            FROM AppointmentCancellationRequest r
            WHERE (:status IS NULL OR r.status = :status)
            """)
    long countByStatus(@Param("status") String status);

    @Query("""
            SELECT COALESCE(SUM(r.refundAmount), 0)
            FROM AppointmentCancellationRequest r
            WHERE r.status = :status
            """)
    Double sumRefundAmountByStatus(@Param("status") String status);
}
