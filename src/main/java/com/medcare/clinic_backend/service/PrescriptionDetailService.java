package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.PrescriptionDetail;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrescriptionDetailService {

    @Autowired
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Autowired
    private MedicalRecordService medicalRecordService;

    public List<PrescriptionDetail> getPrescriptionByRecordId(Integer recordId) {
        medicalRecordService.getRecordById(recordId);
        return prescriptionDetailRepository.findByMedicalRecordId(recordId);
    }

    public List<PrescriptionDetail> getPrescriptionByRecordIdForDoctor(Integer recordId, Integer doctorId) {
        medicalRecordService.getRecordByIdForDoctor(recordId, doctorId);
        return prescriptionDetailRepository.findByMedicalRecordIdAndMedicalRecordDoctorId(recordId, doctorId);
    }

    public PrescriptionDetail addMedicineToPrescription(PrescriptionDetail detail) {
        validateDetail(detail);
        medicalRecordService.getRecordById(detail.getMedicalRecord().getId());
        return prescriptionDetailRepository.save(detail);
    }

    public PrescriptionDetail addMedicineToPrescriptionForDoctor(PrescriptionDetail detail, Integer doctorId) {
        validateDetail(detail);
        medicalRecordService.getRecordByIdForDoctor(detail.getMedicalRecord().getId(), doctorId);
        return prescriptionDetailRepository.save(detail);
    }

    private void validateDetail(PrescriptionDetail detail) {
        if (detail == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu don thuoc khong hop le.");
        }
        if (detail.getMedicalRecord() == null || detail.getMedicalRecord().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Don thuoc phai co medicalRecordId.");
        }
        if (detail.getMedicine() == null || detail.getMedicine().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Don thuoc phai co medicineId.");
        }
        if (detail.getQuantity() == null || detail.getQuantity() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So luong thuoc phai lon hon 0.");
        }
    }
}
