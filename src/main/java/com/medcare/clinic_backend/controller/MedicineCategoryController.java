package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.MedicineCategoryRequest;
import com.medcare.clinic_backend.dto.MedicineCategoryResponse;
import com.medcare.clinic_backend.service.MedicineCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/medicine-categories")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class MedicineCategoryController {

    @Autowired
    private MedicineCategoryService medicineCategoryService;

    @GetMapping
    public List<MedicineCategoryResponse> getAllCategories() {
        return medicineCategoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public MedicineCategoryResponse getCategoryById(@PathVariable Integer id) {
        return medicineCategoryService.getCategoryResponseById(id);
    }

    @PostMapping
    public MedicineCategoryResponse createCategory(@RequestBody MedicineCategoryRequest request) {
        return medicineCategoryService.createCategory(request);
    }

    @PutMapping("/{id}")
    public MedicineCategoryResponse updateCategory(
            @PathVariable Integer id,
            @RequestBody MedicineCategoryRequest request
    ) {
        return medicineCategoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Integer id) {
        medicineCategoryService.deleteCategory(id);
    }
}
