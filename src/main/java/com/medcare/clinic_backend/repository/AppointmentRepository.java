package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.medcare.clinic_backend.entity.Patient;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.appointmentDate >= :start AND a.appointmentDate < :end " +
            "AND a.status != 'CANCELLED'")
    long countByDoctorInSlot(@Param("doctorId") Integer doctorId,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.appointmentDate >= :start AND a.appointmentDate < :end " +
            "AND a.status != 'CANCELLED' AND a.id <> :appointmentId")
    long countByDoctorInSlotExcludingAppointment(@Param("doctorId") Integer doctorId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("appointmentId") Integer appointmentId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId " +
            "AND a.appointmentDate >= :start AND a.appointmentDate < :end " +
            "AND a.status != 'CANCELLED'")
    long countByPatientInSlot(@Param("patientId") Integer patientId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId " +
            "AND a.appointmentDate >= :start AND a.appointmentDate < :end " +
            "AND a.status != 'CANCELLED' AND a.id <> :appointmentId")
    long countByPatientInSlotExcludingAppointment(@Param("patientId") Integer patientId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  @Param("appointmentId") Integer appointmentId);

    long countByAppointmentDateBetween(LocalDateTime start, LocalDateTime end);

    long countByDoctorId(Integer doctorId);

    long countByDoctorIdAndAppointmentDateBetween(Integer doctorId, LocalDateTime start, LocalDateTime end);

    long countByDoctorIdAndStatus(Integer doctorId, String status);

    @Query("""
            SELECT COUNT(a)
            FROM Appointment a
            WHERE a.doctor.id = :doctorId
              AND a.appointmentDate >= :now
              AND UPPER(COALESCE(a.status, '')) NOT IN ('CANCELLED', 'COMPLETED')
            """)
    long countUpcomingOpenAppointmentsByDoctorId(@Param("doctorId") Integer doctorId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(a)
            FROM Appointment a
            WHERE a.doctor.id = :doctorId
              AND a.parentAppointment IS NOT NULL
              AND a.appointmentDate >= :now
              AND UPPER(COALESCE(a.status, '')) NOT IN ('CANCELLED', 'COMPLETED')
            """)
    long countUpcomingOpenFollowUpAppointmentsByDoctorId(@Param("doctorId") Integer doctorId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE Appointment a
            SET a.parentAppointment = null
            WHERE a.doctor.id = :doctorId
            """)
    void clearParentAppointmentByDoctorId(@Param("doctorId") Integer doctorId);

    void deleteByDoctorId(Integer doctorId);

    List<Appointment> findTop5ByOrderByAppointmentDateDesc();

    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Integer patientId);

    List<Appointment> findByDoctorIdOrderByAppointmentDateDesc(Integer doctorId);

    List<Appointment> findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(
            Integer doctorId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Appointment> findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateDesc(
            Integer doctorId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Appointment> findByDoctorIdAndPatientIdOrderByAppointmentDateDesc(Integer doctorId, Integer patientId);

    Optional<Appointment> findByIdAndPatientId(Integer id, Integer patientId);

    Optional<Appointment> findByIdAndDoctorId(Integer id, Integer doctorId);

    List<Appointment> findByDoctorIdAndAppointmentDate(Integer doctorId, LocalDateTime appointmentDate);

    boolean existsByParentAppointmentId(Integer parentAppointmentId);

    @Query("SELECT DISTINCT a.patient FROM Appointment a WHERE a.doctor.id = :doctorId ORDER BY a.patient.fullName ASC")
    List<Patient> findDistinctPatientsByDoctorId(@Param("doctorId") Integer doctorId);

    @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.doctor.id = :doctorId")
    long countDistinctPatientsByDoctorId(@Param("doctorId") Integer doctorId);

    @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a " +
            "WHERE a.doctor.id = :doctorId " +
            "AND LOWER(COALESCE(a.appointmentType, '')) LIKE CONCAT('%', LOWER(:typeKeyword), '%')")
    long countDistinctPatientsByDoctorIdAndTypeKeyword(@Param("doctorId") Integer doctorId, @Param("typeKeyword") String typeKeyword);

    boolean existsByDoctorIdAndPatientId(Integer doctorId, Integer patientId);

    boolean existsByAppointmentCode(String appointmentCode);

    @Query("SELECT SUM(a.consultationFee) FROM Appointment a WHERE a.status = 'COMPLETED'")
    Double calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(a.consultationFee), 0) FROM Appointment a " +
            "WHERE a.status = 'COMPLETED' " +
            "AND a.appointmentDate >= :start AND a.appointmentDate < :end")
    Double calculateRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.status <> 'CANCELLED'")
    Long countDistinctActivePatients();

    @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a " +
            "WHERE a.status <> 'CANCELLED' " +
            "AND a.appointmentDate >= :start AND a.appointmentDate < :end")
    Long countDistinctPatientsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT a.doctor.id) FROM Appointment a " +
            "WHERE a.doctor IS NOT NULL " +
            "AND a.status <> 'CANCELLED' " +
            "AND a.appointmentDate >= :start AND a.appointmentDate < :end")
    Long countDistinctDoctorsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('MONTH', a.appointmentDate), COUNT(DISTINCT a.patient.id) " +
            "FROM Appointment a " +
            "WHERE FUNCTION('YEAR', a.appointmentDate) = :year " +
            "AND a.status <> 'CANCELLED' " +
            "GROUP BY FUNCTION('MONTH', a.appointmentDate) " +
            "ORDER BY FUNCTION('MONTH', a.appointmentDate)")
    List<Object[]> countDistinctPatientsByYearGroupedByMonth(@Param("year") Integer year);

    List<Appointment> findTop10ByOrderByAppointmentDateDesc();
}
