package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<Appointment> findTop5ByOrderByAppointmentDateDesc();

    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Integer patientId);

    List<Appointment> findByDoctorIdOrderByAppointmentDateDesc(Integer doctorId);

    Optional<Appointment> findByIdAndPatientId(Integer id, Integer patientId);

    Optional<Appointment> findByIdAndDoctorId(Integer id, Integer doctorId);

    @Query("SELECT DISTINCT a.patient FROM Appointment a WHERE a.doctor.id = :doctorId ORDER BY a.patient.fullName ASC")
    List<Patient> findDistinctPatientsByDoctorId(@Param("doctorId") Integer doctorId);

    boolean existsByDoctorIdAndPatientId(Integer doctorId, Integer patientId);

    boolean existsByAppointmentCode(String appointmentCode);

    @Query("SELECT SUM(a.consultationFee) FROM Appointment a WHERE a.status = 'COMPLETED'")
    Double calculateTotalRevenue();
}
