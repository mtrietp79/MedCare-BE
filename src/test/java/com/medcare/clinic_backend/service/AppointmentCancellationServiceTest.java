package com.medcare.clinic_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationActionRequest;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationActionResponse;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationRequestDetailResponse;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationRequestListItemResponse;
import com.medcare.clinic_backend.dto.cancellation.CreateCancellationRequestDto;
import com.medcare.clinic_backend.dto.cancellation.CreateCancellationRequestResponse;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.AppointmentCancellationRequest;
import com.medcare.clinic_backend.entity.AppointmentCancellationRequestStatus;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.repository.AppointmentCancellationRequestRepository;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentCancellationServiceTest {

    @Mock
    private AppointmentCancellationRequestRepository cancellationRequestRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AppointmentCancellationService appointmentCancellationService;

    @Test
    void createCancellationRequest_shouldCreatePendingRequestAndUpdateAppointment() {
        Patient patient = patient(7, "Nguyen Van A");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment appointment = appointment(10, patient, doctor, "CONFIRMED", "PAID_ONLINE", 270000.0);

        CreateCancellationRequestDto request = new CreateCancellationRequestDto();
        request.setCancelReason("Toi ban dot xuat");
        request.setBankName("Vietcombank");
        request.setBankAccountNumber("0123456789");
        request.setBankAccountHolder("NGUYEN VAN A");
        request.setPatientNote("Mong ho tro hoan tien");

        TransactionLog paidLog = new TransactionLog();
        paidLog.setAmount(270000.0);
        paidLog.setResponseCode("00");

        when(appointmentRepository.findByIdAndPatientId(10, 7)).thenReturn(Optional.of(appointment));
        when(cancellationRequestRepository.existsByAppointmentIdAndStatusIn(10, Set.of(
                AppointmentCancellationRequestStatus.PENDING,
                AppointmentCancellationRequestStatus.APPROVED,
                AppointmentCancellationRequestStatus.REFUNDED
        ))).thenReturn(false);
        when(transactionLogRepository.findTopByAppointmentIdAndResponseCodeOrderByCreatedAtDesc(10, "00"))
                .thenReturn(paidLog);
        when(invoiceRepository.findFirstByAppointment_IdOrderByCreatedAtDesc(10)).thenReturn(Optional.empty());
        when(cancellationRequestRepository.save(any(AppointmentCancellationRequest.class))).thenAnswer(invocation -> {
            AppointmentCancellationRequest saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        CreateCancellationRequestResponse response = appointmentCancellationService
                .createCancellationRequest(10, 7, request);

        assertEquals(1, response.getRequestId());
        assertEquals(10, response.getAppointmentId());
        assertEquals(AppointmentCancellationRequestStatus.PENDING, response.getStatus());

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(appointmentCaptor.capture());
        assertEquals("CANCEL_REQUESTED", appointmentCaptor.getValue().getStatus());
        assertEquals("REFUND_PENDING", appointmentCaptor.getValue().getPaymentStatus());

        ArgumentCaptor<AppointmentCancellationRequest> requestCaptor =
                ArgumentCaptor.forClass(AppointmentCancellationRequest.class);
        verify(cancellationRequestRepository).save(requestCaptor.capture());
        AppointmentCancellationRequest savedRequest = requestCaptor.getValue();
        assertEquals("Toi ban dot xuat", savedRequest.getCancelReason());
        assertEquals("Mong ho tro hoan tien", savedRequest.getPatientNote());
        assertEquals("Vietcombank", savedRequest.getBankName());
        assertEquals("0123456789", savedRequest.getBankAccountNumber());
        assertEquals("NGUYEN VAN A", savedRequest.getBankAccountHolder());
        assertEquals(AppointmentCancellationRequestStatus.PENDING, savedRequest.getStatus());
    }

    @Test
    void createCancellationRequest_shouldDeserializeSnakeCasePayload() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        CreateCancellationRequestDto request = objectMapper.readValue("""
                {
                  "cancel_reason": "Ban dot xuat",
                  "patient_note": "Mong ho tro",
                  "bank_name": "Vietcombank",
                  "bank_account_number": "0123456789",
                  "bank_account_holder": "NGUYEN VAN A"
                }
                """, CreateCancellationRequestDto.class);

        assertEquals("Ban dot xuat", request.getCancelReason());
        assertEquals("Mong ho tro", request.getPatientNote());
        assertEquals("Vietcombank", request.getBankName());
        assertEquals("0123456789", request.getBankAccountNumber());
        assertEquals("NGUYEN VAN A", request.getBankAccountHolder());
    }

    @Test
    void getAdminDetail_shouldReturnPatientSubmittedFields() {
        AppointmentCancellationRequest cancellationRequest = activeRequest(
                "PENDING",
                "CANCEL_REQUESTED",
                "REFUND_PENDING"
        );
        cancellationRequest.setPatientNote("Mong phong kham ho tro");
        cancellationRequest.setBankName("Vietcombank");
        cancellationRequest.setBankAccountNumber("0123456789");
        cancellationRequest.setBankAccountHolder("NGUYEN MINH ANH");
        cancellationRequest.setCreatedAt(LocalDateTime.of(2026, 6, 9, 10, 30));

        when(cancellationRequestRepository.findDetailedById(1)).thenReturn(Optional.of(cancellationRequest));

        AdminCancellationRequestDetailResponse detail = appointmentCancellationService.getAdminDetail(1);

        assertEquals("Ban dot xuat", detail.getCancelReason());
        assertEquals("Mong phong kham ho tro", detail.getPatientNote());
        assertEquals("Vietcombank", detail.getBankName());
        assertEquals("0123456789", detail.getBankAccountNumber());
        assertEquals("NGUYEN MINH ANH", detail.getBankAccountHolder());
        assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30), detail.getCreatedAt());
        assertEquals(AppointmentCancellationRequestStatus.PENDING, detail.getStatus());
        assertEquals("Ch\u1edd x\u1eed l\u00fd", detail.getStatusLabel());
    }

    @Test
    void getAdminList_shouldMapPatientSubmittedFields() {
        AppointmentCancellationRequest cancellationRequest = activeRequest(
                "PENDING",
                "CANCEL_REQUESTED",
                "REFUND_PENDING"
        );
        cancellationRequest.setPatientNote("Mong phong kham ho tro");
        cancellationRequest.setBankName("Vietcombank");
        cancellationRequest.setBankAccountNumber("0123456789");
        cancellationRequest.setBankAccountHolder("NGUYEN MINH ANH");
        cancellationRequest.setCreatedAt(LocalDateTime.of(2026, 6, 9, 10, 30));

        when(cancellationRequestRepository.findAdminList(eq(null), eq(null), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(cancellationRequest)));

        AdminCancellationRequestListItemResponse item = appointmentCancellationService
                .getAdminList(null, null, 0, 10, null)
                .getContent()
                .get(0);

        assertEquals("Ban dot xuat", item.getCancelReason());
        assertEquals("Mong phong kham ho tro", item.getPatientNote());
        assertEquals("Vietcombank", item.getBankName());
        assertEquals("0123456789", item.getBankAccountNumber());
        assertEquals("NGUYEN MINH ANH", item.getBankAccountHolder());
        assertEquals(LocalDateTime.of(2026, 6, 9, 10, 30), item.getCreatedAt());
    }

    @Test
    void createCancellationRequest_shouldAllowUnpaidAppointmentWithoutBankInfo() {
        Patient patient = patient(7, "Nguyen Van A");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment appointment = appointment(10, patient, doctor, "PENDING_PAYMENT", "UNPAID", 270000.0);

        CreateCancellationRequestDto request = new CreateCancellationRequestDto();
        request.setCancelReason("Toi khong the den kham");

        when(appointmentRepository.findByIdAndPatientId(10, 7)).thenReturn(Optional.of(appointment));
        when(cancellationRequestRepository.existsByAppointmentIdAndStatusIn(eq(10), any())).thenReturn(false);
        when(invoiceRepository.findFirstByAppointment_IdOrderByCreatedAtDesc(10)).thenReturn(Optional.empty());
        when(cancellationRequestRepository.save(any(AppointmentCancellationRequest.class))).thenAnswer(invocation -> {
            AppointmentCancellationRequest saved = invocation.getArgument(0);
            saved.setId(2);
            return saved;
        });

        appointmentCancellationService.createCancellationRequest(10, 7, request);

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(appointmentCaptor.capture());
        assertEquals("CANCEL_REQUESTED", appointmentCaptor.getValue().getStatus());
        assertEquals("UNPAID", appointmentCaptor.getValue().getPaymentStatus());
    }

    @Test
    void createCancellationRequest_shouldRejectCompletedAppointment() {
        Patient patient = patient(7, "Nguyen Van A");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment appointment = appointment(10, patient, doctor, "COMPLETED", "PAID_ONLINE", 270000.0);

        CreateCancellationRequestDto request = validRequest();
        when(appointmentRepository.findByIdAndPatientId(10, 7)).thenReturn(Optional.of(appointment));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                appointmentCancellationService.createCancellationRequest(10, 7, request));

        assertEquals(400, ex.getStatus().value());
        verify(cancellationRequestRepository, never()).save(any());
    }

    @Test
    void createCancellationRequest_shouldRejectDuplicateActiveRequest() {
        Patient patient = patient(7, "Nguyen Van A");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment appointment = appointment(10, patient, doctor, "CONFIRMED", "PAID", 270000.0);

        when(appointmentRepository.findByIdAndPatientId(10, 7)).thenReturn(Optional.of(appointment));
        when(cancellationRequestRepository.existsByAppointmentIdAndStatusIn(eq(10), any())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                appointmentCancellationService.createCancellationRequest(10, 7, validRequest()));

        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void approve_shouldCancelAppointmentAndKeepRefundPending() {
        AppointmentCancellationRequest cancellationRequest = activeRequest("PENDING", "CANCEL_REQUESTED", "REFUND_PENDING");
        Account admin = adminAccount(99, "admin@medcare.vn");

        when(cancellationRequestRepository.findDetailedById(1)).thenReturn(Optional.of(cancellationRequest));
        when(accountRepository.findByUsername("admin@medcare.vn")).thenReturn(Optional.of(admin));
        when(cancellationRequestRepository.save(any(AppointmentCancellationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminCancellationActionRequest approveRequest = new AdminCancellationActionRequest();
        approveRequest.setAdminNote("Dong y huy");
        AdminCancellationActionResponse response = appointmentCancellationService.approve(
                1,
                "admin@medcare.vn",
                approveRequest
        );

        assertEquals(AppointmentCancellationRequestStatus.APPROVED, response.getStatus());
        assertEquals("CANCELLED", cancellationRequest.getAppointment().getStatus());
        assertEquals("REFUND_PENDING", cancellationRequest.getAppointment().getPaymentStatus());
    }

    @Test
    void reject_shouldRestorePaidStatus() {
        AppointmentCancellationRequest cancellationRequest = activeRequest("PENDING", "CANCEL_REQUESTED", "REFUND_PENDING");
        cancellationRequest.getAppointment().setPaymentStatus("PAID_ONLINE");
        Account admin = adminAccount(99, "admin@medcare.vn");

        when(cancellationRequestRepository.findDetailedById(1)).thenReturn(Optional.of(cancellationRequest));
        when(accountRepository.findByUsername("admin@medcare.vn")).thenReturn(Optional.of(admin));
        when(cancellationRequestRepository.save(any(AppointmentCancellationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminCancellationActionRequest rejectRequest = new AdminCancellationActionRequest();
        rejectRequest.setAdminNote("Khong hop le");
        AdminCancellationActionResponse response = appointmentCancellationService.reject(
                1,
                "admin@medcare.vn",
                rejectRequest
        );

        assertEquals(AppointmentCancellationRequestStatus.REJECTED, response.getStatus());
        assertEquals("CANCEL_REJECTED", cancellationRequest.getAppointment().getStatus());
        assertEquals("PAID_ONLINE", cancellationRequest.getAppointment().getPaymentStatus());
    }

    @Test
    void markRefunded_shouldOnlyWorkForApprovedRequest() {
        AppointmentCancellationRequest cancellationRequest = activeRequest("APPROVED", "CANCELLED", "REFUND_PENDING");
        Account admin = adminAccount(99, "admin@medcare.vn");

        when(cancellationRequestRepository.findDetailedById(1)).thenReturn(Optional.of(cancellationRequest));
        when(accountRepository.findByUsername("admin@medcare.vn")).thenReturn(Optional.of(admin));
        when(cancellationRequestRepository.save(any(AppointmentCancellationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminCancellationActionRequest refundRequest = new AdminCancellationActionRequest();
        refundRequest.setAdminNote("Da chuyen khoan ngoai he thong");
        AdminCancellationActionResponse response = appointmentCancellationService.markRefunded(
                1,
                "admin@medcare.vn",
                refundRequest
        );

        assertEquals(AppointmentCancellationRequestStatus.REFUNDED, response.getStatus());
        assertEquals("CANCELLED", cancellationRequest.getAppointment().getStatus());
        assertEquals("REFUNDED", cancellationRequest.getAppointment().getPaymentStatus());
    }

    @Test
    void markRefunded_shouldRejectPendingRequest() {
        AppointmentCancellationRequest cancellationRequest = activeRequest("PENDING", "CANCEL_REQUESTED", "REFUND_PENDING");
        when(cancellationRequestRepository.findDetailedById(1)).thenReturn(Optional.of(cancellationRequest));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                appointmentCancellationService.markRefunded(1, "admin@medcare.vn", new AdminCancellationActionRequest()));

        assertEquals(400, ex.getStatus().value());
    }

    private CreateCancellationRequestDto validRequest() {
        CreateCancellationRequestDto request = new CreateCancellationRequestDto();
        request.setCancelReason("Toi ban dot xuat");
        request.setBankName("Vietcombank");
        request.setBankAccountNumber("0123456789");
        request.setBankAccountHolder("NGUYEN VAN A");
        return request;
    }

    private AppointmentCancellationRequest activeRequest(String requestStatus,
                                                           String appointmentStatus,
                                                           String paymentStatus) {
        Patient patient = patient(7, "Nguyen Van A");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment appointment = appointment(10, patient, doctor, appointmentStatus, paymentStatus, 270000.0);

        AppointmentCancellationRequest request = new AppointmentCancellationRequest();
        request.setId(1);
        request.setAppointment(appointment);
        request.setPatient(patient);
        request.setStatus(requestStatus);
        request.setRefundAmount(270000.0);
        request.setCancelReason("Ban dot xuat");
        return request;
    }

    private Patient patient(Integer id, String fullName) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setFullName(fullName);
        patient.setEmail("patient@gmail.com");
        return patient;
    }

    private Doctor doctor(Integer id, String fullName) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setFullName(fullName);
        return doctor;
    }

    private Appointment appointment(Integer id,
                                    Patient patient,
                                    Doctor doctor,
                                    String status,
                                    String paymentStatus,
                                    Double fee) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setAppointmentCode("PKB-" + id);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus(status);
        appointment.setPaymentStatus(paymentStatus);
        appointment.setConsultationFee(fee);
        appointment.setAppointmentDate(LocalDateTime.of(2026, 6, 12, 8, 0));
        return appointment;
    }

    private Account adminAccount(Integer id, String username) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setRole("ADMIN");
        return account;
    }
}
