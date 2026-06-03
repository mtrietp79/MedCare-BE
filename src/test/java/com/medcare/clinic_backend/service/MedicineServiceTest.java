package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.AdminMedicineSummaryResponse;
import com.medcare.clinic_backend.dto.MedicineRequest;
import com.medcare.clinic_backend.entity.Medicine;
import com.medcare.clinic_backend.entity.MedicineCategory;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.MedicineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicineCategoryService medicineCategoryService;

    @InjectMocks
    private MedicineService medicineService;

    @Test
    void createMedicine_shouldTrimFieldsAndPersistResolvedCategory() {
        MedicineRequest request = new MedicineRequest();
        request.setName("  Paracetamol  ");
        request.setUnit("  vien  ");
        request.setPrice(2000.0);
        request.setQuantity(100);
        request.setManufacturer("  STADA  ");

        MedicineCategory category = sampleCategory(1, MedicineCategoryService.DEFAULT_CATEGORY_NAME);

        when(medicineCategoryService.resolveCategory(null, null)).thenReturn(category);
        when(medicineRepository.save(any(Medicine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Medicine created = medicineService.createMedicine(request);

        assertNotNull(created);
        assertEquals("Paracetamol", created.getName());
        assertEquals("vien", created.getUnit());
        assertEquals("STADA", created.getManufacturer());
        assertSame(category, created.getMedicineCategory());
        assertEquals(MedicineCategoryService.DEFAULT_CATEGORY_NAME, created.getLegacyMedicineCategory());
        assertEquals(MedicineCategoryService.DEFAULT_CATEGORY_NAME, created.getCategory());
    }

    @Test
    void updateMedicineQuantity_shouldRejectNegativeQuantity() {
        Medicine existing = sampleMedicine(10, "Paracetamol", "vien", 100, null);

        when(medicineRepository.findById(10)).thenReturn(Optional.of(existing));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> medicineService.updateMedicineQuantity(10, -1)
        );

        assertEquals("So luong thuoc khong duoc am.", exception.getMessage());
    }

    @Test
    void getAdminMedicineSummary_shouldCountLowStockOutOfStockExpiredAndTotal() {
        Medicine lowStock = sampleMedicine(1, "Vitamin C", "vien", 20, LocalDate.now().plusDays(90));
        Medicine outOfStockAndExpired = sampleMedicine(2, "Amoxicillin", "vien", 10, LocalDate.now().minusDays(1));
        Medicine inStock = sampleMedicine(3, "Omega 3", "chai", 12, LocalDate.now().plusDays(180));

        when(medicineRepository.findAll()).thenReturn(List.of(lowStock, outOfStockAndExpired, inStock));

        AdminMedicineSummaryResponse summary = medicineService.getAdminMedicineSummary();

        assertEquals(1, summary.getLowStockCount());
        assertEquals(1, summary.getOutOfStockCount());
        assertEquals(1, summary.getExpiredCount());
        assertEquals(3, summary.getTotal());
    }

    private Medicine sampleMedicine(Integer id, String name, String unit, Integer quantity, LocalDate expiryDate) {
        Medicine medicine = new Medicine();
        medicine.setId(id);
        medicine.setName(name);
        medicine.setUnit(unit);
        medicine.setPrice(2000.0);
        medicine.setQuantity(quantity);
        medicine.setExpiryDate(expiryDate);
        medicine.setMedicineCategory(sampleCategory(1, MedicineCategoryService.DEFAULT_CATEGORY_NAME));
        medicine.setLegacyMedicineCategory(MedicineCategoryService.DEFAULT_CATEGORY_NAME);
        medicine.setCategory(MedicineCategoryService.DEFAULT_CATEGORY_NAME);
        return medicine;
    }

    private MedicineCategory sampleCategory(Integer id, String name) {
        MedicineCategory category = new MedicineCategory();
        category.setId(id);
        category.setName(name);
        category.setIsActive(Boolean.TRUE);
        return category;
    }
}
