package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.service.DoctorService;
import com.medcare.clinic_backend.service.MedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private PatientRepository patientRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public List<MedicalRecord> getAll() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return medicalRecordService.getRecordsForDoctor(currentDoctor.getId());
        }
        return medicalRecordService.getAllRecords();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public MedicalRecord getById(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return medicalRecordService.getRecordByIdForDoctor(id, currentDoctor.getId());
        }
        return medicalRecordService.getRecordById(id);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public List<MedicalRecord> getMyRecords(Authentication authentication) {
        Patient patient = getCurrentPatientOrThrow(authentication);
        return medicalRecordService.getHistoryByPatientId(patient.getId());
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public MedicalRecord getMyRecordById(@PathVariable Integer id, Authentication authentication) {
        Patient patient = getCurrentPatientOrThrow(authentication);
        return medicalRecordService.getRecordByIdForPatient(id, patient.getId());
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public List<MedicalRecord> getHistoryByPatient(@PathVariable Integer patientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return medicalRecordService.getHistoryByPatientIdForDoctor(patientId, currentDoctor.getId());
        }
        return medicalRecordService.getHistoryByPatientId(patientId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public MedicalRecord create(@RequestBody MedicalRecord record) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
        record.setDoctor(currentDoctor);
        return medicalRecordService.createRecord(record);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public MedicalRecord update(@PathVariable Integer id, @RequestBody MedicalRecord record) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
        return medicalRecordService.updateRecordForDoctor(id, currentDoctor.getId(), record);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable Integer id) {
        medicalRecordService.deleteRecord(id);
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
