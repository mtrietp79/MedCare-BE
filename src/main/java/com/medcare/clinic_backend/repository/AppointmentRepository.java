package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    // 1. Đếm số lượng cuộc hẹn của 1 bác sĩ trong slot (đã có của ông)
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.appointmentDate >= :start AND a.appointmentDate < :end " +
            "AND a.status != 'CANCELLED'")
    long countByDoctorInSlot(@Param("doctorId") Integer doctorId,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    // 2. Đếm số lịch hẹn trong một khoảng thời gian (Dùng để đếm lịch hẹn hôm nay)
    long countByAppointmentDateBetween(LocalDateTime start, LocalDateTime end);

    // 3. Lấy 5 lịch hẹn mới nhất để hiện ở bảng Dashboard
    List<Appointment> findTop5ByOrderByAppointmentDateDesc();

    // 4. Tính tổng doanh thu (Giả sử thực thể Appointment có trường 'fee' hoặc 'price')
    // Nếu ông đặt tên trường khác thì sửa lại 'a.fee' thành tên đó nhé
    @Query("SELECT SUM(a.consultationFee) FROM Appointment a WHERE a.status = 'COMPLETED'")
    Double calculateTotalRevenue();
}
