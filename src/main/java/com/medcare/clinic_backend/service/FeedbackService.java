package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Feedback;
import com.medcare.clinic_backend.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    // Lấy tất cả feedback (Dành cho Admin quản lý)
    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAll();
    }

    // Lấy feedback của 1 bác sĩ (Dành cho trang chi tiết bác sĩ)
    public List<Feedback> getFeedbacksByDoctor(Integer doctorId) {
        return feedbackRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
    }

    // Tạo feedback mới
    public Feedback createFeedback(Feedback feedback) {
        // Có thể thêm logic kiểm tra xem bệnh nhân đã khám bác sĩ này chưa (tùy ông)
        return feedbackRepository.save(feedback);
    }

    // Xóa feedback (Admin xóa nếu comment chửi bậy/spam)
    public void deleteFeedback(Integer id) {
        feedbackRepository.deleteById(id);
    }
}