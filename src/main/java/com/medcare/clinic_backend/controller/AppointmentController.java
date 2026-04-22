package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.SlotAvailabilityDto;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientRepository patientRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public List<Appointment> getAll() {
        return appointmentService.getAllAppointments();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public Appointment getById(@PathVariable Integer id) {
        return appointmentService.getAppointmentById(id);
    }

    @GetMapping("/doctor/{doctorId}/slots")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public List<SlotAvailabilityDto> getDoctorSlotStatus(
            @PathVariable Integer doctorId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return appointmentService.getDoctorSlotStatus(doctorId, date);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<?> create(@RequestBody Appointment appointment) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();

            Patient currentPatient = patientRepository.findByAccount_Username(currentUsername).orElse(null);
            if (currentPatient == null) {
                return ResponseEntity.badRequest()
                        .body("Loi: Tai khoan cua ban chua duoc lien ket voi ho so benh nhan nao.");
            }

            appointment.setPatient(currentPatient);
            if (appointment.getStatus() == null || appointment.getStatus().isEmpty()) {
                appointment.setStatus("PENDING");
            }

            Appointment savedAppointment = appointmentService.createAppointment(appointment);
            return ResponseEntity.ok(savedAppointment);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Loi server: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public Appointment update(@PathVariable Integer id, @RequestBody Appointment appointment) {
        return appointmentService.updateAppointment(id, appointment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public void delete(@PathVariable Integer id) {
        appointmentService.deleteAppointment(id);
    }
}
