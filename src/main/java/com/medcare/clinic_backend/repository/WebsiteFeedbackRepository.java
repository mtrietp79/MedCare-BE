package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.WebsiteFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebsiteFeedbackRepository extends JpaRepository<WebsiteFeedback, Integer> {

    List<WebsiteFeedback> findByStatusOrderByCreatedAtDesc(String status);

    List<WebsiteFeedback> findAllByOrderByCreatedAtDesc();
}
