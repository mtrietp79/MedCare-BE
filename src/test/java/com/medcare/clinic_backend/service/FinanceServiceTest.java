package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.ServicePackage;
import com.medcare.clinic_backend.entity.ServicePackageBooking;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.ServicePackageBookingRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
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

    @InjectMocks
    private FinanceService financeService;

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
        when(invoiceRepository.existsByAppointmentId(11)).thenReturn(false);
        // when(invoiceRepository.existsByAppointmentId(12)).thenReturn(false); // Xóa stub dư thừa này vì isFollowUpType(appointmentType) sẽ return false sớm
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
        assertEquals("Kh\u00e1m b\u1ec7nh", bookingResponse.getAppointmentTypeDisplay());
        assertTrue(Boolean.TRUE.equals(bookingResponse.getCanPayOnline()));
        assertNull(bookingResponse.getPaymentDate());

        InvoiceResponse followUpResponse = byCategory.get("FOLLOW_UP");
        assertEquals("T\u00e1i kh\u00e1m", followUpResponse.getAppointmentTypeDisplay());
        assertEquals(LocalDateTime.of(2026, 6, 12, 12, 30), followUpResponse.getPaymentDate());

        InvoiceResponse packageResponse = byCategory.get("SERVICE_PACKAGE");
        assertEquals("Goi VIP", packageResponse.getServicePackageName());
        assertEquals(LocalDateTime.of(2026, 6, 8, 11, 0), packageResponse.getPaymentDate());
        assertFalse(Boolean.TRUE.equals(packageResponse.getCanPayOnline()));
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
