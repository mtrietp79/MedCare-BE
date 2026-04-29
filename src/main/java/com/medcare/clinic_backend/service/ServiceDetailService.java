package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.ServiceDetail;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceDetailService {

    @Autowired
    private ServiceDetailRepository repository;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private InvoiceService invoiceService;

    public List<ServiceDetail> getByRecordId(Integer recordId) {
        medicalRecordService.getRecordById(recordId);
        return repository.findByMedicalRecordId(recordId);
    }

    public ServiceDetail addService(ServiceDetail detail) {
        validateDetail(detail);
        medicalRecordService.getRecordById(detail.getMedicalRecord().getId());
        ServiceDetail saved = repository.save(detail);
        invoiceService.recalculateInvoiceForRecord(detail.getMedicalRecord().getId());
        return saved;
    }

    private void validateDetail(ServiceDetail detail) {
        if (detail == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu dich vu chi tiet khong hop le.");
        }
        if (detail.getMedicalRecord() == null || detail.getMedicalRecord().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Dich vu phat sinh phai co medicalRecordId.");
        }
        if (detail.getMedicalService() == null || detail.getMedicalService().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Dich vu phat sinh phai co serviceId.");
        }
        if (detail.getQuantity() == null || detail.getQuantity() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So luong dich vu phai lon hon 0.");
        }
    }
}
