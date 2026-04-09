package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicalRecordService {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    // Nhúng InvoiceService vào đây để gọi hàm tính tiền
    @Autowired
    private InvoiceService invoiceService;

    public List<MedicalRecord> getAllRecords() {
        return medicalRecordRepository.findAll();
    }

    public MedicalRecord getRecordById(Integer id) {
        return medicalRecordRepository.findById(id).orElse(null);
    }

    public List<MedicalRecord> getHistoryByPatientId(Integer patientId) {
        return medicalRecordRepository.findByPatientIdOrderByExaminationDateDesc(patientId);
    }

    // Gắn @Transactional: Đảm bảo nếu tạo Hóa đơn lỗi thì Hồ sơ bệnh án cũng bị hủy (không lưu rác)
    @Transactional
    public MedicalRecord createRecord(MedicalRecord record) {
        // 1. Lưu Hồ sơ bệnh án vào database trước để lấy ID
        MedicalRecord savedRecord = medicalRecordRepository.save(record);

        // 2. TỰ ĐỘNG SINH HÓA ĐƠN ngay lập tức dựa trên Hồ sơ vừa tạo
        invoiceService.createInvoiceFromRecord(savedRecord);

        return savedRecord;
    }

    public MedicalRecord updateRecord(Integer id, MedicalRecord details) {
        MedicalRecord record = medicalRecordRepository.findById(id).orElse(null);
        if (record != null) {
            record.setExaminationDate(details.getExaminationDate());
            record.setDiagnosis(details.getDiagnosis());
            record.setTreatmentPlan(details.getTreatmentPlan());
            record.setPrescription(details.getPrescription());
            record.setDoctorAdvice(details.getDoctorAdvice());
            return medicalRecordRepository.save(record);
        }
        return null;
    }

    public void deleteRecord(Integer id) {
        medicalRecordRepository.deleteById(id);
    }
}