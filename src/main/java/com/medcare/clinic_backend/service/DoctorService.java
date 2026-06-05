package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.DoctorPhoto;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.dto.DoctorResponse;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorPhotoRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import com.medcare.clinic_backend.repository.FeedbackRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import com.medcare.clinic_backend.repository.MedicalServiceRepository;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import com.medcare.clinic_backend.repository.SpecialtyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorPhotoRepository doctorPhotoRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private PrescriptionDetailRepository prescriptionDetailRepository;
    @Autowired
    private ServiceDetailRepository serviceDetailRepository;
    @Autowired
    private MedicalServiceRepository medicalServiceRepository;
    @Value("${app.doctor-photo.max-size-bytes:2097152}")
    private long maxDoctorPhotoSizeBytes;

    private static final Set<String> ALLOWED_PHOTO_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public List<Doctor> getAllDoctors() {
        return getAllDoctors(null, null, true);
    }

    public List<Doctor> getAllDoctors(Integer specialtyId) {
        return getAllDoctors(specialtyId, null, true);
    }

    public List<Doctor> getAllDoctors(Integer specialtyId, String name) {
        return getAllDoctors(specialtyId, name, true);
    }

    public List<Doctor> getAllDoctors(Integer specialtyId, String name, boolean includeInactive) {
        String normalizedName = normalizeText(name);
        if (specialtyId == null) {
            if (normalizedName == null) {
                return includeInactive ? doctorRepository.findAll() : doctorRepository.findAll().stream()
                        .filter(doctor -> Boolean.TRUE.equals(doctor.getIsActive()))
                        .toList();
            }
            return includeInactive
                    ? doctorRepository.findByFullNameContainingIgnoreCase(normalizedName)
                    : doctorRepository.findByFullNameContainingIgnoreCaseAndIsActiveTrue(normalizedName);
        }
        if (specialtyId <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "specialtyId phai la so duong.");
        }
        if (normalizedName == null) {
            return includeInactive
                    ? doctorRepository.findBySpecialty_Id(specialtyId)
                    : doctorRepository.findBySpecialty_IdAndIsActiveTrue(specialtyId);
        }
        return includeInactive
                ? doctorRepository.findBySpecialty_IdAndFullNameContainingIgnoreCase(specialtyId, normalizedName)
                : doctorRepository.findBySpecialty_IdAndFullNameContainingIgnoreCaseAndIsActiveTrue(specialtyId, normalizedName);
    }

    public List<DoctorResponse> getAllDoctorResponses(Integer specialtyId) {
        return getAllDoctorResponses(specialtyId, null, true);
    }

    public List<DoctorResponse> getAllDoctorResponses(Integer specialtyId, String name) {
        return getAllDoctorResponses(specialtyId, name, true);
    }

    public List<DoctorResponse> getAllDoctorResponses(Integer specialtyId, String name, boolean includeInactive) {
        return getAllDoctors(specialtyId, name, includeInactive).stream()
                .map(this::toDoctorResponse)
                .toList();
    }

    public Doctor getDoctorById(Integer id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + id));
    }

    public DoctorResponse getDoctorResponseById(Integer id) {
        return toDoctorResponse(getDoctorById(id));
    }

    public Doctor getDoctorByAccountUsername(String username) {
        return doctorRepository.findByAccount_Username(username)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan doctor chua duoc lien ket voi ho so bac si."
                ));
    }

    public DoctorResponse getDoctorResponseByAccountUsername(String username) {
        return toDoctorResponse(getDoctorByAccountUsername(username));
    }

    public String getDisplayNameByUsername(String username) {
        return getDoctorByAccountUsername(username).getFullName();
    }

    @Transactional
    public Doctor createDoctor(Doctor doctor) {
        validateDoctorInput(doctor);
        validateDoctorEmailAvailable(doctor.getEmail(), null);
        doctor.setSpecialty(resolveSpecialty(doctor));
        Account resolvedAccount = resolveAccountForCreate(doctor);
        doctor.setAccount(resolvedAccount);
        doctor.setRating(resolveRatingForCreate(doctor.getRating()));
        doctor.setIsActive(resolveIsActiveForCreate(doctor.getIsActive(), doctor.getStatus()));
        doctor.setExperienceYears(resolveExperienceYearsForCreate(doctor.getExperienceYears()));
        if (doctor.getCreatedAt() == null) {
            doctor.setCreatedAt(LocalDateTime.now());
        }
        return doctorRepository.save(doctor);
    }

    public DoctorResponse toDoctorResponse(Doctor doctor) {
        if (doctor == null) {
            return null;
        }

        DoctorResponse response = new DoctorResponse();
        response.setId(doctor.getId());

        String safeFullName = safeText(doctor.getFullName());
        response.setFullName(safeFullName);
        response.setName(safeFullName);
        response.setEmail(safeText(doctor.getEmail()));
        response.setPhone(safeText(doctor.getPhone()));
        response.setPrice(doctor.getPrice());
        response.setRating(doctor.getRating() == null ? 0.0 : doctor.getRating());
        response.setActive(Boolean.TRUE.equals(doctor.getIsActive()));
        response.setStatus(Boolean.TRUE.equals(doctor.getIsActive()) ? "ACTIVE" : "INACTIVE");
        int normalizedExperienceYears = doctor.getExperienceYears() == null ? 0 : doctor.getExperienceYears();
        response.setExperienceYears(normalizedExperienceYears);
        response.setExperience(normalizedExperienceYears);
        applyPhotoFields(response, doctor);

        DoctorResponse.SpecialtySummary specialtySummary = new DoctorResponse.SpecialtySummary();
        if (doctor.getSpecialty() != null) {
            specialtySummary.setId(doctor.getSpecialty().getId());
            specialtySummary.setName(safeText(doctor.getSpecialty().getName()));
            specialtySummary.setDescription(safeText(doctor.getSpecialty().getDescription()));
            specialtySummary.setCreatedAt(doctor.getSpecialty().getCreatedAt());
            response.setSpecialtyId(doctor.getSpecialty().getId());
            response.setSpecialtyName(safeText(doctor.getSpecialty().getName()));
            response.setSpecialization(safeText(doctor.getSpecialty().getName()));
        } else {
            specialtySummary.setId(null);
            specialtySummary.setName("");
            specialtySummary.setDescription("");
            specialtySummary.setCreatedAt(null);
            response.setSpecialtyId(null);
            response.setSpecialtyName("");
            response.setSpecialization("");
        }
        response.setSpecialty(specialtySummary);

        DoctorResponse.AccountSummary accountSummary = new DoctorResponse.AccountSummary();
        if (doctor.getAccount() != null) {
            accountSummary.setId(doctor.getAccount().getId());
            accountSummary.setUsername(safeText(doctor.getAccount().getUsername()));
            accountSummary.setRole(safeText(doctor.getAccount().getRole()));
            response.setAccountId(doctor.getAccount().getId());
            response.setUsername(safeText(doctor.getAccount().getUsername()));
        } else {
            accountSummary.setId(null);
            accountSummary.setUsername("");
            accountSummary.setRole("");
            response.setAccountId(null);
            response.setUsername("");
        }
        response.setAccount(accountSummary);

        return response;
    }

    @Transactional
    public DoctorResponse uploadOwnPhoto(String username, MultipartFile file) {
        Doctor doctor = getDoctorByAccountUsername(username);
        return toDoctorResponse(saveDoctorPhoto(doctor, file));
    }

    @Transactional
    public DoctorResponse uploadDoctorPhoto(Integer doctorId, MultipartFile file) {
        Doctor doctor = getDoctorById(doctorId);
        return toDoctorResponse(saveDoctorPhoto(doctor, file));
    }

    public DoctorPhoto getDoctorPhoto(Integer doctorId) {
        return doctorPhotoRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Bac si nay chua co anh dai dien."));
    }

    @Transactional
    public void deleteOwnPhoto(String username) {
        Doctor doctor = getDoctorByAccountUsername(username);
        deleteDoctorPhoto(doctor.getId());
    }

    @Transactional
    public void deleteDoctorPhoto(Integer doctorId) {
        getDoctorById(doctorId);
        if (!doctorPhotoRepository.existsByDoctorId(doctorId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Bac si nay chua co anh dai dien.");
        }
        doctorPhotoRepository.deleteByDoctorId(doctorId);
    }

    @Transactional
    public Doctor updateDoctor(Integer id, Doctor doctorDetails) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + id));

        validateDoctorInput(doctorDetails);
        validateDoctorEmailAvailable(doctorDetails.getEmail(), id);

        doctor.setFullName(doctorDetails.getFullName());
        doctor.setEmail(doctorDetails.getEmail());
        doctor.setPhone(doctorDetails.getPhone());
        doctor.setPrice(doctorDetails.getPrice());
        doctor.setRating(resolveRatingForUpdate(doctorDetails.getRating(), doctor.getRating()));
        doctor.setIsActive(resolveIsActiveForUpdate(
                doctorDetails.getIsActive(),
                doctorDetails.getStatus(),
                doctor.getIsActive()
        ));
        doctor.setExperienceYears(resolveExperienceYearsForUpdate(
                doctorDetails.getExperienceYears(),
                doctor.getExperienceYears()
        ));
        doctor.setSpecialty(resolveSpecialty(doctorDetails));
        doctor.setAccount(resolveAccountForUpdate(doctorDetails, doctor.getAccount()));
        return doctorRepository.save(doctor);
    }

    @Transactional
    public void deleteDoctor(Integer id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + id));
        LocalDateTime now = LocalDateTime.now();
        long activeAppointmentCount = appointmentRepository.countUpcomingOpenAppointmentsByDoctorId(id, now);
        long activeFollowUpCount = appointmentRepository.countUpcomingOpenFollowUpAppointmentsByDoctorId(id, now);
        if (activeAppointmentCount > 0) {
            throw buildDoctorDeleteActiveAppointmentException(activeAppointmentCount, activeFollowUpCount);
        }
        Account linkedAccount = doctor.getAccount();
        try {
            medicalServiceRepository.clearAssignedDoctorByDoctorId(id);
            doctorPhotoRepository.deleteByDoctorId(id);
            doctorScheduleRepository.deleteByDoctorId(id);
            List<Integer> recordIds = medicalRecordRepository.findByDoctorIdOrderByExaminationDateDesc(id).stream()
                    .map(MedicalRecord::getId)
                    .filter(recordId -> recordId != null)
                    .toList();
            if (!recordIds.isEmpty()) {
                invoiceRepository.deleteByMedicalRecordIdIn(recordIds);
                prescriptionDetailRepository.deleteByMedicalRecordIdIn(recordIds);
                serviceDetailRepository.deleteByMedicalRecordIdIn(recordIds);
                medicalRecordRepository.deleteByDoctorId(id);
            }
            feedbackRepository.deleteByDoctorId(id);
            List<Integer> appointmentIds = appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(id).stream()
                    .map(Appointment::getId)
                    .filter(appointmentId -> appointmentId != null)
                    .toList();
            if (!appointmentIds.isEmpty()) {
                appointmentRepository.clearParentAppointmentByDoctorId(id);
                appointmentRepository.deleteByDoctorId(id);
            }
            doctorRepository.delete(doctor);
            doctorRepository.flush();
            if (shouldDeleteLinkedDoctorAccount(linkedAccount)) {
                accountRepository.delete(linkedAccount);
                accountRepository.flush();
            }
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Khong the xoa bac si nay vi van con du lieu lien quan.",
                    "DOCTOR_DELETE_BLOCKED"
            );
        }
    }
    @Transactional
    public Doctor updateDoctorActiveStatus(Integer id, Boolean active) {
        if (active == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Trang thai active khong duoc de trong.");
        }
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + id));
        doctor.setIsActive(active);
        return doctorRepository.save(doctor);
    }

    private void validateDoctorInput(Doctor doctor) {
        if (doctor == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu bac si khong hop le.");
        }
        if (doctor.getFullName() == null || doctor.getFullName().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ten bac si khong duoc de trong.");
        }
        if (doctor.getEmail() == null || doctor.getEmail().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email bac si khong duoc de trong.");
        }
        BigDecimal price = doctor.getPrice();
        if (price != null && price.signum() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Gia kham khong duoc am.");
        }
        Double rating = doctor.getRating();
        if (rating != null) {
            if (!Double.isFinite(rating)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Danh gia bac si khong hop le.");
            }
            if (rating < 0 || rating > 5) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Danh gia bac si phai nam trong khoang 0 den 5.");
            }
        }
        Integer experienceYears = doctor.getExperienceYears();
        if (experienceYears != null && experienceYears < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So nam kinh nghiem khong duoc am.");
        }
        if (resolveSpecialtyId(doctor) == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si phai thuoc mot chuyen khoa.");
        }
    }

    private void validateDoctorEmailAvailable(String email, Integer currentDoctorId) {
        String normalizedEmail = normalizeText(email);
        if (normalizedEmail == null) {
            return;
        }

        boolean exists = currentDoctorId == null
                ? doctorRepository.existsByEmail(normalizedEmail)
                : doctorRepository.existsByEmailAndIdNot(normalizedEmail, currentDoctorId);
        if (exists) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email bac si da ton tai.");
        }
    }

    private Specialty resolveSpecialty(Doctor doctor) {
        Integer specialtyId = resolveSpecialtyId(doctor);
        return specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay chuyen khoa ID: " + specialtyId));
    }

    private Integer resolveSpecialtyId(Doctor doctor) {
        if (doctor == null) {
            return null;
        }
        if (doctor.getSpecialty() != null && doctor.getSpecialty().getId() != null) {
            return doctor.getSpecialty().getId();
        }
        return doctor.getSpecialtyId();
    }

    private Double resolveRatingForCreate(Double requestedRating) {
        return requestedRating == null ? 0.0 : requestedRating;
    }

    private Integer resolveExperienceYearsForCreate(Integer requestedExperienceYears) {
        return requestedExperienceYears == null ? 0 : requestedExperienceYears;
    }

    private Boolean resolveIsActiveForCreate(Boolean requestedIsActive, String requestedStatus) {
        if (requestedIsActive != null) {
            return requestedIsActive;
        }
        Boolean parsedFromStatus = parseActiveFromStatus(requestedStatus);
        if (parsedFromStatus != null) {
            return parsedFromStatus;
        }
        return Boolean.TRUE;
    }

    private Double resolveRatingForUpdate(Double requestedRating, Double currentRating) {
        if (requestedRating != null) {
            return requestedRating;
        }
        return currentRating == null ? 0.0 : currentRating;
    }

    private Integer resolveExperienceYearsForUpdate(Integer requestedExperienceYears, Integer currentExperienceYears) {
        if (requestedExperienceYears != null) {
            return requestedExperienceYears;
        }
        return currentExperienceYears == null ? 0 : currentExperienceYears;
    }

    private Boolean resolveIsActiveForUpdate(Boolean requestedIsActive, String requestedStatus, Boolean currentIsActive) {
        if (requestedIsActive != null) {
            return requestedIsActive;
        }
        Boolean parsedFromStatus = parseActiveFromStatus(requestedStatus);
        if (parsedFromStatus != null) {
            return parsedFromStatus;
        }
        return currentIsActive == null ? Boolean.TRUE : currentIsActive;
    }

    private Boolean parseActiveFromStatus(String status) {
        String normalized = normalizeText(status);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase();
        if ("ACTIVE".equals(upper) || "HOAT_DONG".equals(upper) || "HOAT DONG".equals(upper)) {
            return Boolean.TRUE;
        }
        if ("INACTIVE".equals(upper)
                || "KHONG_HOAT_DONG".equals(upper)
                || "KHONG HOAT DONG".equals(upper)
                || "TAM_NGUNG".equals(upper)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Account resolveAccountForCreate(Doctor doctor) {
        Account inputAccount = doctor.getAccount();
        if (inputAccount == null) {
            inputAccount = new Account();
            inputAccount.setUsername(doctor.getUsername());
            inputAccount.setPassword(doctor.getPassword());
        }

        if (inputAccount.getId() != null) {
            Account persistedAccount = resolveDoctorAccountById(inputAccount.getId());
            if (doctorRepository.existsByAccountId(persistedAccount.getId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Account nay da duoc lien ket voi mot bac si khac.");
            }
            return persistedAccount;
        }

        String username = normalizeText(inputAccount.getUsername());
        String rawPassword = inputAccount.getPassword();
        if (username == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Username bac si khong duoc de trong.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Mat khau bac si khong duoc de trong.");
        }
        if (accountRepository.existsByUsername(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Username da ton tai tren he thong.");
        }

        Account newAccount = new Account();
        newAccount.setUsername(username);
        newAccount.setPassword(passwordEncoder.encode(rawPassword));
        newAccount.setRole("ROLE_DOCTOR");
        return accountRepository.save(newAccount);
    }

    private Account resolveAccountForUpdate(Doctor doctorDetails, Account currentAccount) {
        Account requestedAccount = doctorDetails.getAccount();
        if (requestedAccount == null) {
            String username = normalizeText(doctorDetails.getUsername());
            String rawPassword = doctorDetails.getPassword();
            if (username == null && (rawPassword == null || rawPassword.isBlank())) {
                return currentAccount;
            }
            requestedAccount = new Account();
            requestedAccount.setUsername(username);
            requestedAccount.setPassword(rawPassword);
        }

        if (requestedAccount.getId() != null) {
            Account persistedAccount = resolveDoctorAccountById(requestedAccount.getId());
            if (doctorRepository.existsByAccountId(persistedAccount.getId())
                    && (currentAccount == null || !persistedAccount.getId().equals(currentAccount.getId()))) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Account nay da duoc lien ket voi mot bac si khac.");
            }
            return persistedAccount;
        }

        String username = normalizeText(requestedAccount.getUsername());
        String rawPassword = requestedAccount.getPassword();
        if (username == null || rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Neu tao account moi khi cap nhat bac si, vui long cung cap day du username va password."
            );
        }
        if (accountRepository.existsByUsername(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Username da ton tai tren he thong.");
        }

        Account newAccount = new Account();
        newAccount.setUsername(username);
        newAccount.setPassword(passwordEncoder.encode(rawPassword));
        newAccount.setRole("ROLE_DOCTOR");
        return accountRepository.save(newAccount);
    }

    private Account resolveDoctorAccountById(Integer accountId) {
        Account persistedAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Khong tim thay account ID: " + accountId));

        String normalizedRole = normalizeText(persistedAccount.getRole());
        if (normalizedRole == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Account duoc gan cho bac si phai co ROLE_DOCTOR.");
        }
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }
        if (!"ROLE_DOCTOR".equalsIgnoreCase(normalizedRole)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Account duoc gan cho bac si phai co ROLE_DOCTOR.");
        }
        return persistedAccount;
    }

    private Doctor saveDoctorPhoto(Doctor doctor, MultipartFile file) {
        validateDoctorPhoto(file);

        DoctorPhoto photo = doctorPhotoRepository.findByDoctorId(doctor.getId())
                .orElseGet(() -> {
                    DoctorPhoto newPhoto = new DoctorPhoto();
                    newPhoto.setDoctor(doctor);
                    return newPhoto;
                });

        try {
            photo.setFileName(resolveFileName(file.getOriginalFilename()));
            photo.setContentType(file.getContentType());
            photo.setFileSize(file.getSize());
            photo.setData(file.getBytes());
            photo.setUploadedAt(LocalDateTime.now());
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong doc duoc file anh bac si.");
        }

        doctorPhotoRepository.save(photo);
        doctor.setAvatarUrl("/api/doctors/" + doctor.getId() + "/photo");
        doctorRepository.save(doctor);
        return doctor;
    }

    private void validateDoctorPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui long chon file anh bac si.");
        }
        if (file.getSize() > maxDoctorPhotoSizeBytes) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Anh bac si khong duoc vuot qua 2MB.");
        }
        String contentType = normalizeText(file.getContentType());
        if (contentType == null || !ALLOWED_PHOTO_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Anh bac si chi ho tro JPEG, PNG hoac WEBP.");
        }
    }

    private String resolveFileName(String originalFileName) {
        String normalized = normalizeText(originalFileName);
        return normalized == null ? "doctor-photo" : normalized;
    }

    private void applyPhotoFields(DoctorResponse response, Doctor doctor) {
        if (doctor.getId() == null) {
            response.setPhotoId(null);
            response.setPhotoUrl(null);
            response.setImageUrl(null);
            return;
        }

        Integer photoId = doctorPhotoRepository.findIdByDoctorId(doctor.getId()).orElse(null);
        if (photoId == null) {
            response.setPhotoId(null);
            response.setPhotoUrl(null);
            response.setImageUrl(null);
            return;
        }

        String photoUrl = "/api/doctors/" + doctor.getId() + "/photo";
        response.setPhotoId(photoId);
        response.setPhotoUrl(photoUrl);
        response.setImageUrl(photoUrl);
    }

    private BusinessException buildDoctorDeleteActiveAppointmentException(
            long activeAppointmentCount,
            long activeFollowUpCount
    ) {
        long activeRegularAppointmentCount = Math.max(0L, activeAppointmentCount - activeFollowUpCount);
        String message;
        if (activeRegularAppointmentCount > 0 && activeFollowUpCount > 0) {
            message = "Khong the xoa bac si vi dang co lich hen va lich tai kham. Vui long cho den khi het lich.";
        } else if (activeFollowUpCount > 0) {
            message = "Khong the xoa bac si vi dang co lich tai kham. Vui long cho den khi het lich.";
        } else {
            message = "Khong the xoa bac si vi dang co lich hen. Vui long cho den khi het lich.";
        }
        return new BusinessException(HttpStatus.CONFLICT, message, "DOCTOR_DELETE_HAS_ACTIVE_APPOINTMENTS");
    }
    private boolean shouldDeleteLinkedDoctorAccount(Account account) {
        if (account == null) {
            return false;
        }
        String normalizedRole = normalizeText(account.getRole());
        if (normalizedRole == null) {
            return false;
        }
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }
        return "ROLE_DOCTOR".equalsIgnoreCase(normalizedRole);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}

