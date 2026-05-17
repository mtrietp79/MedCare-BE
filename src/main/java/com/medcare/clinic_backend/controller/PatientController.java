package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.service.DoctorService;
import com.medcare.clinic_backend.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public List<Patient> getAll() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return patientService.getPatientsForDoctor(currentDoctor.getId());
        }
        return patientService.getAllPatients();
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public Patient getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return patientService.getPatientByAccountUsername(authentication.getName());
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public Patient updateMyProfile(@RequestBody Patient patient) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return patientService.updateOwnProfile(authentication.getName(), patient);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public Patient getById(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return patientService.getPatientByIdForDoctor(id, currentDoctor.getId());
        }
        return patientService.getPatientById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Patient create(@RequestBody Patient patient) {
        return patientService.createPatient(patient);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Patient update(@PathVariable Integer id, @RequestBody Patient patient) {
        return patientService.updatePatient(id, patient);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable Integer id) {
        patientService.deletePatient(id);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private Doctor getCurrentDoctorOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return doctorService.getDoctorByAccountUsername(authentication.getName());
    }
}
