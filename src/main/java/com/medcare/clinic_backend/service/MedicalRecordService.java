package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MedicalRecordService {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private InvoiceService invoiceService;

    public List<MedicalRecord> getAllRecords() {
        return medicalRecordRepository.findAll();
    }

    public List<MedicalRecord> getRecordsForDoctor(Integer doctorId) {
        return medicalRecordRepository.findByDoctorIdOrderByExaminationDateDesc(doctorId);
    }

    public MedicalRecord getRecordById(Integer id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay ho so benh an ID: " + id));
    }

    public MedicalRecord getRecordByIdForDoctor(Integer id, Integer doctorId) {
        return medicalRecordRepository.findByIdAndDoctorId(id, doctorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay ho so benh an ID: " + id));
    }

    public List<MedicalRecord> getHistoryByPatientId(Integer patientId) {
        return medicalRecordRepository.findByPatientIdOrderByExaminationDateDesc(patientId);
    }

    public List<MedicalRecord> getHistoryByPatientIdForDoctor(Integer patientId, Integer doctorId) {
        return medicalRecordRepository.findByPatientIdAndDoctorIdOrderByExaminationDateDesc(patientId, doctorId);
    }

    @Transactional
    public MedicalRecord createRecord(MedicalRecord record) {
        if (record == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu ho so benh an khong hop le.");
        }
        if (record.getAppointment() == null || record.getAppointment().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ho so benh an phai co appointmentId.");
        }
        if (record.getDiagnosis() == null || record.getDiagnosis().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chan doan khong duoc de trong.");
        }

        Appointment appointment = appointmentRepository.findById(record.getAppointment().getId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Khong tim thay lich hen ID: " + record.getAppointment().getId()
                ));

        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen chua co bac si.");
        }
        if (appointment.getPatient() == null || appointment.getPatient().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen chua co benh nhan.");
        }
        String appointmentStatus = appointment.getStatus() == null ? "" : appointment.getStatus().trim().toUpperCase();
        if ("CANCELLED".equals(appointmentStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong the tao ho so benh an cho lich hen da huy.");
        }
        if ("PENDING".equals(appointmentStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chi tao ho so benh an cho lich hen da duoc xac nhan.");
        }
        if (medicalRecordRepository.existsByAppointmentId(appointment.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Lich hen nay da co ho so benh an.");
        }

        Integer requestedDoctorId = record.getDoctor() == null ? null : record.getDoctor().getId();
        if (requestedDoctorId != null && !requestedDoctorId.equals(appointment.getDoctor().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si trong ho so khong khop voi bac si cua lich hen.");
        }

        Integer requestedPatientId = record.getPatient() == null ? null : record.getPatient().getId();
        if (requestedPatientId != null && !requestedPatientId.equals(appointment.getPatient().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Benh nhan trong ho so khong khop voi benh nhan cua lich hen.");
        }

        record.setAppointment(appointment);
        record.setDoctor(appointment.getDoctor());
        record.setPatient(appointment.getPatient());
        if (record.getExaminationDate() == null) {
            record.setExaminationDate(LocalDate.now());
        }
        if (!"COMPLETED".equals(appointmentStatus)) {
            appointment.setStatus("COMPLETED");
            appointmentRepository.save(appointment);
        }

        MedicalRecord savedRecord = medicalRecordRepository.save(record);
        invoiceService.createInvoiceFromRecord(savedRecord);
        return savedRecord;
    }

    public MedicalRecord updateRecord(Integer id, MedicalRecord details) {
        MedicalRecord record = getRecordById(id);
        applyMutableFields(record, details);
        return medicalRecordRepository.save(record);
    }

    public MedicalRecord updateRecordForDoctor(Integer id, Integer doctorId, MedicalRecord details) {
        MedicalRecord record = getRecordByIdForDoctor(id, doctorId);
        applyMutableFields(record, details);
        return medicalRecordRepository.save(record);
    }

    public void deleteRecord(Integer id) {
        if (!medicalRecordRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay ho so benh an ID: " + id);
        }
        medicalRecordRepository.deleteById(id);
    }

    private void applyMutableFields(MedicalRecord record, MedicalRecord details) {
        if (details == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu cap nhat khong hop le.");
        }
        if (details.getDiagnosis() != null && details.getDiagnosis().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chan doan khong duoc de trong.");
        }

        if (details.getExaminationDate() != null) {
            record.setExaminationDate(details.getExaminationDate());
        }
        if (details.getDiagnosis() != null) {
            record.setDiagnosis(details.getDiagnosis());
        }
        if (details.getTreatmentPlan() != null) {
            record.setTreatmentPlan(details.getTreatmentPlan());
        }
        if (details.getPrescription() != null) {
            record.setPrescription(details.getPrescription());
        }
        if (details.getDoctorAdvice() != null) {
            record.setDoctorAdvice(details.getDoctorAdvice());
        }
    }
}
