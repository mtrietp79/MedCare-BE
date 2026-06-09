package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.DoctorResponse;
import com.medcare.clinic_backend.dto.doctor.UpdateDoctorActiveStatusRequest;
import com.medcare.clinic_backend.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin/doctors")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminDoctorController {

    @Autowired
    private DoctorService doctorService;

    @GetMapping
    public Page<DoctorResponse> getDoctors(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "ALL") String specialtyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        Integer specialtyFilter = parseSpecialtyId(specialtyId);
        String normalizedKeyword = trimToNull(keyword);

        List<DoctorResponse> filtered = doctorService.getAllDoctorResponses(null, normalizedKeyword, true).stream()
                .filter(doctor -> specialtyFilter == null || specialtyFilter.equals(doctor.getSpecialtyId()))
                .filter(doctor -> matchesStatus(doctor, status))
                .sorted(resolveSort(sort))
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        int start = Math.min((int) pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @GetMapping("/{id}")
    public DoctorResponse getDoctorDetail(@PathVariable Integer id) {
        return doctorService.getDoctorResponseById(id);
    }

    @PatchMapping("/{id}/active-status")
    public DoctorResponse updateActiveStatus(
            @PathVariable Integer id,
            @RequestBody UpdateDoctorActiveStatusRequest request
    ) {
        Boolean active = resolveActiveValue(request);
        return doctorService.toDoctorResponse(doctorService.updateDoctorActiveStatus(id, active));
    }

    private Boolean resolveActiveValue(UpdateDoctorActiveStatusRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getActive() != null) {
            return request.getActive();
        }
        String status = request.getStatus();
        if (status == null) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if ("ACTIVE".equals(normalized) || "HOAT_DONG".equals(normalized) || "HOAT DONG".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("INACTIVE".equals(normalized)
                || "KHONG_HOAT_DONG".equals(normalized)
                || "KHONG HOAT DONG".equals(normalized)
                || "TAM_NGUNG".equals(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private boolean matchesStatus(DoctorResponse doctor, String status) {
        String normalized = trimToNull(status);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return true;
        }
        Boolean active = doctor.getActive();
        if (active == null) {
            active = doctor.getStatus() != null && "ACTIVE".equalsIgnoreCase(doctor.getStatus().trim());
        }
        if ("ACTIVE".equalsIgnoreCase(normalized)) {
            return Boolean.TRUE.equals(active);
        }
        if ("INACTIVE".equalsIgnoreCase(normalized) || "LOCKED".equalsIgnoreCase(normalized)) {
            return Boolean.FALSE.equals(active);
        }
        return true;
    }

    private Integer parseSpecialtyId(String specialtyId) {
        String normalized = trimToNull(specialtyId);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Comparator<DoctorResponse> resolveSort(String sort) {
        String normalized = trimToNull(sort);
        if (normalized == null || "name_asc".equalsIgnoreCase(normalized)) {
            return Comparator.comparing((DoctorResponse response) -> safeText(response.getFullName()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(DoctorResponse::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("name_desc".equalsIgnoreCase(normalized)) {
            return Comparator.comparing((DoctorResponse response) -> safeText(response.getFullName()), String.CASE_INSENSITIVE_ORDER)
                    .reversed()
                    .thenComparing(DoctorResponse::getId, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("newest".equalsIgnoreCase(normalized)) {
            return Comparator.comparing(DoctorResponse::getId, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("oldest".equalsIgnoreCase(normalized)) {
            return Comparator.comparing(DoctorResponse::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        return Comparator.comparing((DoctorResponse response) -> safeText(response.getFullName()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(DoctorResponse::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
