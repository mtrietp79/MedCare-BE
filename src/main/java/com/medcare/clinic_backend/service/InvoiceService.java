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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PrescriptionDetailRepository prescriptionRepository;

    @Autowired
    private ServiceDetailRepository serviceDetailRepository;

    @Transactional
    public Invoice createInvoiceFromRecord(MedicalRecord record) {
        if (record == null || record.getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Medical record khong hop le de tao hoa don.");
        }

        Invoice invoice = invoiceRepository.findByMedicalRecordId(record.getId())
                .orElseGet(Invoice::new);

        invoice.setMedicalRecord(record);
        applyInvoiceTotals(invoice, record.getId());
        invoice.setStatus(invoice.getStatus() == null || invoice.getStatus().isBlank() ? "UNPAID" : invoice.getStatus());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void recalculateInvoiceForRecord(Integer recordId) {
        Invoice invoice = invoiceRepository.findByMedicalRecordId(recordId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay hoa don cho medicalRecordId: " + recordId
                ));
        applyInvoiceTotals(invoice, recordId);
        invoiceRepository.save(invoice);
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

    private void applyInvoiceTotals(Invoice invoice, Integer recordId) {
        double medicineFee = prescriptionRepository.findByMedicalRecordId(recordId)
                .stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getMedicine().getPrice())
                .sum();

        double serviceFee = serviceDetailRepository.findByMedicalRecordId(recordId)
                .stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getMedicalService().getPrice())
                .sum();

        invoice.setMedicineFee(medicineFee);
        invoice.setServiceFee(serviceFee);
        invoice.setTotalAmount(medicineFee + serviceFee);
    }
}
