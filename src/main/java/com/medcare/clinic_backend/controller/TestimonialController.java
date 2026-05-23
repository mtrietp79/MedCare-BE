package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.feedback.WebsiteFeedbackPublicResponse;
import com.medcare.clinic_backend.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/testimonials")
public class TestimonialController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping
    public List<WebsiteFeedbackPublicResponse> getTestimonials() {
        return feedbackService.getApprovedWebsiteFeedbacks();
    }
}
