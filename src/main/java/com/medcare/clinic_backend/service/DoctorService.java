package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return doctorRepository.findAll();
    }

    public Doctor getDoctorById(Integer id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + id));
    }

    public Doctor getDoctorByAccountUsername(String username) {
        return doctorRepository.findByAccount_Username(username)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan doctor chua duoc lien ket voi ho so bac si."
                ));
    }

    @Transactional
    public Doctor createDoctor(Doctor doctor) {
        validateDoctorInput(doctor);
        Account resolvedAccount = resolveAccountForCreate(doctor.getAccount());
        doctor.setAccount(resolvedAccount);
        return doctorRepository.save(doctor);
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
        if (doctor.getSpecialty() == null || doctor.getSpecialty().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si phai thuoc mot chuyen khoa.");
        }
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
}
