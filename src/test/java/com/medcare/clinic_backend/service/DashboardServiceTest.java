package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.ServicePackageBookingRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ServicePackageBookingRepository servicePackageBookingRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void exportDashboardReport_shouldGenerateFormattedWorkbookWithVietnameseContent() throws Exception {
        Appointment januaryAppointment = buildAppointment(
                1,
                "Nguyễn Văn An",
                "TS. Trần Mai",
                "Nội tổng quát",
                LocalDateTime.of(2026, 1, 15, 9, 30),
                "COMPLETED"
        );
        Appointment februaryAppointment = buildAppointment(
                2,
                "Nguyễn Văn An",
                "TS. Trần Mai",
                "Nội tổng quát",
                LocalDateTime.of(2026, 2, 20, 14, 15),
                "CONFIRMED"
        );

        Invoice januaryInvoice = buildInvoice(LocalDateTime.of(2026, 1, 15, 11, 0), 1_500_000.0, "PAID");
        Invoice februaryInvoice = buildInvoice(LocalDateTime.of(2026, 2, 20, 16, 0), 750_000.0, "PAID");

        when(doctorRepository.countByIsActiveTrue()).thenReturn(40L);
        when(appointmentRepository.findAll()).thenReturn(List.of(januaryAppointment, februaryAppointment));
        when(appointmentRepository.findTop10ByOrderByAppointmentDateDesc())
                .thenReturn(List.of(februaryAppointment, januaryAppointment));
        when(invoiceRepository.findAll()).thenReturn(List.of(januaryInvoice, februaryInvoice));
        when(servicePackageBookingRepository.findAll()).thenReturn(List.of());

        byte[] workbookBytes = dashboardService.exportDashboardReport(2026);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            assertEquals("Tổng quan", workbook.getSheetName(0));
            assertEquals("Báo cáo theo tháng", workbook.getSheetName(1));
            assertEquals("Bệnh nhân theo tháng", workbook.getSheetName(2));
            assertEquals("Doanh thu theo tháng", workbook.getSheetName(3));
            assertEquals("Lịch hẹn gần đây", workbook.getSheetName(4));

            Sheet summarySheet = workbook.getSheet("Tổng quan");
            assertEquals("Báo cáo Dashboard Admin", summarySheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Năm báo cáo", summarySheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("Chỉ số vận hành", summarySheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals("Tổng lịch hẹn", summarySheet.getRow(7).getCell(0).getStringCellValue());

            Sheet monthlySheet = workbook.getSheet("Báo cáo theo tháng");
            assertEquals("Tháng 1", monthlySheet.getRow(6).getCell(0).getStringCellValue());
            assertEquals(1, (long) monthlySheet.getRow(6).getCell(1).getNumericCellValue());
            assertEquals(1, (long) monthlySheet.getRow(6).getCell(2).getNumericCellValue());
            assertEquals(1_500_000.0, monthlySheet.getRow(6).getCell(3).getNumericCellValue());
            assertEquals("Tháng 2", monthlySheet.getRow(7).getCell(0).getStringCellValue());
            assertEquals(750_000.0, monthlySheet.getRow(7).getCell(3).getNumericCellValue());

            Sheet recentAppointmentsSheet = workbook.getSheet("Lịch hẹn gần đây");
            assertEquals("Bệnh nhân", recentAppointmentsSheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals("Nguyễn Văn An", recentAppointmentsSheet.getRow(6).getCell(0).getStringCellValue());
            assertEquals("TS. Trần Mai", recentAppointmentsSheet.getRow(6).getCell(1).getStringCellValue());
            assertEquals("Nội tổng quát", recentAppointmentsSheet.getRow(6).getCell(2).getStringCellValue());
            assertNotNull(recentAppointmentsSheet.getRow(6).getCell(7).getStringCellValue());
        }
    }

    private Appointment buildAppointment(
            Integer patientId,
            String patientName,
            String doctorName,
            String specialtyName,
            LocalDateTime appointmentDate,
            String status
    ) {
        Patient patient = new Patient();
        patient.setId(patientId);
        patient.setFullName(patientName);

        Specialty specialty = new Specialty();
        specialty.setName(specialtyName);

        Doctor doctor = new Doctor();
        doctor.setFullName(doctorName);
        doctor.setIsActive(true);
        doctor.setSpecialty(specialty);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setSpecialty(specialty);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setStatus(status);
        return appointment;
    }

    private Invoice buildInvoice(LocalDateTime createdAt, double totalAmount, String status) {
        Invoice invoice = new Invoice();
        invoice.setCreatedAt(createdAt);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(status);
        return invoice;
    }
}
