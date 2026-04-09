package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    // Đếm số lượng cuộc hẹn của 1 bác sĩ trong khoảng từ startTime đến endTime
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.appointmentDate >= :start AND a.appointmentDate < :end " +
            "AND a.status != 'CANCELLED'")
    long countByDoctorInSlot(@Param("doctorId") Integer doctorId,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);
}