package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.PrescriptionDetail;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrescriptionDetailService {

    @Autowired
    private PrescriptionDetailRepository prescriptionDetailRepository;

    // Lấy ra đơn thuốc của 1 ca khám cụ thể
    public List<PrescriptionDetail> getPrescriptionByRecordId(Integer recordId) {
        return prescriptionDetailRepository.findByMedicalRecordId(recordId);
    }

    // Thêm 1 loại thuốc vào đơn
    public PrescriptionDetail addMedicineToPrescription(PrescriptionDetail detail) {
        return prescriptionDetailRepository.save(detail);
    }
}