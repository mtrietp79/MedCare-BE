package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Feedback;
import com.medcare.clinic_backend.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    // Lấy tất cả feedback (Admin dùng)
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Feedback> getAll() {
        return feedbackService.getAllFeedbacks();
    }

    // Lấy feedback theo bác sĩ (Ai cũng xem được, không cần token)
    @GetMapping("/doctor/{doctorId}")
    public List<Feedback> getByDoctor(@PathVariable Integer doctorId) {
        return feedbackService.getFeedbacksByDoctor(doctorId);
    }

    // Gửi đánh giá (Chỉ bệnh nhân mới được gửi)
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public Feedback create(@RequestBody Feedback feedback, Authentication authentication) {
        return feedbackService.createFeedback(feedback, authentication.getName());
    }

    // Xóa đánh giá (Admin dùng để dọn rác)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable Integer id) {
        feedbackService.deleteFeedback(id);
    }
}
