package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AdminMedicineSummaryResponse;
import com.medcare.clinic_backend.dto.MedicineRequest;
import com.medcare.clinic_backend.dto.MedicineResponse;
import com.medcare.clinic_backend.dto.MedicineQuantityUpdateRequest;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/medicines")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminMedicineController {

    @Autowired
    private MedicineService medicineService;

    @GetMapping
    public List<MedicineResponse> getAllMedicines(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        return medicineService.getAllAdminMedicines(keyword, categoryId, status, category);
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return medicineService.getMedicineCategories();
    }

    @GetMapping("/summary")
    public AdminMedicineSummaryResponse getSummary() {
        return medicineService.getAdminMedicineSummary();
    }

    @PostMapping
    public MedicineResponse createMedicine(@RequestBody MedicineRequest request) {
        return medicineService.createAdminMedicine(request);
    }

    @PutMapping("/{id}")
    public MedicineResponse updateMedicine(@PathVariable Integer id, @RequestBody MedicineRequest request) {
        return medicineService.updateAdminMedicine(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteMedicine(@PathVariable Integer id) {
        medicineService.deleteMedicine(id);
    }

    @PatchMapping("/{id}/quantity")
    public MedicineResponse updateQuantity(@PathVariable Integer id, @RequestBody MedicineQuantityUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu cap nhat so luong khong hop le.");
        }
        return medicineService.updateAdminMedicineQuantity(id, request.getQuantity());
    }
}
