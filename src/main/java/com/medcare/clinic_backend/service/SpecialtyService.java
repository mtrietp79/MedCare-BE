package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.specialty.AdminSpecialtyListItemResponse;
import com.medcare.clinic_backend.dto.specialty.SpecialtyDeleteCheckResponse;
import com.medcare.clinic_backend.dto.specialty.SpecialtyDeleteConflictResponse;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import com.medcare.clinic_backend.repository.SpecialtyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SpecialtyService {

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    public List<Specialty> getAllSpecialties() {
        return specialtyRepository.findByIsActiveTrue().stream()
                .map(this::enrichDoctorCount)
                .toList();
    }

    public List<Specialty> getAllSpecialtiesForAdmin() {
        return specialtyRepository.findAll().stream()
                .map(this::enrichDoctorCount)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminSpecialtyListItemResponse> getAdminList(String keyword,
                                                             String status,
                                                             int page,
                                                             int size,
                                                             String sort) {
        Boolean activeFilter = toActiveFilter(status);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), parseSort(sort));
        String keywordPattern = toLikePattern(trimToNull(keyword));
        Page<Specialty> specialties = keywordPattern == null
                ? specialtyRepository.findAdminSpecialties(activeFilter, pageable)
                : specialtyRepository.searchAdminSpecialtiesByKeyword(keywordPattern, activeFilter, pageable);
        List<AdminSpecialtyListItemResponse> content = specialties.getContent().stream()
                .map(this::toAdminListItemResponse)
                .toList();
        return new PageImpl<>(content, specialties.getPageable(), specialties.getTotalElements());
    }

    public Specialty createSpecialty(Specialty specialty) {
        validateSpecialty(specialty);
        return enrichDoctorCount(specialtyRepository.save(specialty));
    }

    public Specialty getSpecialtyById(Integer id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay chuyen khoa ID: " + id));
        return enrichDoctorCount(specialty);
    }

    public Specialty updateSpecialty(Integer id, Specialty specialtyDetails) {
        Specialty specialty = getSpecialtyById(id);
        validateSpecialty(specialtyDetails);
        specialty.setName(specialtyDetails.getName().trim());
        specialty.setDescription(specialtyDetails.getDescription());
        if (specialtyDetails.getIsActive() != null) {
            specialty.setIsActive(specialtyDetails.getIsActive());
        }
        return enrichDoctorCount(specialtyRepository.save(specialty));
    }

    public SpecialtyDeleteCheckResponse getDeleteCheck(Integer id) {
        Specialty specialty = getRawSpecialtyById(id);
        RelatedDataCounts counts = getRelatedDataCounts(specialty.getId());
        return SpecialtyDeleteCheckResponse.builder()
                .canDelete(counts.canDelete())
                .doctorCount(counts.doctorCount())
                .appointmentCount(counts.appointmentCount())
                .medicalRecordCount(counts.medicalRecordCount())
                .message(buildDeleteCheckMessage(counts))
                .build();
    }

    public SpecialtyDeleteConflictResponse deleteSpecialtySafely(Integer id) {
        Specialty specialty = getRawSpecialtyById(id);
        RelatedDataCounts counts = getRelatedDataCounts(specialty.getId());
        if (counts.canDelete()) {
            specialtyRepository.delete(specialty);
            return null;
        }
        return SpecialtyDeleteConflictResponse.builder()
                .code("SPECIALTY_HAS_RELATED_DATA")
                .message("Không thể xóa chuyên khoa. Chuyên khoa này hiện đang có bác sĩ hoặc dữ liệu khám bệnh liên quan. Để đảm bảo dữ liệu hệ thống không bị mất, bạn không thể xóa trực tiếp chuyên khoa này. Bạn có thể chuyển các bác sĩ sang chuyên khoa khác hoặc tạm ngưng chuyên khoa để ẩn khỏi hệ thống đặt lịch.")
                .doctorCount(counts.doctorCount())
                .appointmentCount(counts.appointmentCount())
                .medicalRecordCount(counts.medicalRecordCount())
                .build();
    }

    public void deactivateSpecialty(Integer id) {
        Specialty specialty = getRawSpecialtyById(id);
        specialty.setIsActive(false);
        specialtyRepository.save(specialty);
    }

    public Specialty activateSpecialty(Integer id) {
        Specialty specialty = getRawSpecialtyById(id);
        specialty.setIsActive(true);
        return specialtyRepository.save(specialty);
    }

    public Specialty deactivateAndReturn(Integer id) {
        Specialty specialty = getRawSpecialtyById(id);
        specialty.setIsActive(false);
        return specialtyRepository.save(specialty);
    }

    @Transactional
    public int activateAllSpecialties() {
        return specialtyRepository.activateAll();
    }

    @Transactional
    public int deactivateAllSpecialties() {
        return specialtyRepository.deactivateAll();
    }

    private void validateSpecialty(Specialty specialty) {
        if (specialty == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu chuyen khoa khong hop le.");
        }
        if (specialty.getName() == null || specialty.getName().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ten chuyen khoa khong duoc de trong.");
        }
    }

    private Specialty enrichDoctorCount(Specialty specialty) {
        if (specialty == null) {
            return null;
        }
        if (specialty.getId() == null) {
            specialty.setTotalDoctors(0L);
            specialty.setDoctorCount(0L);
            return specialty;
        }
        long totalDoctors = doctorRepository.countBySpecialty_Id(specialty.getId());
        specialty.setTotalDoctors(totalDoctors);
        specialty.setDoctorCount(totalDoctors);
        return specialty;
    }

    private Specialty getRawSpecialtyById(Integer id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay chuyen khoa ID: " + id));
    }

    private RelatedDataCounts getRelatedDataCounts(Integer specialtyId) {
        long doctorCount = doctorRepository.countBySpecialty_Id(specialtyId);
        long appointmentCount = appointmentRepository.countByDoctorSpecialtyId(specialtyId);
        long medicalRecordCount = medicalRecordRepository.countByDoctorSpecialtyId(specialtyId);
        return new RelatedDataCounts(doctorCount, appointmentCount, medicalRecordCount);
    }

    private String buildDeleteCheckMessage(RelatedDataCounts counts) {
        if (counts.canDelete()) {
            return "Chuyên khoa này có thể xóa.";
        }
        if (counts.doctorCount() > 0) {
            return "Chuyên khoa này hiện đang có " + counts.doctorCount() + " bác sĩ đang hoạt động. Bạn không thể xóa chuyên khoa khi vẫn còn bác sĩ thuộc chuyên khoa này. Vui lòng chuyển các bác sĩ sang chuyên khoa khác hoặc tạm ngưng chuyên khoa này.";
        }
        if (counts.appointmentCount() > 0) {
            return "Chuyên khoa này đang có lịch hẹn liên quan. Bạn không thể xóa chuyên khoa khi dữ liệu lịch hẹn vẫn còn tồn tại.";
        }
        return "Chuyên khoa này đang có bệnh án liên quan. Bạn không thể xóa chuyên khoa khi dữ liệu bệnh án vẫn còn tồn tại.";
    }

    private AdminSpecialtyListItemResponse toAdminListItemResponse(Specialty specialty) {
        Specialty enriched = enrichDoctorCount(specialty);
        return AdminSpecialtyListItemResponse.builder()
                .id(enriched.getId())
                .name(enriched.getName())
                .description(enriched.getDescription())
                .doctorCount(enriched.getDoctorCount())
                .isActive(enriched.getIsActive())
                .build();
    }

    private Boolean toActiveFilter(String status) {
        String normalized = trimToNull(status);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        if ("ACTIVE".equalsIgnoreCase(normalized)) {
            return Boolean.TRUE;
        }
        if ("INACTIVE".equalsIgnoreCase(normalized) || "DEACTIVATED".equalsIgnoreCase(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Sort parseSort(String sort) {
        Sort defaultSort = Sort.by(Sort.Direction.ASC, "name");
        if (sort == null || sort.isBlank()) {
            return defaultSort;
        }
        String normalizedSort = sort.trim().toLowerCase();
        if ("name_asc".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.ASC, "name");
        }
        if ("name_desc".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.DESC, "name");
        }
        if ("newest".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("oldest".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        return defaultSort;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toLikePattern(String keyword) {
        return keyword == null ? null : "%" + keyword + "%";
    }

    private record RelatedDataCounts(long doctorCount, long appointmentCount, long medicalRecordCount) {
        private boolean canDelete() {
            return doctorCount == 0 && appointmentCount == 0 && medicalRecordCount == 0;
        }
    }
}
