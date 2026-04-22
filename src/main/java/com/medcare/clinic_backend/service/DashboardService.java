package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class DashboardService {
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private MedicineRepository medicineRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    public Map<String, Object> getSummary() {
        Map<String, Object> stats = new HashMap<>();

        // Tổng số lượng các thực thể
        stats.put("totalPatients", patientRepository.count());
        stats.put("totalDoctors", doctorRepository.count());
        stats.put("totalMedicines", medicineRepository.count());

        // Đếm lịch hẹn hôm nay (từ 00:00:00 đến 23:59:59)
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        stats.put("appointmentsToday", appointmentRepository.countByAppointmentDateBetween(startOfDay, endOfDay));

        // Tính doanh thu thật từ DB
        Double revenue = appointmentRepository.calculateTotalRevenue();
        stats.put("totalRevenue", revenue != null ? revenue : 0);

        return stats;
    }

    public List<Appointment> getRecentAppointments() {
        return appointmentRepository.findTop5ByOrderByAppointmentDateDesc();
    }
}