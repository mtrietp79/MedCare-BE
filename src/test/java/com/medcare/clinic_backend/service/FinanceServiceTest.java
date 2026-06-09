package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.cancellation.PatientCancellationRequestSummary;
import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import com.medcare.clinic_backend.dto.invoice.PatientInvoiceDetailResponse;
import com.medcare.clinic_backend.entity.AppointmentCancellationRequestStatus;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.entity.MedicalService;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.ServicePackage;
import com.medcare.clinic_backend.entity.ServicePackageBooking;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import com.medcare.clinic_backend.repository.ServicePackageBookingRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ServicePackageBookingRepository servicePackageBookingRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Mock
    private ServiceDetailRepository serviceDetailRepository;

    @Mock
    private FinanceStatsService financeStatsService;

    @Mock
    private AppointmentCancellationService appointmentCancellationService;

    @InjectMocks
    private FinanceService financeService;

    @BeforeEach
    void setUpCancellationMocks() {
        lenient().when(appointmentCancellationService.getLatestCancellationSummariesByAppointmentIds(anyList()))
                .thenReturn(Map.of());
    }

    @Test
    void getInvoiceResponsesForPatient_shouldAggregateAllBillingCategories() {
        Patient patient = patient(7, "Nguyen Van A", "0900000001");
        Doctor doctor = doctor(3, "BS Tran Van B");

        Appointment bookingAppointment = appointment(11, "PKB-001", patient, doctor, "Kh\u00e1m b\u1ec7nh",
                LocalDateTime.of(2026, 6, 10, 8, 0), 200000.0, "UNPAID", "PENDING_PAYMENT");
        Appointment followUpAppointment = appointment(12, "PKB-002", patient, doctor, "T\u00e1i kh\u00e1m",
                LocalDateTime.of(2026, 6, 12, 9, 0), 100000.0, "UNPAID", "COMPLETED");

        Invoice postExamInvoice = invoice(31, bookingAppointment, patient, doctor,
                0.0, 50000.0, 150000.0, 200000.0, "PAID", LocalDateTime.of(2026, 6, 10, 11, 0));
        Invoice followUpInvoice = invoice(32, followUpAppointment, patient, doctor,
                100000.0, 20000.0, 30000.0, 150000.0, "PAID", LocalDateTime.of(2026, 6, 12, 11, 30));

        ServicePackageBooking servicePackageBooking = servicePackageBooking(41, patient, "PKG000041", "Goi VIP",
                900000.0, "PAID", "PAID", LocalDateTime.of(2026, 6, 8, 10, 0));

        TransactionLog appointmentPendingLog = transactionLog(null, 11, null, "PENDING", LocalDateTime.of(2026, 6, 5, 10, 0));
        TransactionLog postExamPaidLog = transactionLog(31, null, null, "00", LocalDateTime.of(2026, 6, 10, 12, 0));
        TransactionLog followUpPaidLog = transactionLog(32, null, null, "00", LocalDateTime.of(2026, 6, 12, 12, 30));
        TransactionLog packagePaidLog = transactionLog(null, null, 41, "00", LocalDateTime.of(2026, 6, 8, 11, 0));

        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(7))
                .thenReturn(List.of(followUpAppointment, bookingAppointment));
        when(invoiceRepository.findByMedicalRecordPatientIdOrderByCreatedAtDesc(7))
                .thenReturn(List.of(followUpInvoice, postExamInvoice));
        when(servicePackageBookingRepository.findByPatientIdOrderByCreatedAtDesc(7))
                .thenReturn(List.of(servicePackageBooking));
        when(transactionLogRepository.findByAppointmentIdInOrderByCreatedAtDesc(anyList()))
                .thenReturn(List.of(appointmentPendingLog));
        when(transactionLogRepository.findByInvoiceIdInOrderByCreatedAtDesc(anyList()))
                .thenReturn(List.of(followUpPaidLog, postExamPaidLog));
        when(transactionLogRepository.findByServicePackageBookingIdInOrderByCreatedAtDesc(anyList()))
                .thenReturn(List.of(packagePaidLog));

        List<InvoiceResponse> responses = financeService.getInvoiceResponsesForPatient(7, null, null, null);
        Map<String, InvoiceResponse> byCategory = responses.stream()
                .collect(Collectors.toMap(InvoiceResponse::getInvoiceCategory, Function.identity()));

        assertEquals(4, responses.size());
        assertTrue(byCategory.containsKey("APPOINTMENT_BOOKING"));
        assertTrue(byCategory.containsKey("POST_EXAM"));
        assertTrue(byCategory.containsKey("FOLLOW_UP"));
        assertTrue(byCategory.containsKey("SERVICE_PACKAGE"));

        InvoiceResponse bookingResponse = byCategory.get("APPOINTMENT_BOOKING");
        assertEquals("APPOINTMENT-11", bookingResponse.getUniqueKey());
        assertEquals("APPOINTMENT", bookingResponse.getSourceType());
        assertEquals("PKB-001", bookingResponse.getReferenceCode());
        assertEquals("H\u00f3a \u0111\u01a1n kh\u00e1m b\u1ec7nh", bookingResponse.getInvoiceType());
        assertEquals("EXAMINATION", bookingResponse.getAppointmentType());
        assertEquals("Kh\u00e1m b\u1ec7nh", bookingResponse.getAppointmentTypeLabel());
        assertEquals("Kh\u00e1m b\u1ec7nh", bookingResponse.getAppointmentTypeDisplay());
        assertFalse(Boolean.TRUE.equals(bookingResponse.getIsReExamination()));
        assertTrue(Boolean.TRUE.equals(bookingResponse.getCanPayOnline()));
        assertNull(bookingResponse.getPaymentDate());

        InvoiceResponse followUpResponse = byCategory.get("FOLLOW_UP");
        assertEquals("RE_EXAMINATION", followUpResponse.getAppointmentType());
        assertEquals("T\u00e1i kh\u00e1m", followUpResponse.getAppointmentTypeLabel());
        assertEquals("T\u00e1i kh\u00e1m", followUpResponse.getAppointmentTypeDisplay());
        assertTrue(Boolean.TRUE.equals(followUpResponse.getIsReExamination()));
        assertEquals(LocalDateTime.of(2026, 6, 12, 12, 30), followUpResponse.getPaymentDate());

        InvoiceResponse packageResponse = byCategory.get("SERVICE_PACKAGE");
        assertEquals("Goi VIP", packageResponse.getServicePackageName());
        assertEquals(LocalDateTime.of(2026, 6, 8, 11, 0), packageResponse.getPaymentDate());
        assertFalse(Boolean.TRUE.equals(packageResponse.getCanPayOnline()));
    }

    @Test
    void getInvoiceResponsesForPatient_shouldKeepAppointmentBookingInvoiceWhenPostExamInvoiceExists() {
        Patient patient = patient(7, "Nguyen Van A", "0900000001");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment bookingAppointment = appointment(11, "PKB-1780650222837", patient, doctor, "Kh\u00e1m b\u1ec7nh",
                LocalDateTime.of(2026, 6, 12, 8, 0), 270000.0, "PAID_ONLINE", "CONFIRMED");
        Invoice postExamInvoice = invoice(31, bookingAppointment, patient, doctor,
                0.0, 50000.0, 150000.0, 200000.0, "PAID", LocalDateTime.of(2026, 6, 12, 11, 0));

        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(7)).thenReturn(List.of(bookingAppointment));
        when(invoiceRepository.findByMedicalRecordPatientIdOrderByCreatedAtDesc(7)).thenReturn(List.of(postExamInvoice));
        when(servicePackageBookingRepository.findByPatientIdOrderByCreatedAtDesc(7)).thenReturn(List.of());
        when(transactionLogRepository.findByAppointmentIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());
        when(transactionLogRepository.findByInvoiceIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());
        when(transactionLogRepository.findByServicePackageBookingIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());

        List<InvoiceResponse> responses = financeService.getInvoiceResponsesForPatient(7, null, null, null, null);

        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(item ->
                "APPOINTMENT".equals(item.getSourceType()) && "PKB-1780650222837".equals(item.getReferenceCode())));
        assertTrue(responses.stream().anyMatch(item ->
                "INVOICE".equals(item.getSourceType()) && "POST_EXAM".equals(item.getInvoiceCategory())));
    }

    @Test
    void getInvoiceResponsesForPatient_shouldNotFilterOutInvoicesWhenStatusAndTypeAreAll() {
        Patient patient = patient(7, "Nguyen Van A", "0900000001");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment bookingAppointment = appointment(11, "PKB-001", patient, doctor, "Kh\u00e1m b\u1ec7nh",
                LocalDateTime.of(2026, 6, 10, 8, 0), 200000.0, "PAID", "CONFIRMED");

        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(7)).thenReturn(List.of(bookingAppointment));
        when(invoiceRepository.findByMedicalRecordPatientIdOrderByCreatedAtDesc(7)).thenReturn(List.of());
        when(servicePackageBookingRepository.findByPatientIdOrderByCreatedAtDesc(7)).thenReturn(List.of());
        when(transactionLogRepository.findByAppointmentIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());
        when(transactionLogRepository.findByInvoiceIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());
        when(transactionLogRepository.findByServicePackageBookingIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());

        List<InvoiceResponse> responses = financeService.getInvoiceResponsesForPatient(7, null, "ALL", null, "ALL");

        assertEquals(1, responses.size());
        assertEquals("APPOINTMENT", responses.get(0).getSourceType());
    }

    @Test
    void getPatientInvoiceDetail_shouldReturnAppointmentBookingDetail() {
        Patient patient = patient(7, "Nguyen Van A", "0900000001");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment bookingAppointment = appointment(11, "PKB-1780650222837", patient, doctor, "Kh\u00e1m b\u1ec7nh",
                LocalDateTime.of(2026, 6, 12, 8, 0), 270000.0, "PAID_ONLINE", "CONFIRMED");

        when(appointmentRepository.findByIdAndPatientId(11, 7)).thenReturn(java.util.Optional.of(bookingAppointment));
        when(transactionLogRepository.findByAppointmentIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());
        when(transactionLogRepository.findTopByAppointmentIdAndResponseCodeOrderByCreatedAtDesc(11, "00")).thenReturn(null);
        when(transactionLogRepository.findTopByAppointmentIdAndResponseCodeOrderByCreatedAtDesc(11, "MANUAL_PAID")).thenReturn(null);
        when(medicalRecordRepository.findByAppointmentIdAndPatientId(11, 7)).thenReturn(java.util.Optional.empty());

        PatientInvoiceDetailResponse detail = financeService.getPatientInvoiceDetail(7, 11, "APPOINTMENT", null);

        assertEquals("APPOINTMENT", detail.getSourceType());
        assertEquals("Kh\u00e1m b\u1ec7nh", detail.getExamType());
        assertEquals("EXAMINATION", detail.getAppointmentType());
        assertEquals("Kh\u00e1m b\u1ec7nh", detail.getAppointmentTypeLabel());
        assertEquals("PKB-1780650222837", detail.getAppointmentCode());
        assertNull(detail.getPackageBookingCode());
        assertNull(detail.getServicePackageName());
        assertEquals(270000.0, detail.getTotalAmount());
        assertEquals("PAID", detail.getPaymentStatus());
    }

    @Test
    void getPatientInvoiceDetail_shouldReturnMedicalRecordInvoiceWithItems() {
        Patient patient = patient(7, "Nguyen Van A", "0900000001");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment bookingAppointment = appointment(11, "PKB-1780", patient, doctor, "Kh\u00e1m b\u1ec7nh",
                LocalDateTime.of(2026, 6, 5, 8, 0), 0.0, "PAID", "COMPLETED");
        Invoice postExamInvoice = invoice(4, bookingAppointment, patient, doctor,
                0.0, 91000.0, 166000.0, 257000.0, "UNPAID", LocalDateTime.of(2026, 6, 5, 10, 0));
        postExamInvoice.getMedicalRecord().setMedicalRecordCode("BA-17800004");

        when(invoiceRepository.findByIdAndMedicalRecordPatientId(4, 7)).thenReturn(java.util.Optional.of(postExamInvoice));
        Object[] medicineRow = new Object[]{1, "Paracetamol 500mg", "vi\u00ean", 2, "Ng\u00e0y u\u1ed1ng 2 l\u1ea7n", null, 3000.0};
        Object[] serviceRow = new Object[]{2, "X\u00e9t nghi\u1ec7m m\u00e1u t\u1ed5ng qu\u00e1t", 1, null, 200000.0};
        when(prescriptionDetailRepository.findPatientMedicineRowsByRecordId(104, 7))
                .thenReturn(List.<Object[]>of(medicineRow));
        when(serviceDetailRepository.findPatientServiceRowsByRecordId(104, 7))
                .thenReturn(List.<Object[]>of(serviceRow));

        PatientInvoiceDetailResponse detail = financeService.getPatientInvoiceDetail(7, 4, "MEDICAL_RECORD", null);

        assertEquals("MEDICAL_RECORD", detail.getSourceType());
        assertEquals("H\u00f3a \u0111\u01a1n sau kh\u00e1m", detail.getInvoiceType());
        assertEquals("BA-17800004", detail.getMedicalRecordCode());
        assertEquals("Kh\u00e1m b\u1ec7nh", detail.getExamType());
        assertEquals(91000.0, detail.getMedicineTotal());
        assertEquals(166000.0, detail.getServiceTotal());
        assertEquals(1, detail.getPrescriptionItems().size());
        assertEquals(1, detail.getMedicalServiceItems().size());
        assertNull(detail.getPackageBookingCode());
    }

    @Test
    void getPatientInvoiceDetail_shouldReturnServicePackageDetailOnly() {
        Patient patient = patient(7, "Nguyen Van A", "0900000001");
        ServicePackageBooking booking = servicePackageBooking(2, patient, "PKG000002", "T\u1ea7m so\u00e1t ung th\u01b0 v\u00fa",
                850000.0, "PAID", "PAID", LocalDateTime.of(2026, 6, 8, 10, 0));

        when(servicePackageBookingRepository.findByIdAndPatientId(2, 7)).thenReturn(java.util.Optional.of(booking));
        when(transactionLogRepository.findByServicePackageBookingIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());

        PatientInvoiceDetailResponse detail = financeService.getPatientInvoiceDetail(7, 2, "SERVICE_PACKAGE", null);

        assertEquals("SERVICE_PACKAGE", detail.getSourceType());
        assertEquals("G\u00f3i d\u1ecbch v\u1ee5", detail.getExamType());
        assertEquals("SERVICE_PACKAGE", detail.getAppointmentType());
        assertEquals("PKG000002", detail.getPackageBookingCode());
        assertEquals("T\u1ea7m so\u00e1t ung th\u01b0 v\u00fa", detail.getServicePackageName());
        assertNull(detail.getMedicalRecordCode());
        assertNull(detail.getPrescriptionItems());
    }

    @Test
    void getPatientInvoiceDetail_shouldResolveServiceExamTypeFromMedicalService() {
        Patient patient = patient(7, "Nguyen Van A", "0900000001");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment appointment = appointment(15, "PKB-015", patient, doctor, "Kh\u00e1m b\u1ec7nh",
                LocalDateTime.of(2026, 6, 12, 9, 0), 300000.0, "PAID", "CONFIRMED");
        MedicalService medicalService = new MedicalService();
        medicalService.setId(9);
        medicalService.setName("X\u00e9t nghi\u1ec7m t\u1ed5ng qu\u00e1t");
        appointment.setMedicalService(medicalService);

        when(appointmentRepository.findByIdAndPatientId(15, 7)).thenReturn(java.util.Optional.of(appointment));
        when(transactionLogRepository.findByAppointmentIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());
        when(transactionLogRepository.findTopByAppointmentIdAndResponseCodeOrderByCreatedAtDesc(15, "00")).thenReturn(null);
        when(transactionLogRepository.findTopByAppointmentIdAndResponseCodeOrderByCreatedAtDesc(15, "MANUAL_PAID")).thenReturn(null);
        when(medicalRecordRepository.findByAppointmentIdAndPatientId(15, 7)).thenReturn(java.util.Optional.empty());

        PatientInvoiceDetailResponse detail = financeService.getPatientInvoiceDetail(7, 15, "APPOINTMENT", null);

        assertEquals("Kh\u00e1m b\u1ec7nh", detail.getExamType());
        assertEquals("EXAMINATION", detail.getAppointmentType());
    }

    @Test
    void getInvoiceResponsesForAdmin_shouldExposeCancellationRequestMetadata() {
        Patient patient = patient(7, "Nguyen Van A", "0900000001");
        Doctor doctor = doctor(3, "BS Tran Van B");
        Appointment bookingAppointment = appointment(8, "PKB-1780957879494", patient, doctor, "Kh\u00e1m b\u1ec7nh",
                LocalDateTime.of(2026, 6, 10, 9, 0), 1000000.0, "REFUND_PENDING", "CANCEL_REQUESTED");

        when(appointmentRepository.findAll()).thenReturn(List.of(bookingAppointment));
        when(invoiceRepository.findAll()).thenReturn(List.of());
        when(servicePackageBookingRepository.findAll()).thenReturn(List.of());
        when(transactionLogRepository.findByAppointmentIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());
        when(transactionLogRepository.findByInvoiceIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());
        when(appointmentCancellationService.getLatestCancellationSummariesByAppointmentIds(List.of(8)))
                .thenReturn(Map.of(8, PatientCancellationRequestSummary.builder()
                        .id(1)
                        .status(AppointmentCancellationRequestStatus.PENDING)
                        .statusLabel("Ch\u1edd x\u1eed l\u00fd")
                        .refundAmount(1000000.0)
                        .createdAt(LocalDateTime.of(2026, 6, 9, 10, 30))
                        .build()));

        InvoiceResponse response = financeService.getInvoiceResponsesForAdmin(null, null).stream()
                .filter(item -> Integer.valueOf(8).equals(item.getAppointmentId()))
                .findFirst()
                .orElseThrow();

        assertTrue(Boolean.TRUE.equals(response.getHasCancellationRequest()));
        assertEquals(1, response.getCancellationRequestId());
        assertEquals(AppointmentCancellationRequestStatus.PENDING, response.getCancellationStatus());
        assertEquals("Ch\u1edd x\u1eed l\u00fd", response.getCancellationStatusLabel());
        assertEquals("CANCEL_REQUESTED", response.getAppointmentStatus());
    }

    @Test
    void getInvoiceResponsesForPatient_shouldFilterByCategoryKeywordAndStatus() {
        Patient patient = patient(9, "Le Thi C", "0900000002");
        Doctor doctor = doctor(4, "BS D");
        Appointment bookingAppointment = appointment(21, "PKB-021", patient, doctor, "Kh\u00e1m b\u1ec7nh",
                LocalDateTime.of(2026, 6, 15, 8, 0), 250000.0, "PAID_ONLINE", "CONFIRMED");
        Invoice postExamInvoice = invoice(51, bookingAppointment, patient, doctor,
                0.0, 10000.0, 20000.0, 30000.0, "UNPAID", LocalDateTime.of(2026, 6, 15, 10, 0));

        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(9)).thenReturn(List.of(bookingAppointment));
        when(invoiceRepository.findByMedicalRecordPatientIdOrderByCreatedAtDesc(9)).thenReturn(List.of(postExamInvoice));
        when(servicePackageBookingRepository.findByPatientIdOrderByCreatedAtDesc(9)).thenReturn(List.of());
        when(transactionLogRepository.findByAppointmentIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());
        when(transactionLogRepository.findByInvoiceIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());
        when(transactionLogRepository.findByServicePackageBookingIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());

        List<InvoiceResponse> filtered = financeService.getInvoiceResponsesForPatient(9, "sau kham", "UNPAID", "post_exam");

        assertEquals(1, filtered.size());
        assertEquals("POST_EXAM", filtered.get(0).getInvoiceCategory());
        assertEquals("UNPAID", filtered.get(0).getStatus());
    }

    private Patient patient(Integer id, String fullName, String phone) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setFullName(fullName);
        patient.setPhone(phone);
        return patient;
    }

    private Doctor doctor(Integer id, String fullName) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setFullName(fullName);
        return doctor;
    }

    private Appointment appointment(
            Integer id,
            String code,
            Patient patient,
            Doctor doctor,
            String type,
            LocalDateTime appointmentDate,
            Double consultationFee,
            String paymentStatus,
            String status
    ) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setAppointmentCode(code);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentType(type);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setConsultationFee(consultationFee);
        appointment.setPaymentStatus(paymentStatus);
        appointment.setStatus(status);
        return appointment;
    }

    private Invoice invoice(
            Integer invoiceId,
            Appointment appointment,
            Patient patient,
            Doctor doctor,
            Double consultationFee,
            Double medicineFee,
            Double serviceFee,
            Double totalAmount,
            String status,
            LocalDateTime createdAt
    ) {
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setId(invoiceId + 100);
        medicalRecord.setAppointment(appointment);
        medicalRecord.setPatient(patient);
        medicalRecord.setDoctor(doctor);
        medicalRecord.setExaminationDate(createdAt.toLocalDate());

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setMedicalRecord(medicalRecord);
        invoice.setAppointment(appointment);
        invoice.setConsultationFee(consultationFee);
        invoice.setMedicineFee(medicineFee);
        invoice.setServiceFee(serviceFee);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(status);
        invoice.setCreatedAt(createdAt);
        return invoice;
    }

    private ServicePackageBooking servicePackageBooking(
            Integer id,
            Patient patient,
            String bookingCode,
            String packageName,
            Double totalAmount,
            String paymentStatus,
            String status,
            LocalDateTime createdAt
    ) {
        ServicePackage servicePackage = new ServicePackage();
        servicePackage.setId(id + 1000);
        servicePackage.setName(packageName);

        ServicePackageBooking booking = new ServicePackageBooking();
        booking.setId(id);
        booking.setPatient(patient);
        booking.setServicePackage(servicePackage);
        booking.setBookingCode(bookingCode);
        booking.setBookingDate(createdAt.toLocalDate());
        booking.setBookingTime(LocalTime.of(8, 0));
        booking.setTotalAmount(totalAmount);
        booking.setPaymentStatus(paymentStatus);
        booking.setStatus(status);
        booking.setCreatedAt(createdAt);
        booking.setUpdatedAt(createdAt);
        return booking;
    }

    private TransactionLog transactionLog(
            Integer invoiceId,
            Integer appointmentId,
            Integer servicePackageBookingId,
            String responseCode,
            LocalDateTime createdAt
    ) {
        TransactionLog log = new TransactionLog();
        log.setInvoiceId(invoiceId);
        log.setAppointmentId(appointmentId);
        log.setServicePackageBookingId(servicePackageBookingId);
        log.setResponseCode(responseCode);
        log.setCreatedAt(createdAt);
        return log;
    }
}
