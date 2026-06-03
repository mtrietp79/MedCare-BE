package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PrescriptionDetailRepository prescriptionRepository;

    @Autowired
    private ServiceDetailRepository serviceDetailRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Transactional
    public Invoice createInvoiceFromRecord(MedicalRecord record) {
        if (record == null || record.getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Medical record khong hop le de tao hoa don.");
        }

        Integer recordId = record.getId();
        Optional<Invoice> existingOptional = invoiceRepository.findByMedicalRecordId(recordId);
        InvoiceTotals totals = resolveInvoiceTotals(record);

        Invoice invoice = existingOptional.orElseGet(Invoice::new);
        invoice.setMedicalRecord(record);
        invoice.setAppointment(record.getAppointment());
        applyInvoiceTotals(invoice, totals);
        invoice.setStatus(resolveInvoiceStatus(invoice.getStatus()));
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void recalculateInvoiceForRecord(Integer recordId) {
        if (recordId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu medicalRecordId.");
        }

        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay medicalRecordId: " + recordId
                ));
        InvoiceTotals totals = resolveInvoiceTotals(record);
        Invoice invoice = invoiceRepository.findByMedicalRecordId(recordId).orElseGet(() -> {
            Invoice created = new Invoice();
            created.setMedicalRecord(record);
            created.setAppointment(record.getAppointment());
            created.setStatus("UNPAID");
            return created;
        });

        invoice.setAppointment(record.getAppointment());
        applyInvoiceTotals(invoice, totals);
        invoice.setStatus(resolveInvoiceStatus(invoice.getStatus()));
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

    private void applyInvoiceTotals(Invoice invoice, InvoiceTotals totals) {
        invoice.setConsultationFee(totals.consultationFee());
        invoice.setMedicineFee(totals.medicineFee());
        invoice.setServiceFee(totals.serviceFee());
        invoice.setTotalAmount(totals.totalAmount());
    }

    private InvoiceTotals resolveInvoiceTotals(MedicalRecord record) {
        Integer recordId = record.getId();

        double consultationFee = resolveConsultationFee(record.getAppointment());
        double medicineFee = prescriptionRepository.findByMedicalRecordId(recordId)
                .stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getMedicine().getPrice())
                .sum();

        double serviceFee = serviceDetailRepository.findByMedicalRecordId(recordId)
                .stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getMedicalService().getPrice())
                .sum();

        return new InvoiceTotals(consultationFee, medicineFee, serviceFee);
    }

    private double resolveConsultationFee(Appointment appointment) {
        if (appointment == null) {
            return 0.0;
        }
        if (!isFollowUpType(appointment.getAppointmentType()) && isAppointmentPaid(appointment.getPaymentStatus())) {
            return 0.0;
        }
        Double fee = appointment.getConsultationFee();
        return fee == null ? 0.0 : Math.max(fee, 0.0);
    }

    private boolean isAppointmentPaid(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isBlank()) {
            return false;
        }
        String normalized = paymentStatus.trim().toUpperCase(Locale.ROOT);
        return "PAID".equals(normalized) || "PAID_ONLINE".equals(normalized);
    }

    private boolean isFollowUpType(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        String folded = Normalizer.normalize(type, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .replace(" ", "");
        return folded.contains("taikham");
    }

    private String resolveInvoiceStatus(String currentStatus) {
        if (currentStatus == null || currentStatus.isBlank()) {
            return "UNPAID";
        }
        if (isPaidStatus(currentStatus)) {
            return "PAID";
        }
        return "UNPAID";
    }

    private boolean isPaidStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "PAID".equals(normalized) || normalized.contains("PAID_ONLINE");
    }

    private record InvoiceTotals(double consultationFee, double medicineFee, double serviceFee) {
        double totalAmount() {
            return consultationFee + medicineFee + serviceFee;
        }
    }
}
