package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AdminMedicineResponse;
import com.medcare.clinic_backend.dto.AdminMedicineSummaryResponse;
import com.medcare.clinic_backend.dto.MedicineQuantityUpdateRequest;
import com.medcare.clinic_backend.entity.Medicine;
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
    public List<AdminMedicineResponse> getAllMedicines() {
        return medicineService.getAllAdminMedicines();
    }

    @GetMapping("/summary")
    public AdminMedicineSummaryResponse getSummary() {
        return medicineService.getAdminMedicineSummary();
    }

    @PostMapping
    public Medicine createMedicine(@RequestBody Medicine medicine) {
        return medicineService.createMedicine(medicine);
    }

    @PutMapping("/{id}")
    public Medicine updateMedicine(@PathVariable Integer id, @RequestBody Medicine medicine) {
        return medicineService.updateMedicine(id, medicine);
    }

    @DeleteMapping("/{id}")
    public void deleteMedicine(@PathVariable Integer id) {
        medicineService.deleteMedicine(id);
    }

    @PatchMapping("/{id}/quantity")
    public Medicine updateQuantity(@PathVariable Integer id, @RequestBody MedicineQuantityUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu cap nhat so luong khong hop le.");
        }
        return medicineService.updateMedicineQuantity(id, request.getQuantity());
    }
}
