package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.feedback.*;
import com.medcare.clinic_backend.entity.*;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FeedbackService {
    private static final String WEBSITE_FEEDBACK_PENDING = "PENDING";
    private static final String WEBSITE_FEEDBACK_APPROVED = "APPROVED";
    private static final String WEBSITE_FEEDBACK_HIDDEN = "HIDDEN";

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private WebsiteFeedbackRepository websiteFeedbackRepository;

    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Feedback> getFeedbacksByDoctor(Integer doctorId) {
        return feedbackRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
    }

    @Transactional
    public MessageResponse createDoctorFeedback(String username, DoctorFeedbackCreateRequest request) {
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu danh gia bac si khong hop le.");
        }
        if (request.getAppointmentId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "appointmentId khong duoc de trong.");
        }
        validateRating(request.getRating());

        Patient patient = patientService.getPatientByAccountUsername(username);
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich hen ID: " + request.getAppointmentId()));

        if (appointment.getPatient() == null || !Objects.equals(appointment.getPatient().getId(), patient.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Ban khong co quyen danh gia lich hen nay.");
        }
        if (!isCompletedAppointmentStatus(appointment.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ban chi co the danh gia sau khi da kham xong.");
        }
        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen nay khong co thong tin bac si de danh gia.");
        }
        if (feedbackRepository.existsByAppointmentId(appointment.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen nay da duoc danh gia.");
        }

        Feedback feedback = new Feedback();
        feedback.setAppointment(appointment);
        feedback.setDoctor(appointment.getDoctor());
        feedback.setPatient(patient);
        feedback.setRating(request.getRating());
        feedback.setComment(trimToNull(request.getComment()));
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);

        syncDoctorRating(appointment.getDoctor());
        return new MessageResponse("Danh gia bac si thanh cong");
    }

    @Transactional(readOnly = true)
    public List<DoctorFeedbackResponse> getDoctorFeedbackResponses(Integer doctorId) {
        ensureDoctorExists(doctorId);
        return feedbackRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId).stream()
                .map(feedback -> new DoctorFeedbackResponse(
                        feedback.getId(),
                        feedback.getPatient() == null ? null : feedback.getPatient().getFullName(),
                        feedback.getRating(),
                        feedback.getComment(),
                        feedback.getCreatedAt() == null ? null : feedback.getCreatedAt().toLocalDate()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DoctorRatingSummaryResponse getDoctorRatingSummary(Integer doctorId) {
        ensureDoctorExists(doctorId);
        Double average = feedbackRepository.findAverageRatingByDoctorId(doctorId);
        double normalizedAverage = average == null ? 0.0 : Math.round(average * 10.0) / 10.0;
        long total = feedbackRepository.countByDoctorId(doctorId);
        return new DoctorRatingSummaryResponse(doctorId, normalizedAverage, total);
    }

    @Transactional(readOnly = true)
    public CanFeedbackResponse canPatientFeedbackAppointment(String username, Integer appointmentId) {
        Patient patient = patientService.getPatientByAccountUsername(username);
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return new CanFeedbackResponse(false, "Khong tim thay lich hen.");
        }
        if (appointment.getPatient() == null || !Objects.equals(appointment.getPatient().getId(), patient.getId())) {
            return new CanFeedbackResponse(false, "Ban khong co quyen danh gia lich hen nay.");
        }
        if (!isCompletedAppointmentStatus(appointment.getStatus())) {
            return new CanFeedbackResponse(false, "Ban chi co the danh gia sau khi da kham xong.");
        }
        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            return new CanFeedbackResponse(false, "Lich hen khong co thong tin bac si.");
        }
        if (feedbackRepository.existsByAppointmentId(appointmentId)) {
            return new CanFeedbackResponse(false, "Lich hen nay da duoc danh gia.");
        }
        return new CanFeedbackResponse(true, null);
    }

    @Transactional
    public MessageResponse createWebsiteFeedback(WebsiteFeedbackCreateRequest request, String username, boolean hasPatientRole) {
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu feedback website khong hop le.");
        }
        validateRating(request.getRating());

        Patient patient = null;
        if (hasPatientRole && username != null && !username.isBlank()) {
            patient = patientRepository.findByAccount_Username(username).orElse(null);
        }

        String fullName = trimToNull(request.getFullName());
        String email = trimToNull(request.getEmail());
        if (patient != null) {
            if (fullName == null) {
                fullName = trimToNull(patient.getFullName());
            }
            if (email == null) {
                email = trimToNull(patient.getEmail());
            }
        }
        if (fullName == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "fullName khong duoc de trong.");
        }

        WebsiteFeedback feedback = new WebsiteFeedback();
        feedback.setPatient(patient);
        feedback.setFullName(fullName);
        feedback.setEmail(email);
        feedback.setRating(request.getRating());
        feedback.setComment(trimToNull(request.getComment()));
        feedback.setStatus(WEBSITE_FEEDBACK_PENDING);
        feedback.setIsApproved(false);
        feedback.setCreatedAt(LocalDateTime.now());
        websiteFeedbackRepository.save(feedback);

        return new MessageResponse("Cam on ban da gui danh gia. Danh gia se duoc hien thi sau khi duoc duyet.");
    }

    @Transactional(readOnly = true)
    public List<WebsiteFeedbackPublicResponse> getApprovedWebsiteFeedbacks() {
        return websiteFeedbackRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(feedback -> WEBSITE_FEEDBACK_APPROVED.equals(resolveWebsiteFeedbackStatus(feedback)))
                .map(this::toWebsitePublicResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebsiteFeedbackAdminResponse> getAllWebsiteFeedbacksForAdmin() {
        return websiteFeedbackRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toWebsiteAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse approveWebsiteFeedback(Integer id) {
        WebsiteFeedback feedback = websiteFeedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay website feedback ID: " + id));
        String currentStatus = resolveWebsiteFeedbackStatus(feedback);
        if (WEBSITE_FEEDBACK_APPROVED.equals(currentStatus)) {
            return new MessageResponse("Feedback da o trang thai APPROVED.");
        }
        if (!WEBSITE_FEEDBACK_PENDING.equals(currentStatus) && !WEBSITE_FEEDBACK_HIDDEN.equals(currentStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chi co the duyet feedback o trang thai PENDING hoac HIDDEN.");
        }
        feedback.setStatus(WEBSITE_FEEDBACK_APPROVED);
        feedback.setIsApproved(true);
        websiteFeedbackRepository.save(feedback);
        return new MessageResponse("Duyet feedback thanh cong");
    }

    @Transactional
    public MessageResponse hideWebsiteFeedback(Integer id) {
        WebsiteFeedback feedback = websiteFeedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay website feedback ID: " + id));
        String currentStatus = resolveWebsiteFeedbackStatus(feedback);
        if (WEBSITE_FEEDBACK_HIDDEN.equals(currentStatus)) {
            return new MessageResponse("Feedback da o trang thai HIDDEN.");
        }
        feedback.setStatus(WEBSITE_FEEDBACK_HIDDEN);
        feedback.setIsApproved(false);
        websiteFeedbackRepository.save(feedback);
        return new MessageResponse("An feedback thanh cong");
    }

    @Transactional
    public MessageResponse deleteWebsiteFeedback(Integer id) {
        if (!websiteFeedbackRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay website feedback ID: " + id);
        }
        websiteFeedbackRepository.deleteById(id);
        return new MessageResponse("Xoa feedback thanh cong");
    }

    public void deleteFeedback(Integer id) {
        if (!feedbackRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay feedback ID: " + id);
        }
        Feedback feedback = feedbackRepository.findById(id).orElse(null);
        feedbackRepository.deleteById(id);
        if (feedback != null && feedback.getDoctor() != null) {
            syncDoctorRating(feedback.getDoctor());
        }
    }

    private void syncDoctorRating(Doctor doctor) {
        if (doctor == null || doctor.getId() == null) {
            return;
        }
        Double avg = feedbackRepository.findAverageRatingByDoctorId(doctor.getId());
        double normalized = avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;
        if (doctor.getRating() == null || Double.compare(doctor.getRating(), normalized) != 0) {
            doctor.setRating(normalized);
            doctorRepository.save(doctor);
        }
    }

    private void ensureDoctorExists(Integer doctorId) {
        if (doctorId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "doctorId khong duoc de trong.");
        }
        if (!doctorRepository.existsById(doctorId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + doctorId);
        }
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Rating chi hop le tu 1 den 5.");
        }
    }

    private boolean isCompletedAppointmentStatus(String status) {
        String normalized = foldText(status);
        return normalized != null && (normalized.contains("completed") || normalized.contains("dakham"));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String foldText(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String noAccent = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccent
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private WebsiteFeedbackPublicResponse toWebsitePublicResponse(WebsiteFeedback feedback) {
        return new WebsiteFeedbackPublicResponse(
                feedback.getId(),
                resolveWebsiteFeedbackDisplayName(feedback),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt() == null ? null : feedback.getCreatedAt().toLocalDate()
        );
    }

    private WebsiteFeedbackAdminResponse toWebsiteAdminResponse(WebsiteFeedback feedback) {
        return new WebsiteFeedbackAdminResponse(
                feedback.getId(),
                feedback.getPatient() == null ? null : feedback.getPatient().getId(),
                resolveWebsiteFeedbackDisplayName(feedback),
                feedback.getEmail(),
                feedback.getRating(),
                feedback.getComment(),
                resolveWebsiteFeedbackStatus(feedback),
                feedback.getCreatedAt()
        );
    }

    private String resolveWebsiteFeedbackStatus(WebsiteFeedback feedback) {
        if (feedback == null) {
            return WEBSITE_FEEDBACK_PENDING;
        }
        String status = trimToNull(feedback.getStatus());
        if (status == null) {
            boolean approvedByLegacyFlag = Boolean.TRUE.equals(feedback.getIsApproved());
            return approvedByLegacyFlag ? WEBSITE_FEEDBACK_APPROVED : WEBSITE_FEEDBACK_PENDING;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case WEBSITE_FEEDBACK_PENDING, WEBSITE_FEEDBACK_APPROVED, WEBSITE_FEEDBACK_HIDDEN -> normalized;
            default -> WEBSITE_FEEDBACK_PENDING;
        };
    }

    private String resolveWebsiteFeedbackDisplayName(WebsiteFeedback feedback) {
        String fullName = trimToNull(feedback.getFullName());
        if (fullName != null) {
            return fullName;
        }
        if (feedback.getPatient() != null) {
            return trimToNull(feedback.getPatient().getFullName());
        }
        return null;
    }
}
