package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.invoice.FinanceSummaryResponse;
import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import com.medcare.clinic_backend.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminFinanceController {

    @Autowired
    private FinanceService financeService;

    @GetMapping({"/api/admin/finance", "/api/admin/finance/invoices", "/api/admin/invoices"})
    public List<InvoiceResponse> getAdminInvoices(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category
    ) {
        return financeService.getInvoiceResponsesForAdmin(keyword, status, category);
    }

    @GetMapping({"/api/admin/finance/summary", "/api/admin/invoices/summary"})
    public FinanceSummaryResponse getAdminFinanceSummary(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category
    ) {
        return financeService.buildSummary(financeService.getInvoiceResponsesForAdmin(keyword, status, category));
    }
}
