package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.feedback.MessageResponse;
import com.medcare.clinic_backend.dto.feedback.WebsiteFeedbackAdminResponse;
import com.medcare.clinic_backend.entity.WebsiteFeedback;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.FeedbackRepository;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.repository.WebsiteFeedbackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private PatientService patientService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private WebsiteFeedbackRepository websiteFeedbackRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void approveWebsiteFeedback_shouldApprovePendingFeedback() {
        WebsiteFeedback feedback = sampleWebsiteFeedback(1, "PENDING", false);
        when(websiteFeedbackRepository.findById(1)).thenReturn(Optional.of(feedback));

        MessageResponse response = feedbackService.approveWebsiteFeedback(1);

        assertEquals("Da duyet feedback.", response.getMessage());
        assertEquals("APPROVED", feedback.getStatus());
        assertTrue(Boolean.TRUE.equals(feedback.getIsApproved()));
        verify(websiteFeedbackRepository).save(feedback);
    }

    @Test
    void unhideWebsiteFeedback_shouldRestoreHiddenFeedbackWithOwnMessage() {
        WebsiteFeedback feedback = sampleWebsiteFeedback(2, "HIDDEN", false);
        when(websiteFeedbackRepository.findById(2)).thenReturn(Optional.of(feedback));

        MessageResponse response = feedbackService.unhideWebsiteFeedback(2);

        assertEquals("Da bo an feedback.", response.getMessage());
        assertEquals("APPROVED", feedback.getStatus());
        assertTrue(Boolean.TRUE.equals(feedback.getIsApproved()));
        verify(websiteFeedbackRepository).save(feedback);
    }

    @Test
    void hideWebsiteFeedback_shouldHideFeedback() {
        WebsiteFeedback feedback = sampleWebsiteFeedback(3, "APPROVED", true);
        when(websiteFeedbackRepository.findById(3)).thenReturn(Optional.of(feedback));

        MessageResponse response = feedbackService.hideWebsiteFeedback(3);

        assertEquals("Da an feedback.", response.getMessage());
        assertEquals("HIDDEN", feedback.getStatus());
        assertFalse(Boolean.TRUE.equals(feedback.getIsApproved()));
        verify(websiteFeedbackRepository).save(feedback);
    }

    @Test
    void deleteWebsiteFeedback_shouldDeleteAndReturnMessage() {
        when(websiteFeedbackRepository.existsById(4)).thenReturn(true);

        MessageResponse response = feedbackService.deleteWebsiteFeedback(4);

        assertEquals("Da xoa feedback.", response.getMessage());
        verify(websiteFeedbackRepository).deleteById(4);
    }

    @Test
    void getAllWebsiteFeedbacksForAdmin_shouldExposeActionFlagsByStatus() {
        WebsiteFeedback pending = sampleWebsiteFeedback(10, "PENDING", false);
        WebsiteFeedback approved = sampleWebsiteFeedback(11, "APPROVED", true);
        WebsiteFeedback hidden = sampleWebsiteFeedback(12, "HIDDEN", false);

        when(websiteFeedbackRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(pending, approved, hidden));

        List<WebsiteFeedbackAdminResponse> responses = feedbackService.getAllWebsiteFeedbacksForAdmin();

        WebsiteFeedbackAdminResponse pendingResponse = responses.get(0);
        assertEquals("Cho duyet", pendingResponse.getStatusDisplay());
        assertTrue(Boolean.TRUE.equals(pendingResponse.getCanApprove()));
        assertTrue(Boolean.TRUE.equals(pendingResponse.getCanHide()));
        assertFalse(Boolean.TRUE.equals(pendingResponse.getCanUnhide()));
        assertFalse(Boolean.TRUE.equals(pendingResponse.getVisibleOnHomepage()));

        WebsiteFeedbackAdminResponse approvedResponse = responses.get(1);
        assertEquals("Da duyet", approvedResponse.getStatusDisplay());
        assertFalse(Boolean.TRUE.equals(approvedResponse.getCanApprove()));
        assertTrue(Boolean.TRUE.equals(approvedResponse.getCanHide()));
        assertFalse(Boolean.TRUE.equals(approvedResponse.getCanUnhide()));
        assertTrue(Boolean.TRUE.equals(approvedResponse.getVisibleOnHomepage()));

        WebsiteFeedbackAdminResponse hiddenResponse = responses.get(2);
        assertEquals("Da an", hiddenResponse.getStatusDisplay());
        assertFalse(Boolean.TRUE.equals(hiddenResponse.getCanApprove()));
        assertFalse(Boolean.TRUE.equals(hiddenResponse.getCanHide()));
        assertTrue(Boolean.TRUE.equals(hiddenResponse.getCanUnhide()));
        assertFalse(Boolean.TRUE.equals(hiddenResponse.getVisibleOnHomepage()));
    }

    private WebsiteFeedback sampleWebsiteFeedback(Integer id, String status, boolean approved) {
        WebsiteFeedback feedback = new WebsiteFeedback();
        feedback.setId(id);
        feedback.setFullName("Tester");
        feedback.setEmail("tester@example.com");
        feedback.setRating(5);
        feedback.setComment("Rat tot");
        feedback.setStatus(status);
        feedback.setIsApproved(approved);
        feedback.setCreatedAt(LocalDateTime.of(2026, 6, 2, 20, 0));
        return feedback;
    }
}
