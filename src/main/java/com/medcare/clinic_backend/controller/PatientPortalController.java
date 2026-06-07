package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.patient.PatientAppointmentResponse;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasAuthority('ROLE_PATIENT')")
public class PatientPortalController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientRepository patientRepository;

    @GetMapping("/appointments")
    public List<PatientAppointmentResponse> getMyAppointments(Authentication authentication) {
        Patient patient = getCurrentPatientOrThrow(authentication);
        return appointmentService.getAppointmentResponsesForPatient(patient.getId());
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
}
