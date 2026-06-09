package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.specialty.AdminSpecialtyListItemResponse;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import com.medcare.clinic_backend.repository.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;

    @InjectMocks
    private SpecialtyService specialtyService;

    private Specialty specialty;

    @BeforeEach
    void setUp() {
        specialty = new Specialty();
        specialty.setId(1);
        specialty.setName("Nội tổng quát");
        specialty.setIsActive(true);
    }

    @Test
    void deleteSuccessWhenNoRelatedData() {
        when(specialtyRepository.findById(1)).thenReturn(Optional.of(specialty));
        when(doctorRepository.countBySpecialty_Id(1)).thenReturn(0L);
        when(appointmentRepository.countByDoctorSpecialtyId(1)).thenReturn(0L);
        when(medicalRecordRepository.countByDoctorSpecialtyId(1)).thenReturn(0L);

        var result = specialtyService.deleteSpecialtySafely(1);

        assertThat(result).isNull();
        verify(specialtyRepository).delete(specialty);
    }

    @Test
    void deleteBlockedWhenHasDoctors() {
        when(specialtyRepository.findById(1)).thenReturn(Optional.of(specialty));
        when(doctorRepository.countBySpecialty_Id(1)).thenReturn(2L);
        when(appointmentRepository.countByDoctorSpecialtyId(1)).thenReturn(0L);
        when(medicalRecordRepository.countByDoctorSpecialtyId(1)).thenReturn(0L);

        var result = specialtyService.deleteSpecialtySafely(1);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("SPECIALTY_HAS_RELATED_DATA");
        assertThat(result.getDoctorCount()).isEqualTo(2L);
        verify(specialtyRepository, never()).delete(any());
    }

    @Test
    void deleteBlockedWhenHasAppointments() {
        when(specialtyRepository.findById(1)).thenReturn(Optional.of(specialty));
        when(doctorRepository.countBySpecialty_Id(1)).thenReturn(0L);
        when(appointmentRepository.countByDoctorSpecialtyId(1)).thenReturn(1L);
        when(medicalRecordRepository.countByDoctorSpecialtyId(1)).thenReturn(0L);

        var result = specialtyService.deleteSpecialtySafely(1);

        assertThat(result).isNotNull();
        assertThat(result.getAppointmentCount()).isEqualTo(1L);
    }

    @Test
    void deleteBlockedWhenHasMedicalRecords() {
        when(specialtyRepository.findById(1)).thenReturn(Optional.of(specialty));
        when(doctorRepository.countBySpecialty_Id(1)).thenReturn(0L);
        when(appointmentRepository.countByDoctorSpecialtyId(1)).thenReturn(0L);
        when(medicalRecordRepository.countByDoctorSpecialtyId(1)).thenReturn(3L);

        var result = specialtyService.deleteSpecialtySafely(1);

        assertThat(result).isNotNull();
        assertThat(result.getMedicalRecordCount()).isEqualTo(3L);
    }

    @Test
    void deactivateKeepsSpecialtyAndHidesFromPublicList() {
        when(specialtyRepository.findById(1)).thenReturn(Optional.of(specialty));
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        specialtyService.deactivateSpecialty(1);

        assertThat(specialty.getIsActive()).isFalse();
        verify(specialtyRepository).save(specialty);

        Specialty activeSpecialty = new Specialty();
        activeSpecialty.setId(2);
        activeSpecialty.setName("Da liễu");
        activeSpecialty.setIsActive(true);
        when(specialtyRepository.findByIsActiveTrue()).thenReturn(List.of(activeSpecialty));
        when(doctorRepository.countBySpecialty_Id(2)).thenReturn(0L);

        var publicList = specialtyService.getAllSpecialties();
        assertThat(publicList).extracting(Specialty::getId).containsExactly(2);

        when(specialtyRepository.findAll()).thenReturn(List.of(specialty, activeSpecialty));
        when(doctorRepository.countBySpecialty_Id(1)).thenReturn(0L);

        var adminList = specialtyService.getAllSpecialtiesForAdmin();
        assertThat(adminList).extracting(Specialty::getId).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void activateAllSpecialtiesUpdatesEveryRow() {
        when(specialtyRepository.activateAll()).thenReturn(10);

        int updatedCount = specialtyService.activateAllSpecialties();

        assertThat(updatedCount).isEqualTo(10);
        verify(specialtyRepository).activateAll();
    }

    @Test
    void deactivateAllSpecialtiesUpdatesEveryRow() {
        when(specialtyRepository.deactivateAll()).thenReturn(10);

        int updatedCount = specialtyService.deactivateAllSpecialties();

        assertThat(updatedCount).isEqualTo(10);
        verify(specialtyRepository).deactivateAll();
    }

    @Test
    void getAdminListReturnsPaginatedResponseWithTotals() {
        List<Specialty> pageContent = IntStream.rangeClosed(1, 10)
                .mapToObj(this::buildSpecialty)
                .toList();
        when(specialtyRepository.findAdminSpecialties(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(pageContent, org.springframework.data.domain.PageRequest.of(0, 10), 20));
        when(doctorRepository.countBySpecialty_Id(any())).thenReturn(2L);

        var result = specialtyService.getAdminList("", "ALL", 0, 10, "name_asc");

        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(20);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getContent().getFirst())
                .extracting(AdminSpecialtyListItemResponse::getDoctorCount)
                .isEqualTo(2L);
    }

    @Test
    void getAdminListFiltersActiveStatus() {
        Specialty activeSpecialty = buildSpecialty(1);
        activeSpecialty.setIsActive(true);
        when(specialtyRepository.findAdminSpecialties(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeSpecialty)));
        when(doctorRepository.countBySpecialty_Id(1)).thenReturn(0L);

        var result = specialtyService.getAdminList(null, "ACTIVE", 0, 10, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getIsActive()).isTrue();
        verify(specialtyRepository).findAdminSpecialties(eq(true), any(Pageable.class));
    }

    @Test
    void getAdminListFiltersInactiveStatus() {
        Specialty inactiveSpecialty = buildSpecialty(2);
        inactiveSpecialty.setIsActive(false);
        when(specialtyRepository.findAdminSpecialties(eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inactiveSpecialty)));
        when(doctorRepository.countBySpecialty_Id(2)).thenReturn(0L);

        var result = specialtyService.getAdminList(null, "INACTIVE", 0, 10, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getIsActive()).isFalse();
        verify(specialtyRepository).findAdminSpecialties(eq(false), any(Pageable.class));
    }

    @Test
    void getAdminListUsesKeywordSearch() {
        Specialty specialty = buildSpecialty(3);
        specialty.setName("Nội tổng quát");
        when(specialtyRepository.searchAdminSpecialtiesByKeyword(eq("%nội%"), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(specialty)));
        when(doctorRepository.countBySpecialty_Id(3)).thenReturn(1L);

        var result = specialtyService.getAdminList("nội", "ALL", 0, 10, "name_asc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Nội tổng quát");
        verify(specialtyRepository).searchAdminSpecialtiesByKeyword(eq("%nội%"), isNull(), any(Pageable.class));
    }

    @Test
    void getAdminListMapsSortToEntityFields() {
        when(specialtyRepository.findAdminSpecialties(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        specialtyService.getAdminList(null, "ALL", 0, 10, "name_desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(specialtyRepository).findAdminSpecialties(isNull(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().toString()).contains("name: DESC");
    }

    private Specialty buildSpecialty(int id) {
        Specialty item = new Specialty();
        item.setId(id);
        item.setName("Chuyên khoa " + id);
        item.setDescription("Mô tả " + id);
        item.setIsActive(true);
        return item;
    }
}
