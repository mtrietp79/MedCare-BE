package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.feedback.CanFeedbackResponse;
import com.medcare.clinic_backend.dto.feedback.DoctorFeedbackCreateRequest;
import com.medcare.clinic_backend.dto.feedback.MessageResponse;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasAuthority('ROLE_PATIENT')")
public class PatientDoctorFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("/doctor-feedbacks")
    public MessageResponse createDoctorFeedback(
            @RequestBody DoctorFeedbackCreateRequest request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return feedbackService.createDoctorFeedback(authentication.getName(), request);
    }

    @GetMapping("/appointments/{appointmentId}/can-feedback")
    public CanFeedbackResponse canFeedback(
            @PathVariable Integer appointmentId,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return feedbackService.canPatientFeedbackAppointment(authentication.getName(), appointmentId);
    }
}
