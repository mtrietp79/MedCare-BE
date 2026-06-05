package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.exception.GlobalExceptionHandler;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.service.AppointmentService;
import com.medcare.clinic_backend.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AppointmentController controller = new AppointmentController();
        ReflectionTestUtils.setField(controller, "appointmentService", appointmentService);
        ReflectionTestUtils.setField(controller, "patientRepository", patientRepository);
        ReflectionTestUtils.setField(controller, "doctorRepository", doctorRepository);
        ReflectionTestUtils.setField(controller, "paymentService", paymentService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getById_shouldExposeParentAppointmentIdWithoutSerializingParentAppointment() throws Exception {
        Patient currentPatient = new Patient();
        currentPatient.setId(15);
        Account account = new Account();
        account.setUsername("patient1");
        currentPatient.setAccount(account);

        Specialty specialty = new Specialty();
        specialty.setId(20);
        specialty.setName("Noi tong quat");

        Doctor doctor = new Doctor();
        doctor.setId(7);
        doctor.setFullName("PGS. TS. BS. Nguyen Minh Tu");
        doctor.setSpecialty(specialty);

        Appointment parentAppointment = new Appointment();
        parentAppointment.setId(1);
        parentAppointment.setAppointmentCode("PKB-PARENT");
        parentAppointment.setPatient(currentPatient);
        parentAppointment.setDoctor(doctor);
        parentAppointment.setSpecialty(specialty);
        parentAppointment.setAppointmentDate(LocalDateTime.of(2026, 6, 5, 7, 30));
        parentAppointment.setAppointmentType("Khám bệnh");

        Appointment followUpAppointment = new Appointment();
        followUpAppointment.setId(2);
        followUpAppointment.setAppointmentCode("PKB-FOLLOW-UP");
        followUpAppointment.setPatient(currentPatient);
        followUpAppointment.setDoctor(doctor);
        followUpAppointment.setSpecialty(specialty);
        followUpAppointment.setAppointmentDate(LocalDateTime.of(2026, 6, 6, 9, 0));
        followUpAppointment.setAppointmentType("Tái khám");
        followUpAppointment.setStatus("COMPLETED");
        followUpAppointment.setPaymentStatus("UNPAID");
        followUpAppointment.setFollowUpNote("Tai kham sau 1 ngay");
        followUpAppointment.setParentAppointment(parentAppointment);

        when(patientRepository.findByAccount_Username("patient1")).thenReturn(Optional.of(currentPatient));
        when(appointmentService.getAppointmentByIdForPatient(2, 15)).thenReturn(followUpAppointment);

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("patient1", null, "ROLE_PATIENT"));

        mockMvc.perform(get("/api/appointments/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.appointmentType").value("Tái khám"))
                .andExpect(jsonPath("$.followUpNote").value("Tai kham sau 1 ngay"))
                .andExpect(jsonPath("$.parentAppointmentId").value(1))
                .andExpect(jsonPath("$.parentAppointment").doesNotExist());

        verify(appointmentService).getAppointmentByIdForPatient(2, 15);
    }
}
