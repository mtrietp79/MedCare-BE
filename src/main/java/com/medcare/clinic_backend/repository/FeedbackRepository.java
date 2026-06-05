package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    List<Feedback> findByDoctorIdOrderByCreatedAtDesc(Integer doctorId);

    List<Feedback> findAllByOrderByCreatedAtDesc();

    boolean existsByAppointmentId(Integer appointmentId);

    Optional<Feedback> findByAppointmentId(Integer appointmentId);

    long countByDoctorId(Integer doctorId);

    void deleteByDoctorId(Integer doctorId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.doctor.id = :doctorId")
    Double findAverageRatingByDoctorId(@Param("doctorId") Integer doctorId);
}
