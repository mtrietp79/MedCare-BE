package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientRepository patientRepository;

    // 1. LẤY DANH SÁCH LỊCH HẸN
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public List<Appointment> getAll() {
        return appointmentService.getAllAppointments();
    }

    // 2. XEM CHI TIẾT 1 LỊCH HẸN
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public Appointment getById(@PathVariable Integer id) {
        return appointmentService.getAppointmentById(id);
    }

    // 3. TẠO LỊCH HẸN (Chỉ Bệnh nhân mới được tạo, tự động nhận diện user)
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<?> create(@RequestBody Appointment appointment) {
        try {
            // Lấy username từ thẻ Token (SecurityContext)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();

            // Tìm hồ sơ Patient tương ứng với Account này trong Database
            Patient currentPatient = patientRepository.findByAccount_Username(currentUsername)
                    .orElse(null);

            if (currentPatient == null) {
                return ResponseEntity.badRequest()
                        .body("Lỗi: Tài khoản của bạn chưa được liên kết với hồ sơ bệnh nhân nào!");
            }

            // Tự động gán Bệnh nhân vào lịch hẹn
            appointment.setPatient(currentPatient);

            // Mặc định trạng thái khi mới đặt là PENDING (Chờ xác nhận)
            if (appointment.getStatus() == null || appointment.getStatus().isEmpty()) {
                appointment.setStatus("PENDING");
            }

            // Lưu vào DB và trả về kết quả
            Appointment savedAppointment = appointmentService.createAppointment(appointment);
            return ResponseEntity.ok(savedAppointment);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi server: " + e.getMessage());
        }
    }

    // 4. CẬP NHẬT LỊCH HẸN
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public Appointment update(@PathVariable Integer id, @RequestBody Appointment appointment) {
        return appointmentService.updateAppointment(id, appointment);
    }

    // 5. HỦY/XÓA LỊCH HẸN
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public void delete(@PathVariable Integer id) {
        appointmentService.deleteAppointment(id);
    }
}