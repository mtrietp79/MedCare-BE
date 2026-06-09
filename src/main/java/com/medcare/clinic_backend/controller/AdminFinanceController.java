package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.cancellation.AdminCancellationActionRequest;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationActionResponse;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationRequestDetailResponse;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationRequestListItemResponse;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationRequestStatsResponse;
import com.medcare.clinic_backend.dto.invoice.FinanceSummaryResponse;
import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import com.medcare.clinic_backend.service.AppointmentCancellationService;
import com.medcare.clinic_backend.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminFinanceController {

    @Autowired
    private FinanceService financeService;

    @Autowired
    private AppointmentCancellationService appointmentCancellationService;

    @GetMapping({"/api/admin/finance", "/api/admin/finance/invoices", "/api/admin/invoices"})
    public Page<InvoiceResponse> getAdminInvoices(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        String resolvedCategory = resolveCategory(category, type);
        List<InvoiceResponse> filtered = financeService.getInvoiceResponsesForAdmin(keyword, status, resolvedCategory)
                .stream()
                .sorted(resolveSort(sort))
                .toList();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        int start = Math.min((int) pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @GetMapping({"/api/admin/finance/summary", "/api/admin/finance/stats", "/api/admin/invoices/summary"})
    public FinanceSummaryResponse getAdminFinanceSummary(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type
    ) {
        String resolvedCategory = resolveCategory(category, type);
        return financeService.buildSummary(financeService.getInvoiceResponsesForAdmin(keyword, status, resolvedCategory));
    }

    @GetMapping("/api/admin/finance/cancellation-requests")
    public Page<AdminCancellationRequestListItemResponse> getCancellationRequests(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        return appointmentCancellationService.getAdminList(keyword, status, page, size, sort);
    }

    @GetMapping("/api/admin/finance/cancellation-requests/stats")
    public AdminCancellationRequestStatsResponse getCancellationRequestStats() {
        return appointmentCancellationService.getAdminStats();
    }

    @GetMapping("/api/admin/finance/cancellation-requests/{id}")
    public AdminCancellationRequestDetailResponse getCancellationRequestDetail(@PathVariable Integer id) {
        return appointmentCancellationService.getAdminDetail(id);
    }

    @PatchMapping("/api/admin/finance/cancellation-requests/{id}/approve")
    public AdminCancellationActionResponse approveCancellationRequest(
            @PathVariable Integer id,
            @RequestBody(required = false) AdminCancellationActionRequest request,
            Authentication authentication
    ) {
        return appointmentCancellationService.approve(id, authentication.getName(), request);
    }

    @PatchMapping("/api/admin/finance/cancellation-requests/{id}/reject")
    public AdminCancellationActionResponse rejectCancellationRequest(
            @PathVariable Integer id,
            @RequestBody(required = false) AdminCancellationActionRequest request,
            Authentication authentication
    ) {
        return appointmentCancellationService.reject(id, authentication.getName(), request);
    }

    @PatchMapping("/api/admin/finance/cancellation-requests/{id}/mark-refunded")
    public AdminCancellationActionResponse markCancellationRequestRefunded(
            @PathVariable Integer id,
            @RequestBody(required = false) AdminCancellationActionRequest request,
            Authentication authentication
    ) {
        return appointmentCancellationService.markRefunded(id, authentication.getName(), request);
    }

    @GetMapping("/api/admin/finance/revenue")
    public FinanceSummaryResponse getRevenue(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type
    ) {
        String resolvedCategory = resolveCategory(category, type);
        return financeService.buildSummary(financeService.getInvoiceResponsesForAdmin(keyword, status, resolvedCategory));
    }

    private Comparator<InvoiceResponse> resolveSort(String sort) {
        String normalized = trimToNull(sort);
        if (normalized == null || "newest".equalsIgnoreCase(normalized)) {
            return Comparator.comparing(InvoiceResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("oldest".equalsIgnoreCase(normalized)) {
            return Comparator.comparing(InvoiceResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        return Comparator.comparing(InvoiceResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private String resolveCategory(String category, String type) {
        String categoryValue = trimToNull(category);
        if (categoryValue != null) {
            return "ALL".equalsIgnoreCase(categoryValue) ? null : categoryValue;
        }
        String typeValue = trimToNull(type);
        if (typeValue == null || "ALL".equalsIgnoreCase(typeValue)) {
            return null;
        }
        return typeValue;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
