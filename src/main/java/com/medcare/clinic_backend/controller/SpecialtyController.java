package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.service.SpecialtyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@CrossOrigin("*") // Rất quan trọng: Cho phép ReactJS gọi API mà không bị chặn lỗi CORS
@RestController
@RequestMapping("/api/specialties") // Đường dẫn gốc của API này
public class SpecialtyController {

    @Autowired
    private SpecialtyService specialtyService;

    // API lấy danh sách: GET http://localhost:8080/api/specialties
    @GetMapping
    public List<Specialty> getAll() {
        return specialtyService.getAllSpecialties();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public Specialty create(@RequestBody Specialty specialty) {
        // @RequestBody sẽ tự động chuyển dữ liệu JSON từ ReactJS thành object Specialty trong Java
        return specialtyService.createSpecialty(specialty);
    }

    // API Lấy chi tiết 1 chuyên khoa: GET http://localhost:8080/api/specialties/{id}
    @GetMapping("/{id}")
    public Specialty getById(@PathVariable Integer id) {
        // @PathVariable giúp lấy con số {id} từ trên đường link URL truyền vào hàm
        return specialtyService.getSpecialtyById(id);
    }

    // API Sửa chuyên khoa: PUT http://localhost:8080/api/specialties/{id}
    @PutMapping("/{id}")
    public Specialty update(@PathVariable Integer id, @RequestBody Specialty specialty) {
        return specialtyService.updateSpecialty(id, specialty);
    }

    // API Xóa chuyên khoa: DELETE http://localhost:8080/api/specialties/{id}
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        specialtyService.deleteSpecialty(id);
    }
}