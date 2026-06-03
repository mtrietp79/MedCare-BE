package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.AdminMedicineSummaryResponse;
import com.medcare.clinic_backend.dto.MedicineRequest;
import com.medcare.clinic_backend.dto.MedicineResponse;
import com.medcare.clinic_backend.entity.Medicine;
import com.medcare.clinic_backend.entity.MedicineCategory;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MedicineService {

    private static final String STATUS_OUT_OF_STOCK = "Hết hàng";
    private static final String STATUS_LOW_STOCK = "Sắp hết";
    private static final String STATUS_IN_STOCK = "Còn hàng";
    private static final String STOCK_STATUS_CODE_OUT_OF_STOCK = "OUT_OF_STOCK";
    private static final String STOCK_STATUS_CODE_LOW_STOCK = "LOW_STOCK";
    private static final String STOCK_STATUS_CODE_IN_STOCK = "IN_STOCK";
    private static final String STOCK_STATUS_LABEL_OUT_OF_STOCK = "Hết hàng";
    private static final String STOCK_STATUS_LABEL_LOW_STOCK = "Sắp hết hàng";
    private static final String STOCK_STATUS_LABEL_IN_STOCK = "Còn hàng";
    private static final String EXPIRY_STATUS_CODE_VALID = "VALID";
    private static final String EXPIRY_STATUS_CODE_EXPIRING_SOON = "EXPIRING_SOON";
    private static final String EXPIRY_STATUS_CODE_EXPIRED = "EXPIRED";
    private static final String EXPIRY_STATUS_LABEL_VALID = "Còn HSD";
    private static final String EXPIRY_STATUS_LABEL_EXPIRING_SOON = "Sắp hết HSD";
    private static final String EXPIRY_STATUS_LABEL_EXPIRED = "Hết HSD";
    private static final int EXPIRING_SOON_DAYS = 30;

    private static final Set<String> UNITS_PACK = Set.of("goi", "ong", "mieng");
    private static final Set<String> UNITS_BOTTLE = Set.of("tuyp", "chai", "lo");

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicineCategoryService medicineCategoryService;

    @Transactional(readOnly = true)
    public List<Medicine> getAllMedicines() {
        return getAllMedicines(null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<Medicine> getAllMedicines(String keyword, String category) {
        return getAllMedicines(keyword, null, null, category);
    }

    @Transactional(readOnly = true)
    public List<Medicine> getAllMedicines(String keyword, Integer categoryId, String status, String category) {
        String normalizedKeyword = foldSearchText(keyword);
        String normalizedCategory = foldSearchText(category);
        String normalizedStatus = foldSearchText(status);

        return medicineRepository.findAll().stream()
                .sorted(Comparator.comparing(this::sortKeyByName))
                .filter(medicine -> matchesKeywordFilter(medicine, normalizedKeyword))
                .filter(medicine -> matchesCategoryIdFilter(medicine, categoryId))
                .filter(medicine -> matchesCategoryNameFilter(medicine, normalizedCategory))
                .filter(medicine -> matchesStatusFilter(medicine, normalizedStatus))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getMedicineCategories() {
        return medicineCategoryService.getActiveCategoryNames();
    }

    @Transactional(readOnly = true)
    public Medicine getMedicineById(Integer id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay thuoc co ID: " + id));
    }

    @Transactional(readOnly = true)
    public MedicineResponse getMedicineResponseById(Integer id) {
        return toMedicineResponse(getMedicineById(id));
    }

    @Transactional(readOnly = true)
    public List<MedicineResponse> getAllMedicineResponses(String keyword, Integer categoryId, String status, String category) {
        return getAllMedicines(keyword, categoryId, status, category).stream()
                .map(this::toMedicineResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicineResponse> getAllAdminMedicines() {
        return getAllAdminMedicines(null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<MedicineResponse> getAllAdminMedicines(String keyword, Integer categoryId, String status, String category) {
        return getAllMedicineResponses(keyword, categoryId, status, category);
    }

    @Transactional(readOnly = true)
    public AdminMedicineSummaryResponse getAdminMedicineSummary() {
        List<Medicine> medicines = medicineRepository.findAll();
        long lowStockCount = 0;
        long outOfStockCount = 0;
        long expiredCount = 0;

        for (Medicine medicine : medicines) {
            String status = resolveStockStatus(
                    medicine == null ? null : medicine.getUnit(),
                    medicine == null ? null : medicine.getQuantity()
            );
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

    @Transactional
    public Medicine createMedicine(MedicineRequest request) {
        Medicine medicine = new Medicine();
        applyRequest(medicine, request);
        return medicineRepository.save(medicine);
    }

    @Transactional
    public MedicineResponse createAdminMedicine(MedicineRequest request) {
        return toMedicineResponse(createMedicine(request));
    }

    @Transactional
    public Medicine updateMedicine(Integer id, MedicineRequest request) {
        Medicine existingMedicine = getMedicineById(id);
        applyRequest(existingMedicine, request);
        return medicineRepository.save(existingMedicine);
    }

    @Transactional
    public MedicineResponse updateAdminMedicine(Integer id, MedicineRequest request) {
        return toMedicineResponse(updateMedicine(id, request));
    }

    @Transactional
    public void deleteMedicine(Integer id) {
        if (!medicineRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay thuoc co ID: " + id);
        }
        medicineRepository.deleteById(id);
    }

    @Transactional
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

    @Transactional
    public MedicineResponse updateAdminMedicineQuantity(Integer id, Integer quantity) {
        return toMedicineResponse(updateMedicineQuantity(id, quantity));
    }

    private void applyRequest(Medicine medicine, MedicineRequest request) {
        validateRequest(request);

        MedicineCategory category = medicineCategoryService.resolveCategory(
                request.getMedicineCategoryId(),
                resolveLegacyRequestedCategoryName(request)
        );
        String categoryName = resolveCategoryName(category);

        medicine.setName(request.getName().trim());
        medicine.setUnit(trimToNull(request.getUnit()));
        medicine.setPrice(request.getPrice());
        medicine.setDescription(trimToNull(request.getDescription()));
        medicine.setDosage(trimToNull(request.getDosage()));
        medicine.setQuantity(request.getQuantity() == null ? 0 : request.getQuantity());
        medicine.setManufacturer(trimToNull(request.getManufacturer()));
        medicine.setExpiryDate(request.getExpiryDate());
        medicine.setMedicineCategory(category);
        medicine.setLegacyMedicineCategory(categoryName);
        medicine.setCategory(categoryName);
        medicine.setStatus(resolveStockStatus(medicine.getUnit(), medicine.getQuantity()));
    }

    private void validateRequest(MedicineRequest request) {
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu thuoc khong hop le.");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ten thuoc khong duoc de trong.");
        }
        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Gia thuoc phai lon hon 0.");
        }
        if (request.getQuantity() != null && request.getQuantity() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So luong thuoc khong duoc am.");
        }
    }

    private String resolveLegacyRequestedCategoryName(MedicineRequest request) {
        String legacyMedicineCategory = trimToNull(request == null ? null : request.getMedicineCategory());
        if (legacyMedicineCategory != null) {
            return legacyMedicineCategory;
        }
        return trimToNull(request == null ? null : request.getCategory());
    }

    private String resolveCategoryName(MedicineCategory category) {
        String categoryName = category == null ? null : trimToNull(category.getName());
        return categoryName == null ? MedicineCategoryService.DEFAULT_CATEGORY_NAME : categoryName;
    }

    private String resolveCategoryName(Medicine medicine) {
        return resolveCategoryName(resolveEffectiveCategory(medicine));
    }

    private Integer resolveCategoryId(Medicine medicine) {
        MedicineCategory effectiveCategory = resolveEffectiveCategory(medicine);
        return effectiveCategory == null
                ? null
                : effectiveCategory.getId();
    }

    private MedicineCategory resolveEffectiveCategory(Medicine medicine) {
        if (medicine == null) {
            return medicineCategoryService.findByNameOrNull(MedicineCategoryService.DEFAULT_CATEGORY_NAME);
        }

        String preferredLegacyCategoryName = resolvePreferredLegacyCategoryName(medicine);
        MedicineCategory linkedCategory = medicine.getMedicineCategory();
        if (linkedCategory != null && sameCategoryName(linkedCategory.getName(), preferredLegacyCategoryName)) {
            return linkedCategory;
        }

        MedicineCategory categoryByPreferredName = medicineCategoryService.findByNameOrNull(preferredLegacyCategoryName);
        if (categoryByPreferredName != null) {
            return categoryByPreferredName;
        }

        if (linkedCategory != null) {
            return linkedCategory;
        }

        String legacyMedicineCategory = trimToNull(medicine.getLegacyMedicineCategory());
        MedicineCategory categoryByLegacyName = medicineCategoryService.findByNameOrNull(legacyMedicineCategory);
        if (categoryByLegacyName != null) {
            return categoryByLegacyName;
        }

        return medicineCategoryService.findByNameOrNull(MedicineCategoryService.DEFAULT_CATEGORY_NAME);
    }

    private String resolvePreferredLegacyCategoryName(Medicine medicine) {
        if (medicine == null) {
            return MedicineCategoryService.DEFAULT_CATEGORY_NAME;
        }

        String legacyMedicineCategory = trimToNull(medicine.getLegacyMedicineCategory());
        String category = trimToNull(medicine.getCategory());
        String unit = trimToNull(medicine.getUnit());

        if (legacyMedicineCategory != null && !isSystemFallbackCategoryValue(legacyMedicineCategory, category, unit)) {
            return legacyMedicineCategory;
        }
        if (category != null) {
            return category;
        }
        if (legacyMedicineCategory != null) {
            return legacyMedicineCategory;
        }
        return MedicineCategoryService.DEFAULT_CATEGORY_NAME;
    }

    private boolean isSystemFallbackCategoryValue(String candidate, String category, String unit) {
        String normalizedCandidate = foldSearchText(candidate);
        String normalizedCategory = foldSearchText(category);
        String normalizedUnit = foldSearchText(unit);

        if (normalizedCandidate == null) {
            return false;
        }
        if (normalizedCategory != null
                && !normalizedCategory.equals(normalizedCandidate)
                && foldSearchText(MedicineCategoryService.DEFAULT_CATEGORY_NAME).equals(normalizedCandidate)) {
            return true;
        }
        return normalizedCategory != null
                && !normalizedCategory.equals(normalizedCandidate)
                && normalizedUnit != null
                && normalizedUnit.equals(normalizedCandidate);
    }

    private boolean sameCategoryName(String left, String right) {
        return Objects.equals(foldSearchText(left), foldSearchText(right));
    }

    private String sortKeyByName(Medicine medicine) {
        String name = medicine == null ? null : medicine.getName();
        String normalized = trimToNull(name);
        return normalized == null ? "" : normalized.toLowerCase(Locale.ROOT);
    }

    private boolean matchesKeywordFilter(Medicine medicine, String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return true;
        }
        return containsFolded(medicine == null ? null : medicine.getName(), normalizedKeyword)
                || containsFolded(medicine == null ? null : medicine.getDescription(), normalizedKeyword)
                || containsFolded(medicine == null ? null : medicine.getManufacturer(), normalizedKeyword)
                || containsFolded(resolveCategoryName(medicine), normalizedKeyword);
    }

    private boolean matchesCategoryIdFilter(Medicine medicine, Integer categoryId) {
        if (categoryId == null) {
            return true;
        }
        return Objects.equals(resolveCategoryId(medicine), categoryId);
    }

    private boolean matchesCategoryNameFilter(Medicine medicine, String normalizedCategory) {
        if (normalizedCategory == null) {
            return true;
        }
        return normalizedCategory.equals(foldSearchText(resolveCategoryName(medicine)));
    }

    private boolean matchesStatusFilter(Medicine medicine, String normalizedStatus) {
        if (normalizedStatus == null) {
            return true;
        }
        return matchesStatusToken(normalizedStatus, resolveStockStatusCode(
                medicine == null ? null : medicine.getUnit(),
                medicine == null ? null : medicine.getQuantity()
        ))
                || matchesStatusToken(normalizedStatus, resolveStockStatusLabel(
                medicine == null ? null : medicine.getUnit(),
                medicine == null ? null : medicine.getQuantity()
        ))
                || matchesStatusToken(normalizedStatus, resolveStockStatus(
                medicine == null ? null : medicine.getUnit(),
                medicine == null ? null : medicine.getQuantity()
        ))
                || matchesStatusToken(normalizedStatus, resolveExpiryStatusCode(
                medicine == null ? null : medicine.getExpiryDate()
        ))
                || matchesStatusToken(normalizedStatus, resolveExpiryStatusLabel(
                medicine == null ? null : medicine.getExpiryDate()
        ));
    }

    private boolean matchesStatusToken(String normalizedFilter, String candidate) {
        return normalizedFilter != null && normalizedFilter.equals(foldSearchText(candidate));
    }

    private boolean containsFolded(String source, String normalizedKeyword) {
        String foldedSource = foldSearchText(source);
        return foldedSource != null && normalizedKeyword != null && foldedSource.contains(normalizedKeyword);
    }

    private String foldSearchText(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String withoutAccent = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccent
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String resolveStockStatus(String unit, Integer quantity) {
        return switch (resolveStockStatusCode(unit, quantity)) {
            case STOCK_STATUS_CODE_OUT_OF_STOCK -> STATUS_OUT_OF_STOCK;
            case STOCK_STATUS_CODE_LOW_STOCK -> STATUS_LOW_STOCK;
            default -> STATUS_IN_STOCK;
        };
    }

    public String resolveStockStatusCode(String unit, Integer quantity) {
        int safeQuantity = quantity == null ? 0 : quantity;
        String normalizedUnit = normalizeUnit(unit);

        if ("vien".equals(normalizedUnit)) {
            return stockStatusCodeByThreshold(safeQuantity, 15, 30);
        }
        if (UNITS_PACK.contains(normalizedUnit)) {
            return stockStatusCodeByThreshold(safeQuantity, 10, 20);
        }
        if (UNITS_BOTTLE.contains(normalizedUnit)) {
            return stockStatusCodeByThreshold(safeQuantity, 5, 10);
        }
        if (safeQuantity <= 0) {
            return STOCK_STATUS_CODE_OUT_OF_STOCK;
        }
        if (safeQuantity < 10) {
            return STOCK_STATUS_CODE_LOW_STOCK;
        }
        return STOCK_STATUS_CODE_IN_STOCK;
    }

    public String resolveStockStatusLabel(String unit, Integer quantity) {
        return switch (resolveStockStatusCode(unit, quantity)) {
            case STOCK_STATUS_CODE_OUT_OF_STOCK -> STOCK_STATUS_LABEL_OUT_OF_STOCK;
            case STOCK_STATUS_CODE_LOW_STOCK -> STOCK_STATUS_LABEL_LOW_STOCK;
            default -> STOCK_STATUS_LABEL_IN_STOCK;
        };
    }

    public void applyStockStatus(Medicine medicine) {
        if (medicine == null) {
            return;
        }
        medicine.setStatus(resolveStockStatus(medicine.getUnit(), medicine.getQuantity()));
    }

    private String stockStatusCodeByThreshold(int quantity, int outOfStockThreshold, int inStockThreshold) {
        if (quantity < outOfStockThreshold) {
            return STOCK_STATUS_CODE_OUT_OF_STOCK;
        }
        if (quantity < inStockThreshold) {
            return STOCK_STATUS_CODE_LOW_STOCK;
        }
        return STOCK_STATUS_CODE_IN_STOCK;
    }

    private String normalizeUnit(String unit) {
        String normalized = trimToNull(unit);
        if (normalized == null) {
            return null;
        }
        String withoutAccent = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccent
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private boolean isExpired(LocalDate expiryDate) {
        return EXPIRY_STATUS_CODE_EXPIRED.equals(resolveExpiryStatusCode(expiryDate));
    }

    public String resolveExpiryStatusCode(LocalDate expiryDate) {
        if (expiryDate == null) {
            return EXPIRY_STATUS_CODE_VALID;
        }

        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) {
            return EXPIRY_STATUS_CODE_EXPIRED;
        }
        if (!expiryDate.isAfter(today.plusDays(EXPIRING_SOON_DAYS))) {
            return EXPIRY_STATUS_CODE_EXPIRING_SOON;
        }
        return EXPIRY_STATUS_CODE_VALID;
    }

    public String resolveExpiryStatusLabel(LocalDate expiryDate) {
        return switch (resolveExpiryStatusCode(expiryDate)) {
            case EXPIRY_STATUS_CODE_EXPIRED -> EXPIRY_STATUS_LABEL_EXPIRED;
            case EXPIRY_STATUS_CODE_EXPIRING_SOON -> EXPIRY_STATUS_LABEL_EXPIRING_SOON;
            default -> EXPIRY_STATUS_LABEL_VALID;
        };
    }

    public MedicineResponse toMedicineResponse(Medicine medicine) {
        if (medicine == null) {
            MedicineResponse emptyResponse = new MedicineResponse();
            emptyResponse.setExpired(false);
            return emptyResponse;
        }

        String categoryName = resolveCategoryName(medicine);
        String stockStatus = resolveStockStatusCode(medicine.getUnit(), medicine.getQuantity());
        String stockStatusLabel = resolveStockStatusLabel(medicine.getUnit(), medicine.getQuantity());
        String expiryStatus = resolveExpiryStatusCode(medicine.getExpiryDate());
        String expiryStatusLabel = resolveExpiryStatusLabel(medicine.getExpiryDate());
        return new MedicineResponse(
                medicine.getId(),
                medicine.getName(),
                medicine.getDescription(),
                medicine.getUnit(),
                medicine.getPrice(),
                medicine.getQuantity(),
                medicine.getDosage(),
                medicine.getManufacturer(),
                medicine.getExpiryDate(),
                resolveStockStatus(medicine.getUnit(), medicine.getQuantity()),
                stockStatus,
                stockStatusLabel,
                expiryStatus,
                expiryStatusLabel,
                resolveCategoryId(medicine),
                categoryName,
                categoryName,
                medicine.getCategory(),
                EXPIRY_STATUS_CODE_EXPIRED.equals(expiryStatus)
        );
    }
}
