package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Feedback;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAll();
    }

    public List<Feedback> getFeedbacksByDoctor(Integer doctorId) {
        return feedbackRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
    }

    public Feedback createFeedback(Feedback feedback, String username) {
        if (feedback == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu feedback khong hop le.");
        }
        if (feedback.getDoctor() == null || feedback.getDoctor().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Feedback phai co doctorId.");
        }
        if (feedback.getRating() == null || feedback.getRating() < 1 || feedback.getRating() > 5) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Rating chi hop le tu 1 den 5.");
        }

        Patient patient = patientService.getPatientByAccountUsername(username);
        if (!appointmentRepository.existsByDoctorIdAndPatientId(feedback.getDoctor().getId(), patient.getId())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Ban chi duoc danh gia bac si ma ban da co lich hen."
            );
        }

        feedback.setPatient(patient);
        if (feedback.getComment() != null) {
            feedback.setComment(feedback.getComment().trim());
        }
        return feedbackRepository.save(feedback);
    }

    public void deleteFeedback(Integer id) {
        if (!feedbackRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay feedback ID: " + id);
        }
        feedbackRepository.deleteById(id);
    }
}
