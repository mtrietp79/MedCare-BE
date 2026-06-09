package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.invoice.FinanceSummaryResponse;
import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import com.medcare.clinic_backend.dto.invoice.PatientInvoiceDetailResponse;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import com.medcare.clinic_backend.service.DoctorService;
import com.medcare.clinic_backend.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public List<InvoiceResponse> getAllInvoices(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return financeService.getInvoiceResponsesForDoctor(currentDoctor.getId(), keyword, status, category);
        }
        return financeService.getInvoiceResponsesForAdmin(keyword, status, category);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public FinanceSummaryResponse getInvoiceSummary(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<InvoiceResponse> invoices;
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            invoices = financeService.getInvoiceResponsesForDoctor(currentDoctor.getId(), keyword, status, category);
        } else {
            invoices = financeService.getInvoiceResponsesForAdmin(keyword, status, category);
        }
        return financeService.buildSummary(invoices);
    }

    @GetMapping("/record/{recordId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public InvoiceResponse getInvoiceByRecordId(@PathVariable Integer recordId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            InvoiceResponse invoice = financeService.getInvoiceResponseByRecordId(recordId, currentDoctor.getId());
            if (invoice == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay hoa don cho ho so ID: " + recordId);
            }
            return invoice;
        }
        InvoiceResponse invoice = financeService.getInvoiceResponseByRecordId(recordId, null);
        if (invoice == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay hoa don cho ho so ID: " + recordId);
        }
        return invoice;
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public InvoiceResponse payInvoice(@PathVariable Integer id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay hoa don ID: " + id));
        boolean alreadyPaid = "PAID".equalsIgnoreCase(invoice.getStatus());
        invoice.setStatus("PAID");
        invoiceRepository.save(invoice);
        if (!alreadyPaid) {
            TransactionLog log = new TransactionLog();
            log.setInvoiceId(invoice.getId());
            log.setAmount(invoice.getTotalAmount());
            log.setResponseCode("MANUAL_PAID");
            transactionLogRepository.save(log);
        }
        Integer recordId = invoice.getMedicalRecord() == null ? null : invoice.getMedicalRecord().getId();
        if (recordId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Hoa don khong lien ket ho so benh an.");
        }
        InvoiceResponse response = financeService.getInvoiceResponseByRecordId(recordId, null);
        if (response == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay hoa don cho ho so ID: " + recordId);
        }
        return response;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public List<InvoiceResponse> getMyInvoices(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type,
            Authentication authentication
    ) {
        Patient currentPatient = getCurrentPatientOrThrow(authentication);
        return financeService.getInvoiceResponsesForPatient(currentPatient.getId(), keyword, status, category, type);
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public PatientInvoiceDetailResponse getMyInvoiceById(
            @PathVariable Integer id,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "uniqueKey", required = false) String uniqueKey,
            Authentication authentication
    ) {
        Patient currentPatient = getCurrentPatientOrThrow(authentication);
        return financeService.getPatientInvoiceDetail(currentPatient.getId(), id, sourceType, uniqueKey);
    }

    @GetMapping("/my/record/{recordId}")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public InvoiceResponse getMyInvoiceByRecordId(@PathVariable Integer recordId, Authentication authentication) {
        Patient currentPatient = getCurrentPatientOrThrow(authentication);
        InvoiceResponse invoice = financeService.getInvoiceResponseByRecordIdForPatient(recordId, currentPatient.getId());
        if (invoice == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay hoa don cho ho so ID: " + recordId);
        }
        return invoice;
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private Doctor getCurrentDoctorOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return doctorService.getDoctorByAccountUsername(authentication.getName());
    }

    private Patient getCurrentPatientOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return patientRepository.findByAccount_Username(authentication.getName())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan cua ban chua duoc lien ket voi ho so benh nhan."
                ));
    }
}
