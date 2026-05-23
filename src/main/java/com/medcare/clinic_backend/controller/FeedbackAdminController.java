package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.feedback.DoctorFeedbackResponse;
import com.medcare.clinic_backend.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/feedbacks", "/api/admin/feedbacks"})
public class FeedbackAdminController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<DoctorFeedbackResponse> getAll() {
        return feedbackService.getAllFeedbacks().stream()
                .map(feedback -> new DoctorFeedbackResponse(
                        feedback.getId(),
                        feedback.getPatient() == null ? null : feedback.getPatient().getFullName(),
                        feedback.getRating(),
                        feedback.getComment(),
                        feedback.getCreatedAt() == null ? null : feedback.getCreatedAt().toLocalDate()
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("permitAll()")
    public List<DoctorFeedbackResponse> getByDoctor(@PathVariable Integer doctorId) {
        return feedbackService.getDoctorFeedbackResponses(doctorId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable Integer id) {
        feedbackService.deleteFeedback(id);
    }
}
