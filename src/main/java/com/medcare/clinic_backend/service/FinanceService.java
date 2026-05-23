package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.invoice.FinanceSummaryResponse;
import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class FinanceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    public List<InvoiceResponse> getInvoiceResponsesForAdmin(String keyword, String status) {
        return filterInvoices(toResponses(invoiceRepository.findAll()), keyword, status);
    }

    public List<InvoiceResponse> getInvoiceResponsesForDoctor(Integer doctorId, String keyword, String status) {
        List<Invoice> invoices = invoiceRepository.findByMedicalRecordDoctorIdOrderByCreatedAtDesc(doctorId);
        return filterInvoices(toResponses(invoices), keyword, status);
    }

    public List<InvoiceResponse> getInvoiceResponsesForPatient(Integer patientId, String keyword, String status) {
        List<Invoice> invoices = invoiceRepository.findByMedicalRecordPatientIdOrderByCreatedAtDesc(patientId);
        return filterInvoices(toResponses(invoices), keyword, status);
    }

    public InvoiceResponse getInvoiceResponseByRecordId(Integer recordId, Integer doctorIdOrNull) {
        Invoice invoice = (doctorIdOrNull == null)
                ? invoiceRepository.findByMedicalRecordId(recordId).orElse(null)
                : invoiceRepository.findByMedicalRecordIdAndMedicalRecordDoctorId(recordId, doctorIdOrNull).orElse(null);
        return invoice == null ? null : toResponse(invoice);
    }

    public InvoiceResponse getInvoiceResponseByRecordIdForPatient(Integer recordId, Integer patientId) {
        Invoice invoice = invoiceRepository.findByMedicalRecordIdAndMedicalRecordPatientId(recordId, patientId).orElse(null);
        return invoice == null ? null : toResponse(invoice);
    }

    public InvoiceResponse getInvoiceResponseByIdForPatient(Integer invoiceId, Integer patientId) {
        Invoice invoice = invoiceRepository.findByIdAndMedicalRecordPatientId(invoiceId, patientId).orElse(null);
        return invoice == null ? null : toResponse(invoice);
    }

    public FinanceSummaryResponse buildSummary(List<InvoiceResponse> invoices) {
        LocalDateTime startOfCurrentMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfNextMonth = startOfCurrentMonth.plusMonths(1);

        double totalRevenue = 0.0;
        double monthlyRevenue = 0.0;
        double pendingAmount = 0.0;
        long paidCount = 0;
        long pendingCount = 0;

        for (InvoiceResponse invoice : invoices) {
            if (invoice == null) {
                continue;
            }
            boolean paid = isPaidStatus(invoice.getStatus());
            double totalAmount = safeDouble(invoice.getTotalAmount());
            if (paid) {
                paidCount++;
                totalRevenue += totalAmount;
                LocalDateTime createdAt = invoice.getCreatedAt();
                if (createdAt != null && !createdAt.isBefore(startOfCurrentMonth) && createdAt.isBefore(startOfNextMonth)) {
                    monthlyRevenue += totalAmount;
                }
            } else {
                pendingCount++;
                pendingAmount += totalAmount;
            }
        }

        return new FinanceSummaryResponse(
                totalRevenue,
                monthlyRevenue,
                pendingAmount,
                paidCount,
                pendingCount,
                invoices == null ? 0 : invoices.size()
        );
    }

    private List<InvoiceResponse> toResponses(List<Invoice> invoices) {
        List<InvoiceResponse> responses = new ArrayList<>();
        if (invoices == null) {
            return responses;
        }
        for (Invoice invoice : invoices) {
            responses.add(toResponse(invoice));
        }
        responses.sort((a, b) -> {
            LocalDateTime aTime = a.getCreatedAt();
            LocalDateTime bTime = b.getCreatedAt();
            if (aTime == null && bTime == null) {
                return 0;
            }
            if (aTime == null) {
                return 1;
            }
            if (bTime == null) {
                return -1;
            }
            return bTime.compareTo(aTime);
        });
        return responses;
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        Integer invoiceId = invoice.getId();
        Integer recordId = invoice.getMedicalRecord() == null ? null : invoice.getMedicalRecord().getId();
        String patientName = null;
        String patientPhone = null;
        String doctorName = null;
        if (invoice.getMedicalRecord() != null && invoice.getMedicalRecord().getPatient() != null) {
            patientName = invoice.getMedicalRecord().getPatient().getFullName();
            patientPhone = invoice.getMedicalRecord().getPatient().getPhone();
        }
        if (invoice.getMedicalRecord() != null && invoice.getMedicalRecord().getDoctor() != null) {
            doctorName = invoice.getMedicalRecord().getDoctor().getFullName();
        }
        String invoiceCode = invoiceId == null ? null : "INV" + String.format("%06d", invoiceId);
        double medicineFee = safeDouble(invoice.getMedicineFee());
        double serviceFee = safeDouble(invoice.getServiceFee());
        double totalAmount = safeDouble(invoice.getTotalAmount());
        if (totalAmount <= 0 && (medicineFee + serviceFee) > 0) {
            totalAmount = medicineFee + serviceFee;
        }

        return new InvoiceResponse(
                invoiceId,
                invoiceCode,
                recordId,
                recordId,
                safeText(patientName),
                safeText(patientName),
                safeText(patientPhone),
                safeText(doctorName),
                safeText(doctorName),
                medicineFee,
                serviceFee,
                totalAmount,
                totalAmount,
                normalizeNull(invoice.getStatus()),
                canPayOnline(invoice),
                invoice.getCreatedAt()
        );
    }

    private List<InvoiceResponse> filterInvoices(List<InvoiceResponse> invoices, String keyword, String status) {
        String keywordNorm = normalizeFilter(keyword);
        String statusNorm = normalizeFilter(status);
        if (keywordNorm == null && statusNorm == null) {
            return invoices;
        }

        List<InvoiceResponse> filtered = new ArrayList<>();
        for (InvoiceResponse invoice : invoices) {
            if (invoice == null) {
                continue;
            }
            if (statusNorm != null) {
                String invoiceStatus = normalizeFilter(invoice.getStatus());
                if (invoiceStatus == null || !invoiceStatus.equals(statusNorm)) {
                    continue;
                }
            }
            if (keywordNorm != null && !containsKeyword(invoice, keywordNorm)) {
                continue;
            }
            filtered.add(invoice);
        }
        return filtered;
    }

    private boolean containsKeyword(InvoiceResponse invoice, String keywordNorm) {
        return contains(normalizeFilter(invoice.getInvoiceCode()), keywordNorm)
                || contains(normalizeFilter(invoice.getPatientName()), keywordNorm)
                || contains(normalizeFilter(invoice.getPatientPhone()), keywordNorm)
                || contains(normalizeFilter(invoice.getDoctorName()), keywordNorm)
                || contains(normalizeFilter(invoice.getRecordId() == null ? null : String.valueOf(invoice.getRecordId())), keywordNorm)
                || contains(normalizeFilter(invoice.getId() == null ? null : String.valueOf(invoice.getId())), keywordNorm);
    }

    private boolean contains(String text, String keyword) {
        return text != null && keyword != null && text.contains(keyword);
    }

    private boolean isPaidStatus(String status) {
        String normalized = normalizeFilter(status);
        return normalized != null
                && ("paid".equals(normalized)
                || "da_thanh_toan".equals(normalized)
                || "thanh_toan_thanh_cong".equals(normalized));
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = Normalizer.normalize(trimmed.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace(' ', '_')
                .replace('-', '_')
                .replaceAll("[^a-z0-9_]", "")
                .replaceAll("_+", "_");
        return normalized.isBlank() ? null : normalized;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String normalizeNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private boolean canPayOnline(Invoice invoice) {
        if (invoice == null) {
            return false;
        }
        String status = normalizeFilter(invoice.getStatus());
        double payableAmount = safeDouble(invoice.getTotalAmount());
        if (payableAmount <= 0) {
            payableAmount = safeDouble(invoice.getMedicineFee()) + safeDouble(invoice.getServiceFee());
        }
        return (status == null || "unpaid".equals(status) || "pending".equals(status))
                && payableAmount > 0;
    }
}
