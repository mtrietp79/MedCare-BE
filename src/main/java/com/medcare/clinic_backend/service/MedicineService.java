package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Medicine;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay thuoc co ID: " + id));
    }

    public Medicine createMedicine(Medicine medicine) {
        validateMedicine(medicine);
        return medicineRepository.save(medicine);
    }

    public Medicine updateMedicine(Integer id, Medicine medicineDetails) {
        Medicine existingMedicine = getMedicineById(id);
        validateMedicine(medicineDetails);

        existingMedicine.setName(medicineDetails.getName().trim());
        existingMedicine.setUnit(medicineDetails.getUnit() == null ? null : medicineDetails.getUnit().trim());
        existingMedicine.setPrice(medicineDetails.getPrice());
        existingMedicine.setDescription(medicineDetails.getDescription() == null ? null : medicineDetails.getDescription().trim());

        return medicineRepository.save(existingMedicine);
    }

    public void deleteMedicine(Integer id) {
        if (!medicineRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay thuoc co ID: " + id);
        }
        medicineRepository.deleteById(id);
    }

    private void validateMedicine(Medicine medicine) {
        if (medicine == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu thuoc khong hop le.");
        }
        if (medicine.getName() == null || medicine.getName().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ten thuoc khong duoc de trong.");
        }
        if (medicine.getPrice() == null || medicine.getPrice() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Gia thuoc phai lon hon 0.");
        }
    }
}
