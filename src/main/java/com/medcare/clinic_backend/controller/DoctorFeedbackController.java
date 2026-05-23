package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.feedback.DoctorFeedbackResponse;
import com.medcare.clinic_backend.dto.feedback.DoctorRatingSummaryResponse;
import com.medcare.clinic_backend.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/{doctorId}/feedbacks")
    @PreAuthorize("permitAll()")
    public List<DoctorFeedbackResponse> getDoctorFeedbacks(@PathVariable Integer doctorId) {
        return feedbackService.getDoctorFeedbackResponses(doctorId);
    }

    @GetMapping("/{doctorId}/rating-summary")
    @PreAuthorize("permitAll()")
    public DoctorRatingSummaryResponse getDoctorRatingSummary(@PathVariable Integer doctorId) {
        return feedbackService.getDoctorRatingSummary(doctorId);
    }
}
