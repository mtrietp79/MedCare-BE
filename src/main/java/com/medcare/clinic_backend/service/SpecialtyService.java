package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.repository.SpecialtyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecialtyService {

    @Autowired
    private SpecialtyRepository specialtyRepository;

    // Hàm lấy toàn bộ danh sách chuyên khoa
    public List<Specialty> getAllSpecialties() {
        return specialtyRepository.findAll();
    }

    public Specialty createSpecialty(Specialty specialty) {
        return specialtyRepository.save(specialty);
    }

    // 1. Hàm lấy chi tiết 1 chuyên khoa theo ID (Xem chi tiết)
    public Specialty getSpecialtyById(Integer id) {
        // findById sẽ tìm trong DB, nếu không thấy thì trả về null
        return specialtyRepository.findById(id).orElse(null);
    }

    // 2. Hàm cập nhật chuyên khoa (Sửa)
    public Specialty updateSpecialty(Integer id, Specialty specialtyDetails) {
        // Tìm xem chuyên khoa có tồn tại không
        Specialty specialty = specialtyRepository.findById(id).orElse(null);
        if (specialty != null) {
            // Nếu có, tiến hành cập nhật thông tin
            specialty.setName(specialtyDetails.getName());
            specialty.setDescription(specialtyDetails.getDescription());
            return specialtyRepository.save(specialty); // Lưu lại vào DB
        }
        return null;
    }

    // 3. Hàm xóa chuyên khoa (Xóa)
    public void deleteSpecialty(Integer id) {
        specialtyRepository.deleteById(id);
    }
}