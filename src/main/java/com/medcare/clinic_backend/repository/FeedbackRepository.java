package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    // Lấy toàn bộ đánh giá của 1 bác sĩ cụ thể (sắp xếp mới nhất lên đầu)
    List<Feedback> findByDoctorIdOrderByCreatedAtDesc(Integer doctorId);

    List<Feedback> findAllByOrderByCreatedAtDesc();

}
