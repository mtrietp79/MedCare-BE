package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    // --- 2 HÀM BỊ THIẾU GÂY RA LỖI ĐÃ ĐƯỢC THÊM VÀO ĐÂY ---
    // Lấy tất cả lịch hẹn để hiển thị danh sách
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // Lấy 1 lịch hẹn cụ thể để xem chi tiết
    public Appointment getAppointmentById(Integer id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn ID: " + id));
    }
    // ------------------------------------------------------

    // LOGIC ĐẶT LỊCH (KIỂM TRA SLOT VÀ TỰ ĐỘNG GÁN BÁC SĨ)
    public Appointment createAppointment(Appointment app) {
        LocalDateTime slotStart = app.getAppointmentDate().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime slotEnd = slotStart.plusHours(1);

        // TRƯỜNG HỢP 1: Bệnh nhân ĐÃ CHỌN bác sĩ cụ thể
        if (app.getDoctor() != null && app.getDoctor().getId() != null) {
            long count = appointmentRepository.countByDoctorInSlot(app.getDoctor().getId(), slotStart, slotEnd);
            if (count >= 5) {
                throw new RuntimeException("Khung giờ này của bác sĩ đã đầy. Vui lòng chọn giờ khác!");
            }
            return appointmentRepository.save(app);
        }

        // TRƯỜNG HỢP 2: Bệnh nhân CHƯA chọn bác sĩ -> TÌM NGƯỜI RẢNH NHẤT
        if (app.getSpecialty() != null && app.getSpecialty().getId() != null) {
            List<DoctorSchedule> availableSchedules = scheduleRepository.findByWorkDate(app.getAppointmentDate().toLocalDate());

            Doctor selectedDoctor = null;
            long minPatientCount = 6;

            for (DoctorSchedule schedule : availableSchedules) {
                Doctor doctor = schedule.getDoctor();

                if (doctor.getSpecialty() != null && doctor.getSpecialty().getId().equals(app.getSpecialty().getId())) {
                    long currentCount = appointmentRepository.countByDoctorInSlot(doctor.getId(), slotStart, slotEnd);

                    if (currentCount < 5 && currentCount < minPatientCount) {
                        minPatientCount = currentCount;
                        selectedDoctor = doctor;
                    }
                }
            }

            if (selectedDoctor != null) {
                app.setDoctor(selectedDoctor);
                return appointmentRepository.save(app);
            } else {
                throw new RuntimeException("Hiện tại tất cả bác sĩ thuộc khoa này đều đã kín lịch trong khung giờ bạn chọn.");
            }
        }

        throw new RuntimeException("Vui lòng cung cấp ít nhất Chuyên khoa hoặc Bác sĩ để đặt lịch.");
    }

    // Cập nhật lịch hẹn
    public Appointment updateAppointment(Integer id, Appointment appointmentDetails) {
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment != null) {
            appointment.setPatient(appointmentDetails.getPatient());
            appointment.setSpecialty(appointmentDetails.getSpecialty());
            appointment.setDoctor(appointmentDetails.getDoctor());
            appointment.setAppointmentDate(appointmentDetails.getAppointmentDate());
            appointment.setStatus(appointmentDetails.getStatus());
            appointment.setSymptoms(appointmentDetails.getSymptoms());
            return appointmentRepository.save(appointment);
        }
        return null;
    }

    // Xóa/Hủy lịch hẹn
    public void deleteAppointment(Integer id) {
        appointmentRepository.deleteById(id);
    }
}