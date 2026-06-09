package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.feedback.MessageResponse;
import com.medcare.clinic_backend.dto.specialty.AdminSpecialtyListItemResponse;
import com.medcare.clinic_backend.dto.specialty.SpecialtyActivationResponse;
import com.medcare.clinic_backend.dto.specialty.SpecialtyBulkActivationResponse;
import com.medcare.clinic_backend.dto.specialty.SpecialtyDeleteCheckResponse;
import com.medcare.clinic_backend.dto.specialty.SpecialtyDeleteConflictResponse;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.service.SpecialtyService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/specialties")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminSpecialtyController {

    private final SpecialtyService specialtyService;

    public AdminSpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @GetMapping
    public Page<AdminSpecialtyListItemResponse> getAllSpecialtiesForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        return specialtyService.getAdminList(keyword, status, page, size, sort);
    }

    @GetMapping("/{id}/delete-check")
    public SpecialtyDeleteCheckResponse checkDelete(@PathVariable Integer id) {
        return specialtyService.getDeleteCheck(id);
    }

    @DeleteMapping({"/{id}", "/{id}/delete"})
    public ResponseEntity<?> deleteSpecialty(@PathVariable Integer id) {
        SpecialtyDeleteConflictResponse conflict = specialtyService.deleteSpecialtySafely(id);
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflict);
        }
        return ResponseEntity.ok(new MessageResponse("Xóa chuyên khoa thành công"));
    }

    @PatchMapping("/deactivate-all")
    public SpecialtyBulkActivationResponse deactivateAllSpecialties() {
        int updatedCount = specialtyService.deactivateAllSpecialties();
        return new SpecialtyBulkActivationResponse("Tạm ngưng tất cả chuyên khoa thành công", updatedCount);
    }

    @PatchMapping("/activate-all")
    public SpecialtyBulkActivationResponse activateAllSpecialties() {
        int updatedCount = specialtyService.activateAllSpecialties();
        return new SpecialtyBulkActivationResponse("Bật hoạt động tất cả chuyên khoa thành công", updatedCount);
    }

    @PatchMapping("/{id}/deactivate")
    public SpecialtyActivationResponse deactivateSpecialty(@PathVariable Integer id) {
        Specialty specialty = specialtyService.deactivateAndReturn(id);
        return new SpecialtyActivationResponse("Tạm ngưng chuyên khoa thành công", specialty.getId(), specialty.getIsActive());
    }

    @PatchMapping("/{id}/activate")
    public SpecialtyActivationResponse activateSpecialty(@PathVariable Integer id) {
        Specialty specialty = specialtyService.activateSpecialty(id);
        return new SpecialtyActivationResponse("Kích hoạt chuyên khoa thành công", specialty.getId(), specialty.getIsActive());
    }
}
