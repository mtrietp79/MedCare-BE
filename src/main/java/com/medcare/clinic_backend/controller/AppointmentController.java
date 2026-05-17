package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.SlotAvailabilityDto;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public List<Appointment> getAll() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_PATIENT") && !hasAuthority(authentication, "ROLE_DOCTOR")) {
            Patient currentPatient = getCurrentPatientOrThrow(authentication);
            return appointmentService.getAppointmentsForPatient(currentPatient.getId());
        }

        if (hasAuthority(authentication, "ROLE_DOCTOR")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return appointmentService.getAppointmentsForDoctor(currentDoctor.getId());
        }

        return appointmentService.getAllAppointments();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public Appointment getById(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_PATIENT") && !hasAuthority(authentication, "ROLE_DOCTOR")) {
            Patient currentPatient = getCurrentPatientOrThrow(authentication);
            return appointmentService.getAppointmentByIdForPatient(id, currentPatient.getId());
        }

        if (hasAuthority(authentication, "ROLE_DOCTOR")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return appointmentService.getAppointmentByIdForDoctor(id, currentDoctor.getId());
        }

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
    public ResponseEntity<Appointment> create(@RequestBody Appointment appointment) {
        if (appointment == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu lich hen khong hop le.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Patient currentPatient = getCurrentPatientOrThrow(authentication);

        appointment.setPatient(currentPatient);
        appointment.setStatus("PENDING");

        Appointment savedAppointment = appointmentService.createAppointment(appointment);
        return ResponseEntity.ok(savedAppointment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public Appointment update(@PathVariable Integer id, @RequestBody Appointment appointment) {
        if (appointment == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu cap nhat khong hop le.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_PATIENT") && !hasAuthority(authentication, "ROLE_DOCTOR")) {
            Patient currentPatient = getCurrentPatientOrThrow(authentication);
            appointmentService.getAppointmentByIdForPatient(id, currentPatient.getId());
            if (appointment.getStatus() != null
                    && !appointment.getStatus().isBlank()
                    && !"CANCELLED".equalsIgnoreCase(appointment.getStatus())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Benh nhan chi duoc doi trang thai sang CANCELLED.");
            }
            if (appointment.getPaymentStatus() != null && !appointment.getPaymentStatus().isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Benh nhan khong duoc cap nhat paymentStatus.");
            }
        }
        if (hasAuthority(authentication, "ROLE_DOCTOR")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            appointmentService.getAppointmentByIdForDoctor(id, currentDoctor.getId());
        }
        return appointmentService.updateAppointment(id, appointment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public void delete(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_PATIENT") && !hasAuthority(authentication, "ROLE_DOCTOR")) {
            Patient currentPatient = getCurrentPatientOrThrow(authentication);
            appointmentService.getAppointmentByIdForPatient(id, currentPatient.getId());
        }
        if (hasAuthority(authentication, "ROLE_DOCTOR")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            appointmentService.getAppointmentByIdForDoctor(id, currentDoctor.getId());
        }
        appointmentService.deleteAppointment(id);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private Patient getCurrentPatientOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }

        return patientRepository.findByAccount_Username(authentication.getName())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan cua ban chua duoc lien ket voi ho so benh nhan."
                ));
    }

    private Doctor getCurrentDoctorOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }

        return doctorRepository.findByAccount_Username(authentication.getName())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan doctor chua duoc lien ket voi ho so bac si."
                ));
    }
}
