package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.patient.AdminPatientDetailResponse;
import com.medcare.clinic_backend.entity.*;
import com.medcare.clinic_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPatientManagementServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminPatientManagementService service;

    private Patient patient;
    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(10);
        account.setUsername("trietminhpham79@gmail.com");
        account.setIsActive(true);
        account.setCreatedAt(LocalDateTime.of(2026, 6, 8, 10, 0));

        patient = new Patient();
        patient.setId(1);
        patient.setFullName("Phạm Minh Triết");
        patient.setEmail("trietminhpham79@gmail.com");
        patient.setPhone("0868663667");
        patient.setGender("Nam");
        patient.setDateOfBirth(LocalDate.of(2005, 1, 1));
        patient.setAddress("HUTECH University - Thu Duc Campus");
        patient.setAccount(account);
    }

    @Test
    void adminCanListSearchFilterAndViewDetail() {
        when(patientRepository.searchAdminPatientsByKeyword(eq("%triết%"), eq(true), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(patient)));
        when(patientRepository.searchAdminPatientsByKeyword(eq("%triết%"), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(patient)));
        when(appointmentRepository.countByPatientId(1)).thenReturn(3L);
        when(medicalRecordRepository.countByPatientId(1)).thenReturn(2L);
        when(invoiceRepository.countByMedicalRecordPatientId(1)).thenReturn(2L);
        when(patientRepository.findById(1)).thenReturn(Optional.of(patient));
        when(appointmentRepository.countByPatientIdAndStatus(1, "COMPLETED")).thenReturn(2L);
        when(appointmentRepository.countByPatientIdAndStatus(1, "CANCELLED")).thenReturn(1L);
        when(invoiceRepository.sumPaidAmountByPatientId(1)).thenReturn(500000.0);
        when(appointmentRepository.findTop5ByPatientIdOrderByAppointmentDateDesc(1)).thenReturn(List.of());
        when(medicalRecordRepository.findTop5ByPatientIdOrderByExaminationDateDesc(1)).thenReturn(List.of());

        var page = service.getPatients("triết", "ACTIVE", 0, 10, "newest");
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getFullName()).isEqualTo("Phạm Minh Triết");

        service.getPatients("triết", "INVALID_STATUS", 0, 10, "newest");
        verify(patientRepository).searchAdminPatientsByKeyword(eq("%triết%"), isNull(), any(PageRequest.class));

        AdminPatientDetailResponse detail = service.getPatientDetail(1);
        assertThat(detail.getStatistics().getAppointmentCount()).isEqualTo(3);
        assertThat(detail.getStatistics().getTotalPaidAmount()).isEqualTo(500000);
    }

    @Test
    void adminCanLockUnlockAndResetPassword() {
        when(patientRepository.findByIdWithAccount(1)).thenReturn(Optional.of(patient));
        when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("Bn@123")).thenReturn("ENC");

        var lockResponse = service.lockPatient(1);
        assertThat(account.getIsActive()).isFalse();
        assertThat(lockResponse.getMessage()).isEqualTo("Khóa tài khoản bệnh nhân thành công");
        assertThat(lockResponse.getPatientId()).isEqualTo(1);
        assertThat(lockResponse.getAccountId()).isEqualTo(10);
        assertThat(lockResponse.getIsActive()).isFalse();

        var unlockResponse = service.unlockPatient(1);
        assertThat(account.getIsActive()).isTrue();
        assertThat(unlockResponse.getMessage()).isEqualTo("Mở khóa tài khoản bệnh nhân thành công");
        assertThat(unlockResponse.getPatientId()).isEqualTo(1);
        assertThat(unlockResponse.getAccountId()).isEqualTo(10);
        assertThat(unlockResponse.getIsActive()).isTrue();

        var response = service.resetPassword(1, "Bn@123");
        assertThat(response.get("mustChangePassword")).isEqualTo(true);
        assertThat(response.get("temporaryPassword")).isEqualTo("Bn@123");
        assertThat(account.getMustChangePassword()).isTrue();
    }

    @Test
    void listReturnsIsActiveFromLinkedAccount() {
        when(patientRepository.findAdminPatients(isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(patient)));
        when(appointmentRepository.countByPatientId(1)).thenReturn(0L);
        when(medicalRecordRepository.countByPatientId(1)).thenReturn(0L);
        when(invoiceRepository.countByMedicalRecordPatientId(1)).thenReturn(0L);

        account.setIsActive(false);
        var page = service.getPatients(null, "ALL", 0, 10, "newest");

        assertThat(page.getContent().get(0).getIsActive()).isFalse();
        assertThat(page.getContent().get(0).getAccountId()).isEqualTo(10);
    }

    @Test
    void lockedStatusFilterMapsToInactiveAccounts() {
        when(patientRepository.findAdminPatients(eq(false), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(patient)));
        when(appointmentRepository.countByPatientId(1)).thenReturn(0L);
        when(medicalRecordRepository.countByPatientId(1)).thenReturn(0L);
        when(invoiceRepository.countByMedicalRecordPatientId(1)).thenReturn(0L);

        service.getPatients(null, "LOCKED", 0, 10, "newest");
        verify(patientRepository).findAdminPatients(eq(false), any(PageRequest.class));

        service.getPatients(null, "INACTIVE", 0, 10, "newest");
        verify(patientRepository, times(2)).findAdminPatients(eq(false), any(PageRequest.class));
    }

    @Test
    void statsAreReturnedForAdminCards() {
        when(patientRepository.count()).thenReturn(20L);
        when(patientRepository.countActivePatients()).thenReturn(18L);
        when(patientRepository.countLockedPatients()).thenReturn(2L);
        when(patientRepository.countNewPatientsBetween(any(), any())).thenReturn(5L);

        var stats = service.getStats();
        assertThat(stats.getTotalPatients()).isEqualTo(20L);
        assertThat(stats.getLockedPatients()).isEqualTo(2L);
    }
}