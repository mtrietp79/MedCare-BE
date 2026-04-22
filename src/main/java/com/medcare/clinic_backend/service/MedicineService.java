package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Medicine;
import com.medcare.clinic_backend.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicineService {

    @Autowired
    private MedicineRepository medicineRepository;

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    public Medicine getMedicineById(Integer id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc có ID: " + id));
    }

    public Medicine createMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    public Medicine updateMedicine(Integer id, Medicine medicineDetails) {
        Medicine existingMedicine = getMedicineById(id);

        // Cập nhật các trường thông tin (Ông nhớ sửa lại tên các hàm get/set cho đúng với Entity của ông nhé)
        existingMedicine.setName(medicineDetails.getName());
        // existingMedicine.setPrice(medicineDetails.getPrice());
        // existingMedicine.setQuantity(medicineDetails.getQuantity());

        return medicineRepository.save(existingMedicine);
    }

    public void deleteMedicine(Integer id) {
        medicineRepository.deleteById(id);
    }
}