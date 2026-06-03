package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.feedback.MessageResponse;
import com.medcare.clinic_backend.dto.feedback.WebsiteFeedbackAdminResponse;
import com.medcare.clinic_backend.dto.feedback.WebsiteFeedbackCreateRequest;
import com.medcare.clinic_backend.dto.feedback.WebsiteFeedbackPublicResponse;
import com.medcare.clinic_backend.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@RestController
public class WebsiteFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("/api/public/website-feedbacks")
    @PreAuthorize("permitAll()")
    public MessageResponse createWebsiteFeedback(
            @RequestBody WebsiteFeedbackCreateRequest request,
            Authentication authentication
    ) {
        boolean hasPatientRole = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PATIENT".equals(authority.getAuthority()));
        String username = authentication == null ? null : authentication.getName();
        return feedbackService.createWebsiteFeedback(request, username, hasPatientRole);
    }

    @GetMapping("/api/public/website-feedbacks")
    @PreAuthorize("permitAll()")
    public List<WebsiteFeedbackPublicResponse> getPublicWebsiteFeedbacks() {
        return feedbackService.getApprovedWebsiteFeedbacks();
    }

    @GetMapping("/api/admin/website-feedbacks")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<WebsiteFeedbackAdminResponse> getAllWebsiteFeedbacksForAdmin() {
        return feedbackService.getAllWebsiteFeedbacksForAdmin();
    }

    @RequestMapping(value = "/api/admin/website-feedbacks/{id}/approve", method = {
            RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.POST
    })
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MessageResponse approveWebsiteFeedback(@PathVariable Integer id) {
        return feedbackService.approveWebsiteFeedback(id);
    }

    @RequestMapping(value = {
            "/api/admin/website-feedbacks/{id}/unhide",
            "/api/admin/website-feedbacks/{id}/show",
            "/api/admin/website-feedbacks/{id}/publish"
    }, method = {RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MessageResponse unhideWebsiteFeedback(@PathVariable Integer id) {
        return feedbackService.unhideWebsiteFeedback(id);
    }

    @RequestMapping(value = "/api/admin/website-feedbacks/{id}/hide", method = {
            RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.POST
    })
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MessageResponse hideWebsiteFeedback(@PathVariable Integer id) {
        return feedbackService.hideWebsiteFeedback(id);
    }

    @RequestMapping(value = {
            "/api/admin/website-feedbacks/{id}/archive",
            "/api/admin/website-feedbacks/{id}/reject"
    }, method = {RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MessageResponse hideWebsiteFeedbackCompat(@PathVariable Integer id) {
        return feedbackService.hideWebsiteFeedback(id);
    }

    @DeleteMapping("/api/admin/website-feedbacks/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MessageResponse deleteWebsiteFeedback(@PathVariable Integer id) {
        return feedbackService.deleteWebsiteFeedback(id);
    }

    @RequestMapping(value = {
            "/api/admin/website-feedbacks/{id}/delete",
            "/api/admin/website-feedbacks/{id}/remove",
            "/api/admin/website-feedbacks/{id}/destroy"
    }, method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MessageResponse deleteWebsiteFeedbackCompat(@PathVariable Integer id) {
        return feedbackService.deleteWebsiteFeedback(id);
    }
}
