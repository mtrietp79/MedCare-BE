package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.doctor.*;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.service.DoctorPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@PreAuthorize("hasAuthority('ROLE_DOCTOR')")
public class DoctorPortalController {

    @Autowired
    private DoctorPortalService doctorPortalService;

    @GetMapping("/dashboard")
    public DoctorDashboardResponse getDashboard(Authentication authentication) {
        return doctorPortalService.getDashboard(getCurrentUsername(authentication));
    }

    @GetMapping("/appointments")
    public List<DoctorAppointmentListItemResponse> getAppointments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String type,
            Authentication authentication
    ) {
        return doctorPortalService.getAppointments(
                getCurrentUsername(authentication),
                keyword,
                status,
                parseDateOrNull(date),
                type
        );
    }

    @GetMapping("/appointments/{id}")
    public DoctorAppointmentDetailResponse getAppointmentDetail(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        return doctorPortalService.getAppointmentDetail(getCurrentUsername(authentication), id);
    }

    @PostMapping("/appointments/{appointmentId}/complete")
    public CompleteAppointmentResponse completeAppointment(
            @PathVariable Integer appointmentId,
            @RequestBody CompleteAppointmentRequest request,
            Authentication authentication
    ) {
        return doctorPortalService.completeAppointment(getCurrentUsername(authentication), appointmentId, request);
    }

    @GetMapping("/medicines")
    public List<DoctorMedicineResponse> getMedicines(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            Authentication authentication
    ) {
        return doctorPortalService.getMedicinesForDoctor(
                getCurrentUsername(authentication),
                keyword,
                categoryId,
                status,
                category
        );
    }

    @GetMapping("/medical-services")
    public List<DoctorMedicalServiceResponse> getMedicalServices(Authentication authentication) {
        return doctorPortalService.getMedicalServicesForDoctor(getCurrentUsername(authentication));
    }

    @GetMapping("/medical-records/summary")
    public DoctorMedicalRecordsSummaryResponse getMedicalRecordSummary(Authentication authentication) {
        return doctorPortalService.getMedicalRecordSummary(getCurrentUsername(authentication));
    }

    @GetMapping("/medical-records/patients")
    public List<DoctorMedicalRecordPatientItemResponse> getMedicalRecordPatients(
            @RequestParam(required = false) String keyword,
            Authentication authentication
    ) {
        return doctorPortalService.getMedicalRecordPatients(getCurrentUsername(authentication), keyword);
    }

    @GetMapping("/medical-records/patients/{patientId}")
    public DoctorPatientMedicalRecordsResponse getPatientMedicalRecords(
            @PathVariable Integer patientId,
            Authentication authentication
    ) {
        return doctorPortalService.getPatientMedicalRecords(getCurrentUsername(authentication), patientId);
    }

    @PostMapping("/medical-records/{recordId}/follow-up")
    public CreateFollowUpResponse createFollowUp(
            @PathVariable Integer recordId,
            @RequestBody CreateFollowUpRequest request,
            Authentication authentication
    ) {
        return doctorPortalService.createFollowUp(getCurrentUsername(authentication), recordId, request);
    }

    @GetMapping("/schedule/week")
    public DoctorWeekScheduleResponse getWeekSchedule(
            @RequestParam String startDate,
            Authentication authentication
    ) {
        LocalDate parsedDate = parseRequiredDate(startDate, "startDate");
        return doctorPortalService.getWeekSchedule(getCurrentUsername(authentication), parsedDate);
    }

    @GetMapping("/schedule/day")
    public List<DoctorScheduleDayAppointmentResponse> getDaySchedule(
            @RequestParam String date,
            @RequestParam String period,
            Authentication authentication
    ) {
        LocalDate parsedDate = parseRequiredDate(date, "date");
        return doctorPortalService.getDaySchedule(getCurrentUsername(authentication), parsedDate, period);
    }

    @GetMapping("/profile")
    public DoctorProfileResponse getProfile(Authentication authentication) {
        return doctorPortalService.getProfile(getCurrentUsername(authentication));
    }

    @PutMapping("/profile")
    public DoctorProfileResponse updateProfile(
            @RequestBody UpdateDoctorProfileRequest request,
            Authentication authentication
    ) {
        return doctorPortalService.updateProfile(getCurrentUsername(authentication), request);
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AvatarUploadResponse uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return doctorPortalService.uploadProfileAvatar(getCurrentUsername(authentication), file);
    }

    @DeleteMapping("/profile/avatar")
    public AvatarUploadResponse deleteAvatar(Authentication authentication) {
        return doctorPortalService.deleteProfileAvatar(getCurrentUsername(authentication));
    }

    private String getCurrentUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return authentication.getName();
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseRequiredDate(value, "date");
    }

    private LocalDate parseRequiredDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu tham so '" + fieldName + "'.");
        }

        String normalized = value.trim();
        List<DateTimeFormatter> acceptedFormats = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-M-d")
        );

        for (DateTimeFormatter formatter : acceptedFormats) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }

        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Ngay khong hop le. Dinh dang ho tro: yyyy-MM-dd hoac d/M/yyyy."
        );
    }
}
