package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.AdminMedicineResponse;
import com.medcare.clinic_backend.dto.AdminMedicineSummaryResponse;
import com.medcare.clinic_backend.entity.Medicine;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MedicineService {

    private static final String STATUS_OUT_OF_STOCK = "H\u1EBFt h\u00E0ng";
    private static final String STATUS_LOW_STOCK = "S\u1EAFp h\u1EBFt";
    private static final String STATUS_IN_STOCK = "C\u00F2n h\u00E0ng";

    private static final Set<String> UNITS_PACK = Set.of("goi", "ong", "mieng");
    private static final Set<String> UNITS_BOTTLE = Set.of("tuyp", "chai", "lo");

    @Autowired
    private MedicineRepository medicineRepository;

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    public Medicine getMedicineById(Integer id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay thuoc co ID: " + id));
    }

    public List<AdminMedicineResponse> getAllAdminMedicines() {
        return medicineRepository.findAll()
                .stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    public AdminMedicineSummaryResponse getAdminMedicineSummary() {
        List<Medicine> medicines = medicineRepository.findAll();
        long lowStockCount = 0;
        long outOfStockCount = 0;
        long expiredCount = 0;

        for (Medicine medicine : medicines) {
            String status = resolveStockStatus(medicine == null ? null : medicine.getUnit(), medicine == null ? null : medicine.getQuantity());
            if (STATUS_LOW_STOCK.equals(status)) {
                lowStockCount++;
            } else if (STATUS_OUT_OF_STOCK.equals(status)) {
                outOfStockCount++;
            }
            if (isExpired(medicine == null ? null : medicine.getExpiryDate())) {
                expiredCount++;
            }
        }

        return new AdminMedicineSummaryResponse(
                lowStockCount,
                outOfStockCount,
                expiredCount,
                medicines.size()
        );
    }

    public Medicine createMedicine(Medicine medicine) {
        validateMedicine(medicine);
        normalizeMedicine(medicine);
        return medicineRepository.save(medicine);
    }

    public Medicine updateMedicine(Integer id, Medicine medicineDetails) {
        Medicine existingMedicine = getMedicineById(id);
        validateMedicine(medicineDetails);
        normalizeMedicine(medicineDetails);

        existingMedicine.setName(medicineDetails.getName());
        existingMedicine.setUnit(medicineDetails.getUnit());
        existingMedicine.setPrice(medicineDetails.getPrice());
        existingMedicine.setDescription(medicineDetails.getDescription());
        existingMedicine.setDosage(medicineDetails.getDosage());
        existingMedicine.setQuantity(medicineDetails.getQuantity());
        existingMedicine.setCategory(medicineDetails.getCategory());
        existingMedicine.setManufacturer(medicineDetails.getManufacturer());
        existingMedicine.setExpiryDate(medicineDetails.getExpiryDate());
        existingMedicine.setStatus(medicineDetails.getStatus());

        return medicineRepository.save(existingMedicine);
    }

    public void deleteMedicine(Integer id) {
        if (!medicineRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay thuoc co ID: " + id);
        }
        medicineRepository.deleteById(id);
    }

    public Medicine updateMedicineQuantity(Integer id, Integer quantity) {
        Medicine medicine = getMedicineById(id);
        if (quantity == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So luong khong duoc de trong.");
        }
        if (quantity < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So luong thuoc khong duoc am.");
        }

        medicine.setQuantity(quantity);
        medicine.setStatus(resolveStockStatus(medicine.getUnit(), quantity));
        return medicineRepository.save(medicine);
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
        if (medicine.getQuantity() != null && medicine.getQuantity() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So luong thuoc khong duoc am.");
        }
    }

    private void normalizeMedicine(Medicine medicine) {
        medicine.setName(medicine.getName().trim());
        medicine.setUnit(trimToNull(medicine.getUnit()));
        medicine.setDescription(trimToNull(medicine.getDescription()));
        medicine.setDosage(trimToNull(medicine.getDosage()));
        medicine.setQuantity(medicine.getQuantity() == null ? 0 : medicine.getQuantity());
        medicine.setCategory(trimToNull(medicine.getCategory()));
        medicine.setManufacturer(trimToNull(medicine.getManufacturer()));
        medicine.setStatus(resolveStockStatus(medicine.getUnit(), medicine.getQuantity()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String resolveStockStatus(String unit, Integer quantity) {
        int safeQuantity = quantity == null ? 0 : quantity;
        String normalizedUnit = normalizeUnit(unit);

        if ("vien".equals(normalizedUnit)) {
            return statusByThreshold(safeQuantity, 15, 30);
        }
        if (UNITS_PACK.contains(normalizedUnit)) {
            return statusByThreshold(safeQuantity, 10, 20);
        }
        if (UNITS_BOTTLE.contains(normalizedUnit)) {
            return statusByThreshold(safeQuantity, 5, 10);
        }
        return statusByThreshold(safeQuantity, 10, 20);
    }

    public void applyStockStatus(Medicine medicine) {
        if (medicine == null) {
            return;
        }
        medicine.setStatus(resolveStockStatus(medicine.getUnit(), medicine.getQuantity()));
    }

    private String statusByThreshold(int quantity, int outOfStockThreshold, int inStockThreshold) {
        if (quantity < outOfStockThreshold) {
            return STATUS_OUT_OF_STOCK;
        }
        if (quantity < inStockThreshold) {
            return STATUS_LOW_STOCK;
        }
        return STATUS_IN_STOCK;
    }

    private String normalizeUnit(String unit) {
        String normalized = trimToNull(unit);
        if (normalized == null) {
            return null;
        }
        String withoutAccent = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccent
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private boolean isExpired(LocalDate expiryDate) {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    private AdminMedicineResponse toAdminResponse(Medicine medicine) {
        if (medicine == null) {
            return new AdminMedicineResponse(
                    null, null, null, null, null, null, null,
                    null, null, null, null, false
            );
        }
        String computedStatus = resolveStockStatus(medicine.getUnit(), medicine.getQuantity());
            return new AdminMedicineResponse(
                medicine.getId(),
                medicine.getName(),
                medicine.getCategory(),
                medicine.getManufacturer(),
                medicine.getQuantity(),
                medicine.getUnit(),
                medicine.getPrice(),
                medicine.getExpiryDate(),
                computedStatus,
                medicine.getDosage(),
                medicine.getDescription(),
                isExpired(medicine.getExpiryDate())
        );
    }
}
