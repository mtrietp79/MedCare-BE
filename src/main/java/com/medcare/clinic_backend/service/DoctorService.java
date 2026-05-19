package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.dto.DoctorResponse;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Doctor> getAllDoctors() {
        return getAllDoctors(null);
    }

    public List<Doctor> getAllDoctors(Integer specialtyId) {
        if (specialtyId == null) {
            return doctorRepository.findAll();
        }
        if (specialtyId <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "specialtyId phai la so duong.");
        }
        return doctorRepository.findBySpecialty_Id(specialtyId);
    }

    public List<DoctorResponse> getAllDoctorResponses(Integer specialtyId) {
        return getAllDoctors(specialtyId).stream()
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
        Account resolvedAccount = resolveAccountForCreate(doctor.getAccount());
        doctor.setAccount(resolvedAccount);
        doctor.setRating(resolveRatingForCreate(doctor.getRating()));
        doctor.setExperienceYears(resolveExperienceYearsForCreate(doctor.getExperienceYears()));
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
        int normalizedExperienceYears = doctor.getExperienceYears() == null ? 0 : doctor.getExperienceYears();
        response.setExperienceYears(normalizedExperienceYears);
        response.setExperience(normalizedExperienceYears);

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
    public Doctor updateDoctor(Integer id, Doctor doctorDetails) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + id));

        validateDoctorInput(doctorDetails);

        doctor.setFullName(doctorDetails.getFullName());
        doctor.setEmail(doctorDetails.getEmail());
        doctor.setPhone(doctorDetails.getPhone());
        doctor.setPrice(doctorDetails.getPrice());
        doctor.setRating(resolveRatingForUpdate(doctorDetails.getRating(), doctor.getRating()));
        doctor.setExperienceYears(resolveExperienceYearsForUpdate(
                doctorDetails.getExperienceYears(),
                doctor.getExperienceYears()
        ));
        doctor.setSpecialty(doctorDetails.getSpecialty());
        doctor.setAccount(resolveAccountForUpdate(doctorDetails.getAccount(), doctor.getAccount()));
        return doctorRepository.save(doctor);
    }

    public void deleteDoctor(Integer id) {
        if (!doctorRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + id);
        }
        doctorRepository.deleteById(id);
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
        if (doctor.getSpecialty() == null || doctor.getSpecialty().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si phai thuoc mot chuyen khoa.");
        }
    }

    private Double resolveRatingForCreate(Double requestedRating) {
        return requestedRating == null ? 0.0 : requestedRating;
    }

    private Integer resolveExperienceYearsForCreate(Integer requestedExperienceYears) {
        return requestedExperienceYears == null ? 0 : requestedExperienceYears;
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

    private Account resolveAccountForCreate(Account inputAccount) {
        if (inputAccount == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui long cung cap account cho bac si moi.");
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

    private Account resolveAccountForUpdate(Account requestedAccount, Account currentAccount) {
        if (requestedAccount == null) {
            return currentAccount;
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
