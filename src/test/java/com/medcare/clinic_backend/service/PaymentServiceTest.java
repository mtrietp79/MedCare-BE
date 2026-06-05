package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.config.VNPayConfig;
import com.medcare.clinic_backend.dto.payment.InvoicePaymentReceiptResponse;
import com.medcare.clinic_backend.dto.payment.PaymentReturnResult;
import com.medcare.clinic_backend.dto.payment.ServicePackagePaymentReceiptResponse;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.entity.MedicalService;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.ServicePackage;
import com.medcare.clinic_backend.entity.ServicePackageBooking;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.repository.ServicePackageBookingRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ServicePackageBookingRepository servicePackageBookingRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private AppointmentNotificationService appointmentNotificationService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void getServicePackagePaymentReceipt_shouldReturnReceiptForPaidBooking() {
        Patient patient = patient(1, "Pham Minh Triet", "0912345678", "triet@example.com");
        ServicePackage servicePackage = new ServicePackage();
        servicePackage.setId(5);
        servicePackage.setName("Goi VIP");
        servicePackage.setDescription("Mo ta goi VIP");

        ServicePackageBooking booking = new ServicePackageBooking();
        booking.setId(10);
        booking.setBookingCode("PKG-000010");
        booking.setPatient(patient);
        booking.setServicePackage(servicePackage);
        booking.setBookingDate(LocalDate.of(2026, 6, 4));
        booking.setBookingTime(LocalTime.of(8, 30));
        booking.setTotalAmount(600000.0);
        booking.setPaymentStatus("PAID");
        booking.setStatus("PAID");
        booking.setNote("Den som 10 phut");

        TransactionLog paidLog = new TransactionLog();
        paidLog.setVnpTransactionNo("TXN-SPB-10");
        paidLog.setBankCode("NCB");
        paidLog.setAmount(600000.0);
        paidLog.setCreatedAt(LocalDateTime.of(2026, 6, 4, 9, 0));
        paidLog.setResponseCode("00");

        when(patientRepository.findByAccount_Username("patient")).thenReturn(Optional.of(patient));
        when(servicePackageBookingRepository.findByIdAndPatientId(10, 1)).thenReturn(Optional.of(booking));
        when(transactionLogRepository.findTopByServicePackageBookingIdAndResponseCodeOrderByCreatedAtDesc(10, "00"))
                .thenReturn(paidLog);

        ServicePackagePaymentReceiptResponse response =
                paymentService.getServicePackagePaymentReceipt(10, "patient");

        assertEquals("PKG-000010", response.bookingCode());
        assertEquals("Pham Minh Triet", response.patient().fullName());
        assertEquals("Goi VIP", response.booking().packageName());
        assertEquals("VNPAY", response.payment().method());
        assertEquals("TXN-SPB-10", response.payment().transactionNo());
        assertEquals(LocalDateTime.of(2026, 6, 4, 9, 0), response.payment().paidAt());
    }

    @Test
    void getInvoicePaymentReceipt_shouldReturnReceiptForPaidInvoice() {
        Patient patient = patient(2, "Pham Minh Triet", "0912345678", "triet@example.com");
        Doctor doctor = new Doctor();
        doctor.setId(7);
        doctor.setFullName("PGS. TS. BS. Nguyen Minh Tu");

        Specialty specialty = new Specialty();
        specialty.setId(3);
        specialty.setName("Noi tong quat");

        MedicalService medicalService = new MedicalService();
        medicalService.setId(9);
        medicalService.setName("Kham tong quat");

        Appointment appointment = new Appointment();
        appointment.setId(4);
        appointment.setAppointmentCode("PKB-1780508777039");
        appointment.setAppointmentType("Kham benh");
        appointment.setDoctor(doctor);
        appointment.setSpecialty(specialty);
        appointment.setMedicalService(medicalService);

        MedicalRecord record = new MedicalRecord();
        record.setId(11);
        record.setMedicalRecordCode("HSBA-000011");
        record.setPatient(patient);
        record.setDoctor(doctor);
        record.setAppointment(appointment);

        Invoice invoice = new Invoice();
        invoice.setId(1);
        invoice.setMedicalRecord(record);
        invoice.setAppointment(appointment);
        invoice.setConsultationFee(1000000.0);
        invoice.setMedicineFee(120000.0);
        invoice.setServiceFee(100000.0);
        invoice.setTotalAmount(1220000.0);
        invoice.setStatus("PAID");
        invoice.setCreatedAt(LocalDateTime.of(2026, 6, 4, 0, 48));

        TransactionLog paidLog = new TransactionLog();
        paidLog.setInvoiceId(1);
        paidLog.setVnpTransactionNo("TXN-INV-1");
        paidLog.setBankCode("NCB");
        paidLog.setAmount(1220000.0);
        paidLog.setCreatedAt(LocalDateTime.of(2026, 6, 4, 0, 48));
        paidLog.setResponseCode("00");

        when(patientRepository.findByAccount_Username("patient")).thenReturn(Optional.of(patient));
        when(invoiceRepository.findByIdAndMedicalRecordPatientId(1, 2)).thenReturn(Optional.of(invoice));
        when(transactionLogRepository.findTopByInvoiceIdAndResponseCodeOrderByCreatedAtDesc(1, "00")).thenReturn(paidLog);

        InvoicePaymentReceiptResponse response = paymentService.getInvoicePaymentReceipt(1, "patient");

        assertEquals("INV000001", response.invoiceCode());
        assertEquals("Pham Minh Triet", response.patient().fullName());
        assertEquals("PGS. TS. BS. Nguyen Minh Tu", response.invoice().doctorName());
        assertEquals("Kham tong quat", response.invoice().serviceName());
        assertEquals("POST_EXAM", response.invoice().invoiceCategory());
        assertEquals("TXN-INV-1", response.payment().transactionNo());
        assertEquals("VNPAY", response.payment().method());
    }

    @Test
    void processServicePackageBookingVnpayReturn_shouldMarkPaidAndSendMail() {
        ReflectionTestUtils.setField(paymentService, "secretKey", "secret-key");

        ServicePackageBooking booking = new ServicePackageBooking();
        booking.setId(1);
        booking.setBookingCode("PKG-000001");
        booking.setTotalAmount(600000.0);
        booking.setPaymentStatus("PENDING");
        booking.setStatus("PENDING_PAYMENT");

        TransactionLog paidLog = new TransactionLog();
        paidLog.setVnpTransactionNo("TXN-SPB-1");
        paidLog.setBankCode("NCB");
        paidLog.setAmount(600000.0);
        paidLog.setCreatedAt(LocalDateTime.of(2026, 6, 4, 8, 0));
        paidLog.setResponseCode("00");

        when(servicePackageBookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(servicePackageBookingRepository.save(booking)).thenReturn(booking);
        when(transactionLogRepository.existsByVnpTransactionNo("TXN-SPB-1")).thenReturn(false);
        when(transactionLogRepository.findTopByServicePackageBookingIdAndResponseCodeOrderByCreatedAtDesc(1, "00"))
                .thenReturn(paidLog);

        PaymentReturnResult result = paymentService.processServicePackageBookingVnpayReturn(
                signedParams("SPB-1", 60000000L, "TXN-SPB-1"),
                1
        );

        assertTrue(result.success());
        assertEquals("PAID", booking.getPaymentStatus());
        assertEquals("PAID", booking.getStatus());
        verify(appointmentNotificationService).sendServicePackagePaymentReceipt(booking, paidLog);
    }

    @Test
    void processInvoiceVnpayReturn_shouldMarkPaidAndSendMail() {
        ReflectionTestUtils.setField(paymentService, "secretKey", "secret-key");

        Invoice invoice = new Invoice();
        invoice.setId(3);
        invoice.setTotalAmount(1220000.0);
        invoice.setStatus("UNPAID");

        TransactionLog paidLog = new TransactionLog();
        paidLog.setInvoiceId(3);
        paidLog.setVnpTransactionNo("TXN-INV-3");
        paidLog.setBankCode("NCB");
        paidLog.setAmount(1220000.0);
        paidLog.setCreatedAt(LocalDateTime.of(2026, 6, 4, 8, 30));
        paidLog.setResponseCode("00");

        when(invoiceRepository.findById(3)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(transactionLogRepository.existsByVnpTransactionNo("TXN-INV-3")).thenReturn(false);
        when(transactionLogRepository.findTopByInvoiceIdAndResponseCodeOrderByCreatedAtDesc(3, "00")).thenReturn(paidLog);

        PaymentReturnResult result = paymentService.processInvoiceVnpayReturn(
                signedParams("INV-3", 122000000L, "TXN-INV-3"),
                3
        );

        assertTrue(result.success());
        assertEquals("PAID", invoice.getStatus());
        verify(appointmentNotificationService).sendInvoicePaymentReceipt(invoice, paidLog);
    }

    private Patient patient(Integer id, String fullName, String phone, String email) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setFullName(fullName);
        patient.setPhone(phone);
        patient.setEmail(email);
        return patient;
    }

    private Map<String, String> signedParams(String txnRef, long amount, String transactionNo) {
        String secretKey = (String) ReflectionTestUtils.getField(paymentService, "secretKey");
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", transactionNo);
        params.put("vnp_BankCode", "NCB");

        List<String> keys = new ArrayList<>(params.keySet());
        keys.sort(Comparator.naturalOrder());
        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (String key : keys) {
            String value = params.get(key);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (!first) {
                hashData.append('&');
            }
            hashData.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
            first = false;
        }
        params.put("vnp_SecureHash", VNPayConfig.hmacSHA512(secretKey, hashData.toString()));
        return params;
    }
}
