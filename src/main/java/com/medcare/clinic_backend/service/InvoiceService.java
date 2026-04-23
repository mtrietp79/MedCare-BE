package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PrescriptionDetailRepository prescriptionRepository;

    @Autowired
    private ServiceDetailRepository serviceDetailRepository;

    public Invoice createInvoiceFromRecord(MedicalRecord record) {
        double medicineFee = prescriptionRepository.findByMedicalRecordId(record.getId())
                .stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getMedicine().getPrice())
                .sum();

        double serviceFee = serviceDetailRepository.findByMedicalRecordId(record.getId())
                .stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getMedicalService().getPrice())
                .sum();

        Invoice invoice = new Invoice();
        invoice.setMedicalRecord(record);
        invoice.setMedicineFee(medicineFee);
        invoice.setServiceFee(serviceFee);
        invoice.setTotalAmount(medicineFee + serviceFee);
        invoice.setStatus("UNPAID");
        return invoiceRepository.save(invoice);
    }

    public Invoice getInvoiceByRecordId(Integer recordId) {
        return invoiceRepository.findByMedicalRecordId(recordId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay hoa don cho medicalRecordId: " + recordId
                ));
    }

    public Invoice getInvoiceByRecordIdForDoctor(Integer recordId, Integer doctorId) {
        return invoiceRepository.findByMedicalRecordIdAndMedicalRecordDoctorId(recordId, doctorId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay hoa don cho medicalRecordId: " + recordId
                ));
    }

    public List<Invoice> getInvoicesForDoctor(Integer doctorId) {
        return invoiceRepository.findByMedicalRecordDoctorIdOrderByCreatedAtDesc(doctorId);
    }
}
