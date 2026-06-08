package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AppointmentSlotResponse;
import com.medcare.clinic_backend.dto.doctor.CompleteAppointmentRequest;
import com.medcare.clinic_backend.dto.doctor.CompleteAppointmentResponse;
import com.medcare.clinic_backend.dto.doctor.CreateFollowUpRequest;
import com.medcare.clinic_backend.dto.doctor.CreateFollowUpResponse;
import com.medcare.clinic_backend.dto.doctor.DoctorPatientMedicalRecordsResponse;
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
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getPatientMedicalRecords_shouldSupportLegacyAndCanonicalRoutes() throws Exception {
        DoctorPatientMedicalRecordsResponse response = new DoctorPatientMedicalRecordsResponse(
                new DoctorPatientMedicalRecordsResponse.PatientProfile(15, "Pham Minh Triet", null, null, null, null, null, null),
                List.of()
        );
        when(doctorPortalService.getPatientMedicalRecords("doctor1", 15)).thenReturn(response);

        mockMvc.perform(get("/api/doctor/medical-records/15")
                        .principal(new TestingAuthenticationToken("doctor1", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient.id").value(15))
                .andExpect(jsonPath("$.records").isArray());

        mockMvc.perform(get("/api/doctor/medical-records/patients/15")
                        .principal(new TestingAuthenticationToken("doctor1", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient.id").value(15))
                .andExpect(jsonPath("$.records").isArray());

        verify(doctorPortalService, org.mockito.Mockito.times(2)).getPatientMedicalRecords("doctor1", 15);
    }

    @Test
    void createFollowUp_shouldAcceptCanonicalAndLegacyAliasesTogether() throws Exception {
        when(doctorPortalService.createFollowUp(eq("doctor1"), eq(95), any(CreateFollowUpRequest.class)))
                .thenReturn(new CreateFollowUpResponse(
                        503,
                        LocalDate.of(2026, 6, 14),
                        LocalTime.of(8, 0),
                        "Tái khám",
                        "Chưa khám",
                        "Chưa thanh toán",
                        100000.0
                ));

        mockMvc.perform(post("/api/doctor/medical-records/95/follow-up")
                        .principal(new TestingAuthenticationToken("doctor1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "followUpDate": "2026-06-14",
                                  "followUpTime": "08:00",
                                  "followUpNote": "Tai kham sau 1 tuan",
                                  "date": "2026-06-14",
                                  "time": "08:00",
                                  "appointmentDate": "2026-06-14",
                                  "appointmentTime": "08:00",
                                  "note": "Tai kham sau 1 tuan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(503))
                .andExpect(jsonPath("$.appointmentDate").value("2026-06-14"))
                .andExpect(jsonPath("$.appointmentTime").value("08:00:00"));

        ArgumentCaptor<CreateFollowUpRequest> requestCaptor = ArgumentCaptor.forClass(CreateFollowUpRequest.class);
        verify(doctorPortalService).createFollowUp(eq("doctor1"), eq(95), requestCaptor.capture());
        assertEquals("2026-06-14", requestCaptor.getValue().getFollowUpDate());
        assertEquals("08:00", requestCaptor.getValue().getFollowUpTime());
        assertEquals("Tai kham sau 1 tuan", requestCaptor.getValue().getNote());
    }

    @Test
    void createFollowUp_shouldMapPersistenceSystemErrorToCanonical400Payload() throws Exception {
        when(doctorPortalService.createFollowUp(eq("doctor1"), eq(96), any(CreateFollowUpRequest.class)))
                .thenThrow(new UnexpectedRollbackException("Constraint failed at commit"));

        mockMvc.perform(post("/api/doctor/medical-records/96/follow-up")
                        .principal(new TestingAuthenticationToken("doctor1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "followUpDate": "2026-06-14",
                                  "followUpTime": "08:00",
                                  "note": "Tai kham sau 1 tuan"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FOLLOW_UP_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Khong the tao lich tai kham do du lieu lien ket hoac rang buoc luu tru khong hop le."))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void completeAppointment_shouldAcceptTopLevelFollowUpCompatibilityFields() throws Exception {
        when(doctorPortalService.completeAppointment(eq("doctor1"), eq(77), any(CompleteAppointmentRequest.class)))
                .thenReturn(new CompleteAppointmentResponse(
                        "Hoan tat kham thanh cong",
                        77,
                        "Khám bệnh",
                        "Đã khám",
                        null,
                        null
                ));

        mockMvc.perform(post("/api/doctor/appointments/77/complete")
                        .principal(new TestingAuthenticationToken("doctor1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diagnosis": "Cam cum",
                                  "needFollowUp": true,
                                  "followUpDate": "14-06-2026",
                                  "followUpTime": "08:00:00",
                                  "followUpNote": "Tai kham sau 1 tuan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(77));

        ArgumentCaptor<CompleteAppointmentRequest> requestCaptor = ArgumentCaptor.forClass(CompleteAppointmentRequest.class);
        verify(doctorPortalService).completeAppointment(eq("doctor1"), eq(77), requestCaptor.capture());
        assertEquals(Boolean.TRUE, requestCaptor.getValue().getNeedFollowUp());
        assertEquals("14-06-2026", requestCaptor.getValue().getFollowUpDate());
        assertEquals("08:00:00", requestCaptor.getValue().getFollowUpTime());
        assertEquals("Tai kham sau 1 tuan", requestCaptor.getValue().getFollowUpNote());
    }

    @Test
    void completeAppointment_shouldAcceptNestedFollowUpAliases() throws Exception {
        when(doctorPortalService.completeAppointment(eq("doctor1"), eq(78), any(CompleteAppointmentRequest.class)))
                .thenReturn(new CompleteAppointmentResponse(
                        "Hoan tat kham thanh cong",
                        78,
                        "Khám bệnh",
                        "Đã khám",
                        null,
                        null
                ));

        mockMvc.perform(post("/api/doctor/appointments/78/complete")
                        .principal(new TestingAuthenticationToken("doctor1", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diagnosis": "Cam cum",
                                  "followUp": {
                                    "needFollowUp": true,
                                    "date": "2026-06-14",
                                    "time": "08:00",
                                    "followUpNote": "Tai kham sau 1 tuan"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(78));

        ArgumentCaptor<CompleteAppointmentRequest> requestCaptor = ArgumentCaptor.forClass(CompleteAppointmentRequest.class);
        verify(doctorPortalService).completeAppointment(eq("doctor1"), eq(78), requestCaptor.capture());
        assertEquals(Boolean.TRUE, requestCaptor.getValue().getFollowUp().getNeedFollowUp());
        assertEquals("2026-06-14", requestCaptor.getValue().getFollowUp().getFollowUpDate());
        assertEquals("08:00", requestCaptor.getValue().getFollowUp().getFollowUpTime());
        assertEquals("Tai kham sau 1 tuan", requestCaptor.getValue().getFollowUp().getNote());
    }

    @Test
    void getFollowUpSlots_shouldExposeDoctorFollowUpAvailability() throws Exception {
        when(doctorPortalService.getFollowUpSlots("doctor1", LocalDate.of(2026, 6, 14)))
                .thenReturn(List.of(
                        buildSlot("08:00", 5, 1, 4, true),
                        buildSlot("14:00", 5, 5, 0, false)
                ));

        mockMvc.perform(get("/api/doctor/follow-up-slots?date=2026-06-14")
                        .principal(new TestingAuthenticationToken("doctor1", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].time").value("08:00"))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[0].remainingSlots").value(4))
                .andExpect(jsonPath("$[0].startTime").value("2026-06-14T08:00:00"))
                .andExpect(jsonPath("$[0].maxPatients").value(5))
                .andExpect(jsonPath("$[0].disabled").value(false))
                .andExpect(jsonPath("$[1].available").value(false))
                .andExpect(jsonPath("$[1].disabled").value(true))
                .andExpect(jsonPath("$[1].disabledReason").value("FULL"));
    }

    @Test
    void getFollowUpSlotsByAppointment_shouldExposeSharedSlotAvailability() throws Exception {
        when(doctorPortalService.getFollowUpSlotsByAppointmentId(
                "doctor1",
                12,
                LocalDate.of(2026, 6, 12)
        )).thenReturn(List.of(
                buildSlot("08:00", 5, 5, 0, false),
                buildSlot("09:00", 5, 0, 5, true)
        ));

        mockMvc.perform(get("/api/doctor/appointments/12/follow-up-slots?date=2026-06-12")
                        .principal(new TestingAuthenticationToken("doctor1", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].time").value("08:00"))
                .andExpect(jsonPath("$[0].available").value(false))
                .andExpect(jsonPath("$[1].time").value("09:00"))
                .andExpect(jsonPath("$[1].available").value(true));
    }

    @Test
    void getFollowUpSlotsByMedicalRecord_shouldExposeSharedSlotAvailability() throws Exception {
        when(doctorPortalService.getFollowUpSlotsByMedicalRecordId(
                "doctor1",
                91,
                LocalDate.of(2026, 6, 11)
        )).thenReturn(List.of(
                buildSlot("08:00", 5, 0, 5, true),
                buildSlot("09:00", 5, 0, 5, true)
        ));

        mockMvc.perform(get("/api/doctor/medical-records/91/follow-up-slots?date=11-06-2026")
                        .principal(new TestingAuthenticationToken("doctor1", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].startTime").exists())
                .andExpect(jsonPath("$[0].disabled").value(false));
    }

    private AppointmentSlotResponse buildSlot(
            String time,
            int totalSlots,
            long bookedSlots,
            long remainingSlots,
            boolean available
    ) {
        LocalDate date = LocalDate.of(2026, 6, 14);
        LocalDateTime start = date.atTime(LocalTime.parse(time));
        LocalDateTime end = start.plusMinutes(time.endsWith(":30") ? 30 : 60);
        String shift = start.getHour() < 12 ? "morning" : "afternoon";
        return new AppointmentSlotResponse(
                time,
                start,
                end,
                shift,
                totalSlots,
                bookedSlots,
                remainingSlots,
                available,
                available ? null : "FULL"
        );
    }
}
