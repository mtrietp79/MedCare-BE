package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.MedicalService;
import com.medcare.clinic_backend.entity.MedicalServicePhoto;
import com.medcare.clinic_backend.entity.MedicalServicePrescriptionItem;
import com.medcare.clinic_backend.entity.Medicine;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.MedicalServicePhotoRepository;
import com.medcare.clinic_backend.repository.MedicalServiceRepository;
import com.medcare.clinic_backend.repository.MedicineRepository;
import com.medcare.clinic_backend.repository.SpecialtyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class MedicalServiceService {
    @Autowired
    private MedicalServiceRepository repository;

    @Autowired
    private MedicalServicePhotoRepository photoRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Value("${app.medical-service-photo.max-size-bytes:2097152}")
    private long maxServicePhotoSizeBytes;

    private static final Set<String> ALLOWED_PHOTO_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Transactional(readOnly = true)
    public List<MedicalService> getActiveServices(Integer specialtyId) {
        return getActiveServices(specialtyId, null);
    }

    @Transactional(readOnly = true)
    public List<MedicalService> getActiveServices(Integer specialtyId, String keyword) {
        String normalizedKeyword = normalizeText(keyword);
        List<MedicalService> services;
        if (specialtyId != null && normalizedKeyword != null) {
            services = repository.findBySpecialty_IdAndActiveTrueAndNameContainingIgnoreCaseOrderByIdDesc(specialtyId, normalizedKeyword);
        } else if (specialtyId != null) {
            services = repository.findBySpecialty_IdAndActiveTrueOrderByIdDesc(specialtyId);
        } else if (normalizedKeyword != null) {
            services = repository.findByActiveTrueAndNameContainingIgnoreCaseOrderByIdDesc(normalizedKeyword);
        } else {
            services = repository.findByActiveTrueOrderByIdDesc();
        }
        return applyPhotoFields(services);
    }

    @Transactional(readOnly = true)
    public List<MedicalService> getAllForAdmin(Integer specialtyId) {
        return getAllForAdmin(specialtyId, null);
    }

    @Transactional(readOnly = true)
    public List<MedicalService> getAllForAdmin(Integer specialtyId, String keyword) {
        String normalizedKeyword = normalizeText(keyword);
        List<MedicalService> services;
        if (specialtyId != null && normalizedKeyword != null) {
            services = repository.findBySpecialty_IdAndNameContainingIgnoreCaseOrderByIdDesc(specialtyId, normalizedKeyword);
        } else if (specialtyId != null) {
            services = repository.findBySpecialty_IdOrderByIdDesc(specialtyId);
        } else if (normalizedKeyword != null) {
            services = repository.findByNameContainingIgnoreCaseOrderByIdDesc(normalizedKeyword);
        } else {
            services = repository.findAll();
        }
        return applyPhotoFields(services);
    }

    @Transactional(readOnly = true)
    public MedicalService getById(Integer id) {
        return applyPhotoFields(repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay goi dich vu ID: " + id)));
    }

    @Transactional
    public MedicalService create(MedicalService medicalService) {
        validateService(medicalService);
        attachReferences(medicalService);
        return applyPhotoFields(repository.save(medicalService));
    }

    @Transactional
    public MedicalService update(Integer id, MedicalService details) {
        MedicalService existing = getById(id);
        validateService(details);

        existing.setName(details.getName());
        existing.setDescription(details.getDescription());
        existing.setPrice(details.getPrice());
        existing.setActive(resolveActive(details, existing.getActive()));
        existing.setAdvertised(details.getAdvertised() == null ? existing.getAdvertised() : details.getAdvertised());
        existing.setSpecialty(resolveSpecialty(details));
        existing.setAssignedDoctor(resolveAssignedDoctor(details, existing.getSpecialty()));

        existing.getPrescriptionItems().clear();
        existing.getPrescriptionItems().addAll(resolvePrescriptionItems(existing, details.getPrescriptionItems()));

        return applyPhotoFields(repository.save(existing));
    }

    @Transactional
    public MedicalService setActive(Integer id, boolean active) {
        MedicalService existing = getById(id);
        existing.setActive(active);
        return applyPhotoFields(repository.save(existing));
    }

    @Transactional(readOnly = true)
    public MedicalService getActiveByIdForBooking(Integer id) {
        MedicalService service = getById(id);
        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu da dung hoat dong.");
        }
        return service;
    }

    @Transactional
    public MedicalService uploadPhoto(Integer serviceId, MultipartFile file) {
        MedicalService medicalService = getById(serviceId);
        validatePhoto(file);

        MedicalServicePhoto photo = photoRepository.findByMedicalServiceId(serviceId)
                .orElseGet(() -> {
                    MedicalServicePhoto newPhoto = new MedicalServicePhoto();
                    newPhoto.setMedicalService(medicalService);
                    return newPhoto;
                });

        try {
            photo.setFileName(resolveFileName(file.getOriginalFilename()));
            photo.setContentType(file.getContentType());
            photo.setFileSize(file.getSize());
            photo.setData(file.getBytes());
            photo.setUploadedAt(LocalDateTime.now());
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong doc duoc file anh goi dich vu.");
        }

        photoRepository.save(photo);
        return applyPhotoFields(medicalService);
    }

    public MedicalServicePhoto getPhoto(Integer serviceId) {
        return photoRepository.findByMedicalServiceId(serviceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Goi dich vu nay chua co anh."));
    }

    @Transactional
    public void deletePhoto(Integer serviceId) {
        getById(serviceId);
        if (!photoRepository.existsByMedicalServiceId(serviceId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Goi dich vu nay chua co anh.");
        }
        photoRepository.deleteByMedicalServiceId(serviceId);
    }

    private void validateService(MedicalService medicalService) {
        if (medicalService == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu goi dich vu khong hop le.");
        }
        if (medicalService.getName() == null || medicalService.getName().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ten goi dich vu khong duoc de trong.");
        }
        if (medicalService.getPrice() == null || medicalService.getPrice() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Gia tien goi dich vu phai lon hon hoac bang 0.");
        }
        if (resolveSpecialtyId(medicalService) == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu phai thuoc mot chuyen khoa.");
        }
    }

    private void attachReferences(MedicalService medicalService) {
        medicalService.setSpecialty(resolveSpecialty(medicalService));
        medicalService.setAssignedDoctor(resolveAssignedDoctor(medicalService, medicalService.getSpecialty()));
        medicalService.setActive(resolveActive(medicalService, true));
        medicalService.setAdvertised(medicalService.getAdvertised() == null ? false : medicalService.getAdvertised());
        medicalService.setPrescriptionItems(resolvePrescriptionItems(medicalService, medicalService.getPrescriptionItems()));
    }

    private Boolean resolveActive(MedicalService medicalService, Boolean defaultValue) {
        String status = normalizeText(medicalService.getStatus());
        if (status != null) {
            String normalizedStatus = status
                    .toLowerCase()
                    .replace(" ", "")
                    .replace("_", "")
                    .replace("-", "");
            if (normalizedStatus.equals("active")
                    || normalizedStatus.equals("hoatdong")
                    || normalizedStatus.equals("hoạtđộng")) {
                return true;
            }
            if (normalizedStatus.equals("inactive")
                    || normalizedStatus.equals("disabled")
                    || normalizedStatus.equals("stopped")
                    || normalizedStatus.equals("dunghoatdong")
                    || normalizedStatus.equals("dừnghoạtđộng")) {
                return false;
            }
        }
        return medicalService.getActive() == null ? defaultValue : medicalService.getActive();
    }

    private Specialty resolveSpecialty(MedicalService medicalService) {
        Integer specialtyId = resolveSpecialtyId(medicalService);
        return specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay chuyen khoa ID: " + specialtyId));
    }

    private Doctor resolveAssignedDoctor(MedicalService medicalService, Specialty specialty) {
        Integer doctorId = resolveAssignedDoctorId(medicalService);
        if (doctorId == null) {
            return null;
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + doctorId));

        Integer specialtyId = specialty == null ? null : specialty.getId();
        Integer doctorSpecialtyId = doctor.getSpecialty() == null ? null : doctor.getSpecialty().getId();
        if (specialtyId == null || doctorSpecialtyId == null || !specialtyId.equals(doctorSpecialtyId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si dam nhan phai thuoc chuyen khoa cua goi dich vu.");
        }

        return doctor;
    }

    private Integer resolveSpecialtyId(MedicalService medicalService) {
        if (medicalService == null) {
            return null;
        }
        if (medicalService.getSpecialty() != null && medicalService.getSpecialty().getId() != null) {
            return medicalService.getSpecialty().getId();
        }
        return medicalService.getSpecialtyId();
    }

    private Integer resolveAssignedDoctorId(MedicalService medicalService) {
        if (medicalService == null) {
            return null;
        }
        if (medicalService.getAssignedDoctor() != null && medicalService.getAssignedDoctor().getId() != null) {
            return medicalService.getAssignedDoctor().getId();
        }
        return medicalService.getAssignedDoctorId();
    }

    private List<MedicalServicePrescriptionItem> resolvePrescriptionItems(
            MedicalService medicalService,
            List<MedicalServicePrescriptionItem> requestedItems
    ) {
        List<MedicalServicePrescriptionItem> resolvedItems = new ArrayList<>();
        if (requestedItems == null || requestedItems.isEmpty()) {
            return resolvedItems;
        }

        for (MedicalServicePrescriptionItem item : requestedItems) {
            Integer medicineId = resolveMedicineId(item);
            if (medicineId == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Thuoc trong don mau phai co medicineId.");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "So luong thuoc trong don mau phai lon hon 0.");
            }

            Medicine medicine = medicineRepository.findById(medicineId)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "Khong tim thay thuoc ID: " + medicineId
                    ));

            MedicalServicePrescriptionItem resolvedItem = new MedicalServicePrescriptionItem();
            resolvedItem.setMedicalService(medicalService);
            resolvedItem.setMedicine(medicine);
            resolvedItem.setQuantity(item.getQuantity());
            resolvedItem.setDosage(item.getDosage());
            resolvedItems.add(resolvedItem);
        }

        return resolvedItems;
    }

    private Integer resolveMedicineId(MedicalServicePrescriptionItem item) {
        if (item == null) {
            return null;
        }
        if (item.getMedicine() != null && item.getMedicine().getId() != null) {
            return item.getMedicine().getId();
        }
        return item.getMedicineId();
    }

    private void validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui long chon file anh goi dich vu.");
        }
        if (file.getSize() > maxServicePhotoSizeBytes) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Anh goi dich vu khong duoc vuot qua 2MB.");
        }
        String contentType = normalizeText(file.getContentType());
        if (contentType == null || !ALLOWED_PHOTO_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Anh goi dich vu chi ho tro JPEG, PNG hoac WEBP.");
        }
    }

    private String resolveFileName(String originalFileName) {
        String normalized = normalizeText(originalFileName);
        return normalized == null ? "medical-service-photo" : normalized;
    }

    private List<MedicalService> applyPhotoFields(List<MedicalService> services) {
        services.forEach(this::applyPhotoFields);
        return services;
    }

    private MedicalService applyPhotoFields(MedicalService medicalService) {
        if (medicalService == null || medicalService.getId() == null) {
            return medicalService;
        }

        if (photoRepository.findIdByMedicalServiceId(medicalService.getId()).isPresent()) {
            medicalService.setImageUrl("/api/medical-services/" + medicalService.getId() + "/photo");
        } else {
            medicalService.setImageUrl(null);
        }
        if (medicalService.getPrescriptionItems() != null) {
            medicalService.getPrescriptionItems().size();
        }
        return medicalService;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
