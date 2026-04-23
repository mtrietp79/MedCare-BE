package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Patient;
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(patient.getEmail());
        message.setSubject("Phieu kham benh - " + appointment.getAppointmentCode());
        message.setText(buildTicketContent(appointment));
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Khong gui duoc email phieu kham cho appointmentCode={}", appointment.getAppointmentCode(), ex);
        }
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
}
