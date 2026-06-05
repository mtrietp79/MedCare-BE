package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.medicalrecord.PatientMedicalRecordDetailResponse;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Mock
    private ServiceDetailRepository serviceDetailRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @InjectMocks
    private MedicalRecordService medicalRecordService;

    @Test
    void getPatientRecordDetailById_shouldExposeInvoicePaymentDateAndFollowUpCategory() {
        Patient patient = new Patient();
        patient.setId(15);
        patient.setFullName("Pham Thi E");
        patient.setPhone("0900000015");

        Specialty specialty = new Specialty();
        specialty.setId(2);
        specialty.setName("Noi tong quat");

        Doctor doctor = new Doctor();
        doctor.setId(8);
        doctor.setFullName("BS Hoang F");
        doctor.setPhone("0900000008");
        doctor.setEmail("doctor@example.com");
        doctor.setSpecialty(specialty);

        Appointment appointment = new Appointment();
        appointment.setId(77);
        appointment.setAppointmentCode("PKB-077");
        appointment.setAppointmentDate(LocalDateTime.of(2026, 6, 20, 9, 0));
        appointment.setAppointmentType("T\u00e1i kh\u00e1m");
        appointment.setStatus("COMPLETED");
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        MedicalRecord record = new MedicalRecord();
        record.setId(44);
        record.setMedicalRecordCode("BA-044");
        record.setCreatedAt(LocalDateTime.of(2026, 6, 20, 10, 0));
        record.setExaminationDate(LocalDate.of(2026, 6, 20));
        record.setDiagnosis("Tai kham theo hen");
        record.setDoctorAdvice("Theo doi them");
        record.setType("T\u00e1i kh\u00e1m");
        record.setAppointment(appointment);
        record.setPatient(patient);
        record.setDoctor(doctor);

        Invoice invoice = new Invoice();
        invoice.setId(88);
        invoice.setMedicalRecord(record);
        invoice.setAppointment(appointment);
        invoice.setConsultationFee(120000.0);
        invoice.setMedicineFee(10000.0);
        invoice.setServiceFee(5000.0);
        invoice.setTotalAmount(135000.0);
        invoice.setStatus("PAID");
        invoice.setCreatedAt(LocalDateTime.of(2026, 6, 20, 10, 30));

        TransactionLog paidLog = new TransactionLog();
        paidLog.setInvoiceId(88);
        paidLog.setResponseCode("00");
        paidLog.setCreatedAt(LocalDateTime.of(2026, 6, 20, 11, 0));

        when(medicalRecordRepository.findByIdAndPatientId(44, 15)).thenReturn(Optional.of(record));
        when(prescriptionDetailRepository.findPatientMedicineRowsByRecordId(44, 15)).thenReturn(List.of());
        when(serviceDetailRepository.findPatientServiceRowsByRecordId(44, 15)).thenReturn(List.of());
        when(invoiceRepository.findByMedicalRecordIdAndMedicalRecordPatientId(44, 15)).thenReturn(Optional.of(invoice));
        when(transactionLogRepository.findTopByInvoiceIdAndResponseCodeOrderByCreatedAtDesc(88, "00")).thenReturn(paidLog);

        PatientMedicalRecordDetailResponse response = medicalRecordService.getPatientRecordDetailById(44, 15);

        assertNotNull(response.getInvoice());
        assertEquals("FOLLOW_UP", response.getInvoice().getInvoiceCategory());
        assertEquals("H\u00f3a \u0111\u01a1n t\u00e1i kh\u00e1m", response.getInvoice().getInvoiceCategoryDisplay());
        assertEquals(LocalDateTime.of(2026, 6, 20, 11, 0), response.getInvoice().getPaymentDate());
        assertEquals("T\u00e1i kh\u00e1m", response.getAppointment().getType());
    }
}
