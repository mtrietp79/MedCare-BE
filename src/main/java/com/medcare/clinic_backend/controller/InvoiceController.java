package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.service.DoctorService;
import com.medcare.clinic_backend.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private DoctorService doctorService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public List<Invoice> getAllInvoices() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return invoiceService.getInvoicesForDoctor(currentDoctor.getId());
        }
        return invoiceRepository.findAll();
    }

    @GetMapping("/record/{recordId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public Invoice getInvoiceByRecordId(@PathVariable Integer recordId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return invoiceService.getInvoiceByRecordIdForDoctor(recordId, currentDoctor.getId());
        }
        return invoiceService.getInvoiceByRecordId(recordId);
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Invoice payInvoice(@PathVariable Integer id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay hoa don ID: " + id));
        invoice.setStatus("PAID");
        return invoiceRepository.save(invoice);
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
}
