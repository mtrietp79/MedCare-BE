package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.MedicalService;
import com.medcare.clinic_backend.entity.MedicalServicePhoto;
import com.medcare.clinic_backend.entity.MedicalServicePrescriptionItem;
import com.medcare.clinic_backend.entity.Medicine;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.exception.BusinessException;
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

    @Value("${app.medical-service-photo.max-size-bytes:2097152}")
    private long maxServicePhotoSizeBytes;

    private static final Set<String> ALLOWED_PHOTO_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public List<MedicalService> getActiveServices(Integer specialtyId) {
        List<MedicalService> services;
        if (specialtyId != null) {
            services = repository.findBySpecialtyIdAndActiveTrueOrderByIdDesc(specialtyId);
        } else {
            services = repository.findByActiveTrueOrderByIdDesc();
        }
        return applyPhotoFields(services);
    }

    public List<MedicalService> getAllForAdmin(Integer specialtyId) {
        List<MedicalService> services;
        if (specialtyId != null) {
            services = repository.findBySpecialtyIdOrderByIdDesc(specialtyId);
        } else {
            services = repository.findAll();
        }
        return applyPhotoFields(services);
    }

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
        existing.setActive(details.getActive() == null ? existing.getActive() : details.getActive());
        existing.setSpecialty(resolveSpecialty(details.getSpecialty()));

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
        if (medicalService.getSpecialty() == null || medicalService.getSpecialty().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu phai thuoc mot chuyen khoa.");
        }
    }

    private void attachReferences(MedicalService medicalService) {
        medicalService.setSpecialty(resolveSpecialty(medicalService.getSpecialty()));
        medicalService.setActive(medicalService.getActive() == null ? true : medicalService.getActive());
        medicalService.setPrescriptionItems(resolvePrescriptionItems(medicalService, medicalService.getPrescriptionItems()));
    }

    private Specialty resolveSpecialty(Specialty specialty) {
        return specialtyRepository.findById(specialty.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay chuyen khoa ID: " + specialty.getId()));
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
            if (item.getMedicine() == null || item.getMedicine().getId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Thuoc trong don mau phai co medicineId.");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "So luong thuoc trong don mau phai lon hon 0.");
            }

            Medicine medicine = medicineRepository.findById(item.getMedicine().getId())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "Khong tim thay thuoc ID: " + item.getMedicine().getId()
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

        if (photoRepository.existsByMedicalServiceId(medicalService.getId())) {
            medicalService.setImageUrl("/api/medical-services/" + medicalService.getId() + "/photo");
        } else {
            medicalService.setImageUrl(null);
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
