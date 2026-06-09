package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.servicepackage.AdminServicePackageBookingResponse;
import com.medcare.clinic_backend.dto.servicepackage.UpdateServicePackageBookingStatusRequest;
import com.medcare.clinic_backend.service.ServicePackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/service-package-bookings", "/api/admin/service-packages/bookings"})
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminServicePackageBookingController {

    @Autowired
    private ServicePackageService servicePackageService;

    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        List<AdminServicePackageBookingResponse> all = servicePackageService.getAllBookingsForAdmin(null, null);
        long total = all.size();
        long pendingCount = all.stream().filter(item -> "PENDING_PAYMENT".equalsIgnoreCase(item.getStatus())).count();
        long paidCount = all.stream().filter(item -> "PAID".equalsIgnoreCase(item.getStatus())).count();
        long completedCount = all.stream().filter(item -> "COMPLETED".equalsIgnoreCase(item.getStatus())).count();
        long cancelledCount = all.stream().filter(item -> "CANCELLED".equalsIgnoreCase(item.getStatus())).count();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("pendingCount", pendingCount);
        stats.put("paidCount", paidCount);
        stats.put("completedCount", completedCount);
        stats.put("cancelledCount", cancelledCount);
        return stats;
    }

    @GetMapping
    public Page<AdminServicePackageBookingResponse> getAllBookings(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        List<AdminServicePackageBookingResponse> all = servicePackageService.getAllBookingsForAdmin(status, keyword).stream()
                .sorted(resolveSort(sort))
                .toList();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        int start = Math.min((int) pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    @GetMapping("/{id}")
    public AdminServicePackageBookingResponse getDetail(@PathVariable Integer id) {
        return servicePackageService.getAllBookingsForAdmin(null, null).stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new com.medcare.clinic_backend.exception.BusinessException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Khong tim thay dat goi dich vu ID: " + id
                ));
    }

    @PatchMapping("/{id}/status")
    public AdminServicePackageBookingResponse updateStatus(
            @PathVariable Integer id,
            @RequestBody UpdateServicePackageBookingStatusRequest request
    ) {
        return servicePackageService.updateBookingStatusForAdmin(
                id,
                request == null ? null : request.getStatus()
        );
    }

    private Comparator<AdminServicePackageBookingResponse> resolveSort(String sort) {
        String normalized = trimToNull(sort);
        if (normalized == null || "newest".equalsIgnoreCase(normalized)) {
            return Comparator.comparing(AdminServicePackageBookingResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("oldest".equalsIgnoreCase(normalized)) {
            return Comparator.comparing(AdminServicePackageBookingResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        return Comparator.comparing(AdminServicePackageBookingResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
