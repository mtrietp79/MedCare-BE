package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.MedicineCategoryRequest;
import com.medcare.clinic_backend.dto.MedicineCategoryResponse;
import com.medcare.clinic_backend.entity.MedicineCategory;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.MedicineCategoryRepository;
import com.medcare.clinic_backend.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MedicineCategoryService {

    public static final String DEFAULT_CATEGORY_NAME = "Khác";

    @Autowired
    private MedicineCategoryRepository medicineCategoryRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Transactional(readOnly = true)
    public List<MedicineCategoryResponse> getAllCategories() {
        return medicineCategoryRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MedicineCategoryResponse getCategoryResponseById(Integer id) {
        return toResponse(getCategoryById(id));
    }

    @Transactional
    public MedicineCategoryResponse createCategory(MedicineCategoryRequest request) {
        MedicineCategory category = new MedicineCategory();
        applyRequest(category, request, null);
        return toResponse(medicineCategoryRepository.save(category));
    }

    @Transactional
    public MedicineCategoryResponse updateCategory(Integer id, MedicineCategoryRequest request) {
        MedicineCategory category = getCategoryById(id);
        applyRequest(category, request, id);
        return toResponse(medicineCategoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Integer id) {
        MedicineCategory category = getCategoryById(id);
        if (medicineRepository.existsByMedicineCategory_Id(id)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Không thể xóa danh mục đang có thuốc.");
        }
        medicineCategoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<String> getActiveCategoryNames() {
        List<MedicineCategory> categories = medicineCategoryRepository.findByIsActiveTrueOrderByNameAsc();
        if (categories.isEmpty()) {
            return List.of(DEFAULT_CATEGORY_NAME);
        }
        return categories.stream()
                .map(MedicineCategory::getName)
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public MedicineCategory resolveCategory(Integer categoryId, String fallbackName) {
        if (categoryId != null) {
            MedicineCategory category = medicineCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "medicineCategoryId không tồn tại."));
            if (!Boolean.TRUE.equals(category.getIsActive())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Danh mục thuốc đang bị vô hiệu hóa.");
            }
            return category;
        }

        String normalizedFallback = trimToNull(fallbackName);
        if (normalizedFallback != null) {
            return medicineCategoryRepository.findByNameIgnoreCase(normalizedFallback)
                    .orElseGet(() -> createCategoryIfMissing(normalizedFallback));
        }

        return getOrCreateDefaultCategory();
    }

    @Transactional
    public MedicineCategory getOrCreateDefaultCategory() {
        return medicineCategoryRepository.findByNameIgnoreCase(DEFAULT_CATEGORY_NAME)
                .orElseGet(() -> {
                    MedicineCategory category = new MedicineCategory();
                    category.setName(DEFAULT_CATEGORY_NAME);
                    category.setIsActive(Boolean.TRUE);
                    return medicineCategoryRepository.save(category);
                });
    }

    @Transactional(readOnly = true)
    public MedicineCategory findByNameOrNull(String name) {
        String normalizedName = trimToNull(name);
        if (normalizedName == null) {
            return null;
        }
        return medicineCategoryRepository.findByNameIgnoreCase(normalizedName).orElse(null);
    }

    @Transactional(readOnly = true)
    public MedicineCategory getCategoryById(Integer id) {
        return medicineCategoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục thuốc ID: " + id));
    }

    private MedicineCategory createCategoryIfMissing(String name) {
        MedicineCategory category = new MedicineCategory();
        category.setName(name);
        category.setIsActive(Boolean.TRUE);
        return medicineCategoryRepository.save(category);
    }

    private void applyRequest(MedicineCategory category, MedicineCategoryRequest request, Integer currentId) {
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Dữ liệu danh mục thuốc không hợp lệ.");
        }

        String normalizedName = trimToNull(request.getName());
        if (normalizedName == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tên danh mục thuốc không được để trống.");
        }

        medicineCategoryRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Tên danh mục thuốc đã tồn tại.");
                });

        category.setName(normalizedName);
        category.setDescription(trimToNull(request.getDescription()));
        category.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
    }

    private MedicineCategoryResponse toResponse(MedicineCategory category) {
        if (category == null) {
            return new MedicineCategoryResponse(null, null, null, null, null, null);
        }
        return new MedicineCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIsActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
