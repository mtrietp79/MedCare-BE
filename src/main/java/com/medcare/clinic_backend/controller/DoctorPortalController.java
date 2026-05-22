package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.DoctorResponse;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
public class DoctorPortalController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public DoctorResponse getProfile() {
        return doctorService.getDoctorResponseByAccountUsername(getCurrentUsername());
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public Map<String, Object> getDashboardStats() {
        Doctor doctor = getCurrentDoctor();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        Map<String, Object> stats = new HashMap<>();
        stats.put("doctorId", doctor.getId());
        stats.put("totalAppointments", appointmentRepository.countByDoctorId(doctor.getId()));
        stats.put("appointmentsToday", appointmentRepository.countByDoctorIdAndAppointmentDateBetween(
                doctor.getId(),
                startOfDay,
                endOfDay
        ));
        stats.put("pendingAppointments", appointmentRepository.countByDoctorIdAndStatus(doctor.getId(), "PENDING"));
        stats.put("confirmedAppointments", appointmentRepository.countByDoctorIdAndStatus(doctor.getId(), "CONFIRMED"));
        stats.put("completedAppointments", appointmentRepository.countByDoctorIdAndStatus(doctor.getId(), "COMPLETED"));
        stats.put("totalPatients", appointmentRepository.countDistinctPatientsByDoctorId(doctor.getId()));
        return stats;
    }

    private Doctor getCurrentDoctor() {
        return doctorRepository.findByAccount_Username(getCurrentUsername())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan doctor chua duoc lien ket voi ho so bac si."
                ));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return authentication.getName();
    }
}
