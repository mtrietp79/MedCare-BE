package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.feedback.MessageResponse;
import com.medcare.clinic_backend.dto.servicepackage.AdminServicePackageResponse;
import com.medcare.clinic_backend.dto.servicepackage.AdminServicePackageSummaryResponse;
import com.medcare.clinic_backend.entity.MedicalService;
import com.medcare.clinic_backend.entity.ServicePackage;
import com.medcare.clinic_backend.entity.ServicePackageBooking;
import com.medcare.clinic_backend.entity.ServicePackageItem;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.MedicalServiceRepository;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.repository.ServicePackageBookingRepository;
import com.medcare.clinic_backend.repository.ServicePackageRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicePackageServiceTest {

    @Mock
    private ServicePackageRepository servicePackageRepository;

    @Mock
    private MedicalServiceRepository medicalServiceRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientService patientService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ServicePackageBookingRepository servicePackageBookingRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @InjectMocks
    private ServicePackageService servicePackageService;

    @Test
    void getAllForAdmin_shouldFilterByKeywordActiveAndConfigured() {
        ServicePackage activeConfigured = samplePackage(1, "Goi Kham Tong Quat", true, true, "Xet nghiem mau");
        ServicePackage inactiveEmpty = samplePackage(2, "Goi Nhi", false, false, null);

        when(servicePackageRepository.findAll()).thenReturn(List.of(activeConfigured, inactiveEmpty));
        when(servicePackageBookingRepository.findAll()).thenReturn(List.of());

        List<AdminServicePackageResponse> responses =
                servicePackageService.getAllForAdmin("tong quat", true, true);

        assertEquals(1, responses.size());
        assertEquals("Goi Kham Tong Quat", responses.get(0).getName());
        assertEquals("ACTIVE", responses.get(0).getStatus());
        assertEquals(1, responses.get(0).getItemCount());
    }

    @Test
    void getAdminSummary_shouldCountPackagesByManagementState() {
        ServicePackage activeConfigured = samplePackage(1, "A", true, true, "XN");
        ServicePackage inactiveEmpty = samplePackage(2, "B", false, false, null);

        ServicePackageBooking booking = new ServicePackageBooking();
        booking.setServicePackage(activeConfigured);
        booking.setStatus("PAID");
        booking.setPaymentStatus("PAID");

        when(servicePackageRepository.findAll()).thenReturn(List.of(activeConfigured, inactiveEmpty));
        when(servicePackageBookingRepository.findAll()).thenReturn(List.of(booking));

        AdminServicePackageSummaryResponse summary = servicePackageService.getAdminSummary();

        assertEquals(2, summary.getTotalPackages());
        assertEquals(1, summary.getActivePackages());
        assertEquals(1, summary.getInactivePackages());
        assertEquals(1, summary.getPackagesWithBookings());
        assertEquals(1, summary.getPackagesWithoutItems());
    }

    @Test
    void setActiveForAdmin_shouldToggleStatusAndExposeDisplayFields() {
        ServicePackage servicePackage = samplePackage(10, "Goi XN", false, true, "Mau");
        when(servicePackageRepository.findById(10)).thenReturn(Optional.of(servicePackage));
        when(servicePackageRepository.save(servicePackage)).thenReturn(servicePackage);
        when(servicePackageBookingRepository.findAll()).thenReturn(List.of());

        AdminServicePackageResponse response = servicePackageService.setActiveForAdmin(10, true);

        assertTrue(Boolean.TRUE.equals(servicePackage.getIsActive()));
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("Dang hoat dong", response.getStatusDisplay());
    }

    @Test
    void deleteForAdmin_shouldRejectWhenPackageAlreadyHasBookings() {
        when(servicePackageRepository.existsById(5)).thenReturn(true);
        when(servicePackageBookingRepository.existsByServicePackage_Id(5)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> servicePackageService.deleteForAdmin(5)
        );

        assertEquals("Khong the xoa goi dich vu da co booking. Vui long chuyen sang tam ngung.", exception.getMessage());
    }

    @Test
    void deleteForAdmin_shouldDeleteUnusedPackage() {
        when(servicePackageRepository.existsById(6)).thenReturn(true);
        when(servicePackageBookingRepository.existsByServicePackage_Id(6)).thenReturn(false);

        MessageResponse response = servicePackageService.deleteForAdmin(6);

        assertEquals("Da xoa goi dich vu.", response.getMessage());
        verify(servicePackageRepository).deleteById(6);
    }

    private ServicePackage samplePackage(
            Integer id,
            String name,
            boolean active,
            boolean configured,
            String medicalServiceName
    ) {
        ServicePackage servicePackage = new ServicePackage();
        servicePackage.setId(id);
        servicePackage.setName(name);
        servicePackage.setDescription(name + " desc");
        servicePackage.setPrice(500000.0);
        servicePackage.setDurationMinutes(60);
        servicePackage.setIsActive(active);
        servicePackage.setCreatedAt(LocalDateTime.of(2026, 6, 3, 10, 0));
        servicePackage.setUpdatedAt(LocalDateTime.of(2026, 6, 3, 10, 0));

        if (configured) {
            MedicalService medicalService = new MedicalService();
            medicalService.setId(id * 100);
            medicalService.setName(medicalServiceName);

            ServicePackageItem item = new ServicePackageItem();
            item.setId(id * 1000);
            item.setServicePackage(servicePackage);
            item.setMedicalService(medicalService);
            servicePackage.getItems().add(item);
        }

        return servicePackage;
    }
}
