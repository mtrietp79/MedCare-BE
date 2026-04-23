package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Integer> {

    // Tìm danh sách bác sĩ trực trong một ngày cụ thể
    List<DoctorSchedule> findByWorkDate(LocalDate date);

    // Tìm lịch trực của một bác sĩ cụ thể
    List<DoctorSchedule> findByDoctorId(Integer doctorId);

    List<DoctorSchedule> findByDoctorIdAndWorkDate(Integer doctorId, LocalDate workDate);

    boolean existsByDoctorIdAndWorkDateAndShift(Integer doctorId, LocalDate workDate, String shift);
}
