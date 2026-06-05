package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.doctor.CreateFollowUpRequest;
import com.medcare.clinic_backend.dto.doctor.CreateFollowUpResponse;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.exception.GlobalExceptionHandler;
import com.medcare.clinic_backend.service.DoctorPortalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DoctorPortalControllerTest {

    @Mock
    private DoctorPortalService doctorPortalService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DoctorPortalController controller = new DoctorPortalController();
        ReflectionTestUtils.setField(controller, "doctorPortalService", doctorPortalService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createFollowUp_shouldAcceptDateAndTimeAliases() throws Exception {
        when(doctorPortalService.createFollowUp(eq("doctor1"), eq(91), any(CreateFollowUpRequest.class)))
                .thenReturn(new CreateFollowUpResponse(
                        501,
                        LocalDate.of(2026, 6, 6),
                        LocalTime.of(9, 0),
                        "T\u00e1i kh\u00e1m",
                        "Ch\u01b0a kh\u00e1m",
                        "Ch\u01b0a thanh to\u00e1n",
                        100000.0
                ));

        mockMvc.perform(post("/api/doctor/medical-records/91/follow-up")
                        .principal(new TestingAuthenticationToken("doctor1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-06-06",
                                  "time": "09:00",
                                  "followUpNote": "Tai kham sau 1 tuan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(501))
                .andExpect(jsonPath("$.appointmentDate").value("2026-06-06"))
                .andExpect(jsonPath("$.appointmentTime").value("09:00:00"))
                .andExpect(jsonPath("$.type").value("T\u00e1i kh\u00e1m"))
                .andExpect(jsonPath("$.status").value("Ch\u01b0a kh\u00e1m"))
                .andExpect(jsonPath("$.paymentStatus").value("Ch\u01b0a thanh to\u00e1n"))
                .andExpect(jsonPath("$.consultationFee").value(100000.0))
                .andExpect(jsonPath("$.appointmentId").doesNotExist());

        ArgumentCaptor<CreateFollowUpRequest> requestCaptor = ArgumentCaptor.forClass(CreateFollowUpRequest.class);
        verify(doctorPortalService).createFollowUp(eq("doctor1"), eq(91), requestCaptor.capture());
        assertEquals("2026-06-06", requestCaptor.getValue().getFollowUpDate());
        assertEquals("09:00", requestCaptor.getValue().getFollowUpTime());
        assertEquals("Tai kham sau 1 tuan", requestCaptor.getValue().getNote());
    }

    @Test
    void createFollowUp_shouldAcceptAppointmentDateAndTimeAliases() throws Exception {
        when(doctorPortalService.createFollowUp(eq("doctor1"), eq(92), any(CreateFollowUpRequest.class)))
                .thenReturn(new CreateFollowUpResponse(
                        502,
                        LocalDate.of(2026, 6, 7),
                        LocalTime.of(10, 0),
                        "T\u00e1i kh\u00e1m",
                        "Ch\u01b0a kh\u00e1m",
                        "Ch\u01b0a thanh to\u00e1n",
                        100000.0
                ));

        mockMvc.perform(post("/api/doctor/medical-records/92/follow-up")
                        .principal(new TestingAuthenticationToken("doctor1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appointmentDate": "2026-06-07",
                                  "appointmentTime": "10:00",
                                  "note": "Can mang ket qua xet nghiem"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(502));

        ArgumentCaptor<CreateFollowUpRequest> requestCaptor = ArgumentCaptor.forClass(CreateFollowUpRequest.class);
        verify(doctorPortalService).createFollowUp(eq("doctor1"), eq(92), requestCaptor.capture());
        assertEquals("2026-06-07", requestCaptor.getValue().getFollowUpDate());
        assertEquals("10:00", requestCaptor.getValue().getFollowUpTime());
        assertEquals("Can mang ket qua xet nghiem", requestCaptor.getValue().getNote());
    }

    @Test
    void createFollowUp_shouldReturnCanonical400PayloadForValidationException() throws Exception {
        when(doctorPortalService.createFollowUp(eq("doctor1"), eq(93), any(CreateFollowUpRequest.class)))
                .thenThrow(new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Khung gio tai kham da full.",
                        "FOLLOW_UP_VALIDATION_ERROR",
                        Map.of("followUpTime", "Khung gio nay da du so luong benh nhan toi da.")
                ));

        mockMvc.perform(post("/api/doctor/medical-records/93/follow-up")
                        .principal(new TestingAuthenticationToken("doctor1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "followUpDate": "2026-06-07",
                                  "followUpTime": "09:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Khung gio tai kham da full."))
                .andExpect(jsonPath("$.code").value("FOLLOW_UP_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.followUpTime")
                        .value("Khung gio nay da du so luong benh nhan toi da."));
    }

    @Test
    void createFollowUp_shouldReturnCanonical400PayloadForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/doctor/medical-records/94/follow-up")
                        .principal(new TestingAuthenticationToken("doctor1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "followUpDate": "2026-06-07",
                                  "followUpTime":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Du lieu gui len khong dung dinh dang JSON hoac sai kieu du lieu."))
                .andExpect(jsonPath("$.code").value("FOLLOW_UP_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isMap());

        verifyNoInteractions(doctorPortalService);
    }
}
