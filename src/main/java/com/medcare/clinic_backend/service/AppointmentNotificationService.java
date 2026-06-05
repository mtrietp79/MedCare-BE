package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.ServicePackageBooking;
import com.medcare.clinic_backend.entity.TransactionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class AppointmentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private JavaMailSender mailSender;

    public void sendAppointmentTicket(Appointment appointment) {
        if (appointment == null || appointment.getPatient() == null) {
            return;
        }

        Patient patient = appointment.getPatient();
        if (patient.getEmail() == null || patient.getEmail().isBlank()) {
            return;
        }

        sendMailSafely(
                patient.getEmail(),
                "Phieu kham benh - " + appointment.getAppointmentCode(),
                buildTicketContent(appointment),
                "appointmentCode",
                appointment.getAppointmentCode()
        );
    }

    public void sendServicePackagePaymentReceipt(ServicePackageBooking booking, TransactionLog paidLog) {
        if (booking == null || booking.getPatient() == null) {
            return;
        }

        Patient patient = booking.getPatient();
        if (patient.getEmail() == null || patient.getEmail().isBlank()) {
            return;
        }

        String bookingCode = booking.getBookingCode() == null ? String.valueOf(booking.getId()) : booking.getBookingCode();
        sendMailSafely(
                patient.getEmail(),
                "Bien lai thanh toan goi dich vu - " + bookingCode,
                buildServicePackageReceiptContent(booking, paidLog),
                "bookingCode",
                bookingCode
        );
    }

    public void sendInvoicePaymentReceipt(Invoice invoice, TransactionLog paidLog) {
        if (invoice == null || invoice.getMedicalRecord() == null || invoice.getMedicalRecord().getPatient() == null) {
            return;
        }

        Patient patient = invoice.getMedicalRecord().getPatient();
        if (patient.getEmail() == null || patient.getEmail().isBlank()) {
            return;
        }

        String invoiceCode = buildInvoiceCode(invoice);
        sendMailSafely(
                patient.getEmail(),
                "Bien lai thanh toan hoa don - " + invoiceCode,
                buildInvoiceReceiptContent(invoice, paidLog),
                "invoiceCode",
                invoiceCode
        );
    }

    private String buildTicketContent(Appointment appointment) {
        String patientName = appointment.getPatient() == null ? "" : appointment.getPatient().getFullName();
        String doctorName = appointment.getDoctor() == null ? "Dang cap nhat" : appointment.getDoctor().getFullName();
        String specialtyName = appointment.getSpecialty() == null ? "Dang cap nhat" : appointment.getSpecialty().getName();

        return "Phieu kham benh cua ban da duoc tao thanh cong.\n"
                + "Ma phieu: " + appointment.getAppointmentCode() + "\n"
                + "Benh nhan: " + patientName + "\n"
                + "Bac si: " + doctorName + "\n"
                + "Chuyen khoa: " + specialtyName + "\n"
                + "Thoi gian kham: " + appointment.getAppointmentDate().format(DATE_TIME_FORMATTER) + "\n"
                + "Trang thai: " + appointment.getStatus() + "\n";
    }

    private String buildServicePackageReceiptContent(ServicePackageBooking booking, TransactionLog paidLog) {
        String patientName = booking.getPatient() == null ? "Dang cap nhat" : safeText(booking.getPatient().getFullName(), "Dang cap nhat");
        String packageName = booking.getServicePackage() == null ? "Dang cap nhat" : safeText(booking.getServicePackage().getName(), "Dang cap nhat");
        String bookingCode = booking.getBookingCode() == null ? String.valueOf(booking.getId()) : booking.getBookingCode();
        String bookingDate = booking.getBookingDate() == null ? "Dang cap nhat" : booking.getBookingDate().format(DATE_FORMATTER);
        String bookingTime = booking.getBookingTime() == null ? "Dang cap nhat" : booking.getBookingTime().format(TIME_FORMATTER);

        return "Dat goi dich vu cua ban da thanh toan thanh cong.\n"
                + "Ma dat goi: " + bookingCode + "\n"
                + "Benh nhan: " + patientName + "\n"
                + "Goi dich vu: " + packageName + "\n"
                + "Ngay den: " + bookingDate + "\n"
                + "Gio den: " + bookingTime + "\n"
                + "Tong tien: " + safeAmount(booking.getTotalAmount()) + " VND\n"
                + "Trang thai thanh toan: " + safeText(booking.getPaymentStatus(), "PAID") + "\n"
                + "Ma giao dich: " + (paidLog == null ? "Dang cap nhat" : safeText(paidLog.getVnpTransactionNo(), "Dang cap nhat")) + "\n"
                + "Ngan hang: " + (paidLog == null ? "Dang cap nhat" : safeText(paidLog.getBankCode(), "Dang cap nhat")) + "\n"
                + "Thoi gian thanh toan: " + formatDateTime(paidLog == null ? null : paidLog.getCreatedAt()) + "\n";
    }

    private String buildInvoiceReceiptContent(Invoice invoice, TransactionLog paidLog) {
        String invoiceCode = buildInvoiceCode(invoice);
        String patientName = invoice.getMedicalRecord() == null || invoice.getMedicalRecord().getPatient() == null
                ? "Dang cap nhat"
                : safeText(invoice.getMedicalRecord().getPatient().getFullName(), "Dang cap nhat");
        String doctorName = invoice.getMedicalRecord() == null || invoice.getMedicalRecord().getDoctor() == null
                ? "Dang cap nhat"
                : safeText(invoice.getMedicalRecord().getDoctor().getFullName(), "Dang cap nhat");
        Appointment appointment = invoice.getAppointment() != null
                ? invoice.getAppointment()
                : (invoice.getMedicalRecord() == null ? null : invoice.getMedicalRecord().getAppointment());
        String appointmentCode = appointment == null ? "Dang cap nhat" : safeText(appointment.getAppointmentCode(), "Dang cap nhat");
        String serviceName = appointment == null ? "Dang cap nhat" : safeText(appointment.getServiceName(), "Dang cap nhat");
        String specialtyName = appointment == null ? "Dang cap nhat" : safeText(appointment.getSpecialtyName(), "Dang cap nhat");
        String recordCode = invoice.getMedicalRecord() == null
                ? "Dang cap nhat"
                : safeText(invoice.getMedicalRecord().getMedicalRecordCode(), "HSBA-" + invoice.getMedicalRecord().getId());

        return "Hoa don cua ban da thanh toan thanh cong.\n"
                + "Ma hoa don: " + invoiceCode + "\n"
                + "Benh nhan: " + patientName + "\n"
                + "Bac si: " + doctorName + "\n"
                + "Ho so benh an: " + recordCode + "\n"
                + "Lich lien quan: " + appointmentCode + "\n"
                + "Chuyen khoa: " + specialtyName + "\n"
                + "Dich vu: " + serviceName + "\n"
                + "Phi kham: " + safeAmount(invoice.getConsultationFee()) + " VND\n"
                + "Tien thuoc: " + safeAmount(invoice.getMedicineFee()) + " VND\n"
                + "Tien dich vu: " + safeAmount(invoice.getServiceFee()) + " VND\n"
                + "Tong tien: " + safeAmount(invoice.getTotalAmount()) + " VND\n"
                + "Ma giao dich: " + (paidLog == null ? "Dang cap nhat" : safeText(paidLog.getVnpTransactionNo(), "Dang cap nhat")) + "\n"
                + "Ngan hang: " + (paidLog == null ? "Dang cap nhat" : safeText(paidLog.getBankCode(), "Dang cap nhat")) + "\n"
                + "Thoi gian thanh toan: " + formatDateTime(paidLog == null ? null : paidLog.getCreatedAt()) + "\n";
    }

    private void sendMailSafely(String email, String subject, String body, String keyName, String keyValue) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Khong gui duoc email thong bao cho {}={}", keyName, keyValue, ex);
        }
    }

    private String buildInvoiceCode(Invoice invoice) {
        if (invoice == null || invoice.getId() == null) {
            return "INV";
        }
        return "INV" + String.format("%06d", invoice.getId());
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "Dang cap nhat" : value.format(DATE_TIME_FORMATTER);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeAmount(Double value) {
        long amount = value == null ? 0L : Math.round(value);
        return String.format("%,d", amount).replace(',', '.');
    }
}
