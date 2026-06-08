package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public List<Patient> getPatientsForDoctor(Integer doctorId) {
        return appointmentRepository.findDistinctPatientsByDoctorId(doctorId);
    }

    public Patient getPatientById(Integer id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay benh nhan ID: " + id));
    }

    public Patient getPatientByIdForDoctor(Integer id, Integer doctorId) {
        Patient patient = getPatientById(id);
        if (!appointmentRepository.existsByDoctorIdAndPatientId(doctorId, id)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bac si khong duoc xem ho so benh nhan nay.");
        }
        return patient;
    }

    public Patient getPatientByAccountUsername(String username) {
        return patientRepository.findByAccount_Username(username)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan cua ban chua duoc lien ket voi ho so benh nhan."
                ));
    }

    public Account findLinkedAccountByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return null;
        }

        long count = patientRepository.countByEmailIgnoreCase(normalizedEmail);
        if (count > 1) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Email duoc lien ket voi nhieu ho so benh nhan. Khong the tu dong lien ket tai khoan."
            );
        }

        return patientRepository.findFirstByEmailIgnoreCase(normalizedEmail)
                .map(Patient::getAccount)
                .orElse(null);
    }

    public Account findLinkedAccountByPhone(String phone) {
        String normalizedPhone = normalizeText(phone);
        if (normalizedPhone == null) {
            return null;
        }
        if (!normalizedPhone.matches("^(0|\\+84)\\d{9,10}$")) {
            return null;
        }

        Account linkedByExactPhone = patientRepository.findFirstByPhone(normalizedPhone)
                .map(Patient::getAccount)
                .orElse(null);
        if (linkedByExactPhone != null) {
            return linkedByExactPhone;
        }

        String alternatePhone = toAlternatePhoneFormat(normalizedPhone);
        if (alternatePhone == null) {
            return null;
        }
        return patientRepository.findFirstByPhone(alternatePhone)
                .map(Patient::getAccount)
                .orElse(null);
    }

    public boolean isProfileCompletedByUsername(String username) {
        Patient patient = getPatientByAccountUsername(username);
        return Boolean.TRUE.equals(patient.getProfileCompleted());
    }

    public String getDisplayNameByUsername(String username) {
        return getPatientByAccountUsername(username).getFullName();
    }

    public void ensureProfileCompleted(Integer patientId) {
        Patient patient = getPatientById(patientId);
        if (!Boolean.TRUE.equals(patient.getProfileCompleted())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Vui long cap nhat day du ho so ca nhan truoc khi dat lich.",
                    "PROFILE_INCOMPLETE"
            );
        }
    }

    public Patient createPatient(Patient patient) {
        validatePatientInput(patient, true, null);
        return patientRepository.save(patient);
    }

    public Patient updatePatient(Integer id, Patient patientDetails) {
        Patient patient = getPatientById(id);
        applyUpdatableFields(patient, patientDetails, true);
        return patientRepository.save(patient);
    }

    @Transactional
    public Patient updateOwnProfile(String username, Patient patientDetails) {
        Patient patient = getPatientByAccountUsername(username);
        applyUpdatableFields(patient, patientDetails, false);
        return patientRepository.save(patient);
    }

    public void deletePatient(Integer id) {
        if (!patientRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay benh nhan ID: " + id);
        }
        patientRepository.deleteById(id);
    }

    @Transactional
    public Patient createInitialProfileForAccount(Account account) {
        return createInitialProfileForAccount(account, null, null, null);
    }

    @Transactional
    public Patient createInitialProfileForAccount(Account account, String preferredFullName) {
        return createInitialProfileForAccount(account, preferredFullName, null, null);
    }

    @Transactional
    public Patient createInitialProfileForAccount(
            Account account,
            String preferredFullName,
            String preferredPhone,
            String preferredEmail
    ) {
        if (account == null || account.getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong the khoi tao ho so benh nhan khi chua co account.");
        }

        return patientRepository.findByAccount_Username(account.getUsername())
                .orElseGet(() -> {
                    Patient patient = new Patient();
                    String resolvedEmail = resolveBootstrapEmail(preferredEmail, account.getUsername());
                    String resolvedPhone = resolveBootstrapPhone(preferredPhone, account.getUsername());
                    ensureBootstrapIdentifiersAvailable(resolvedEmail, resolvedPhone);
                    patient.setAccount(account);
                    patient.setFullName(buildDefaultFullName(preferredFullName, account.getUsername()));
                    patient.setEmail(resolvedEmail);
                    patient.setPhone(resolvedPhone);
                    patient.setGender(null);
                    patient.setNationalId(null);
                    patient.setDateOfBirth(null);
                    patient.setAddress(null);
                    patient.setProfileCompleted(false);
                    return patientRepository.save(patient);
                });
    }

    private void applyUpdatableFields(Patient patient, Patient patientDetails, boolean adminUpdate) {
        validatePatientInput(patientDetails, !adminUpdate, patient.getId());

        patient.setFullName(normalizeText(patientDetails.getFullName()));
        patient.setDateOfBirth(patientDetails.getDateOfBirth());
        patient.setPhone(normalizePhone(patientDetails.getPhone()));
        patient.setGender(normalizeGender(patientDetails.getGender()));
        patient.setNationalId(normalizeNationalId(patientDetails.getNationalId()));
        patient.setAddress(normalizeText(patientDetails.getAddress()));
        patient.setEmail(normalizeEmail(patientDetails.getEmail()));
        patient.setProfileCompleted(isProfileComplete(patient));
    }

    private void validatePatientInput(Patient patient, boolean requireCompletedProfile, Integer currentPatientId) {
        if (patient == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu benh nhan khong hop le.");
        }

        String fullName = normalizeText(patient.getFullName());
        LocalDate dateOfBirth = patient.getDateOfBirth();
        String phone = normalizePhone(patient.getPhone());
        String gender = normalizeGender(patient.getGender());
        String nationalId = normalizeNationalId(patient.getNationalId());
        String address = normalizeText(patient.getAddress());
        String email = normalizeEmail(patient.getEmail());

        patient.setFullName(fullName);
        patient.setDateOfBirth(dateOfBirth);
        patient.setPhone(phone);
        patient.setGender(gender);
        patient.setNationalId(nationalId);
        patient.setAddress(address);
        patient.setEmail(email);

        if (fullName == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ho ten khong duoc de trong.");
        }

        if (phone != null && patientRepository.existsByPhoneAndIdNot(phone, currentPatientId == null ? -1 : currentPatientId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So dien thoai da duoc su dung.");
        }
        if (email != null && patientRepository.existsByEmailIgnoreCaseAndIdNot(email, currentPatientId == null ? -1 : currentPatientId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email này đã được sử dụng");
        }
        if (nationalId != null && patientRepository.existsByNationalIdAndIdNot(nationalId, currentPatientId == null ? -1 : currentPatientId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CCCD da duoc su dung.");
        }

        boolean completed = isProfileComplete(fullName, dateOfBirth, phone, gender, nationalId, address);
        patient.setProfileCompleted(completed);

        if (requireCompletedProfile && !completed) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Ho so benh nhan phai co day du ho ten, ngay sinh, so dien thoai, gioi tinh, CCCD va dia chi thuong tru."
            );
        }
    }

    private boolean isProfileComplete(Patient patient) {
        return isProfileComplete(
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getPhone(),
                patient.getGender(),
                patient.getNationalId(),
                patient.getAddress()
        );
    }

    private boolean isProfileComplete(
            String fullName,
            LocalDate dateOfBirth,
            String phone,
            String gender,
            String nationalId,
            String address
    ) {
        return normalizeText(fullName) != null
                && dateOfBirth != null
                && normalizePhone(phone) != null
                && normalizeGender(gender) != null
                && normalizeNationalId(nationalId) != null
                && normalizeText(address) != null;
    }

    private String buildDefaultFullName(String preferredFullName, String username) {
        String normalized = normalizeText(preferredFullName);
        if (normalized != null) {
            return normalized;
        }
        normalized = normalizeText(username);
        return normalized == null ? "Benh nhan moi" : normalized;
    }

    private String resolveEmail(String username) {
        String normalized = normalizeEmail(username);
        return normalized != null && normalized.contains("@") ? normalized : null;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizePhone(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("^(0|\\+84)\\d{9,10}$")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So dien thoai khong hop le.");
        }
        return normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email không hợp lệ");
        }
        return normalized.toLowerCase();
    }

    private String normalizeGender(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase();
        if (!List.of("MALE", "FEMALE", "OTHER").contains(upper)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Gioi tinh chi ho tro MALE, FEMALE hoac OTHER.");
        }
        return upper;
    }

    private String normalizeNationalId(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("^\\d{12}$")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CCCD phai gom dung 12 chu so.");
        }
        return normalized;
    }

    private boolean isPhoneNumber(String value) {
        String normalized = normalizeText(value);
        return normalized != null && normalized.matches("^(0|\\+84)\\d{9,10}$");
    }

    private String toAlternatePhoneFormat(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.startsWith("+84") && phone.length() > 3) {
            return "0" + phone.substring(3);
        }
        if (phone.startsWith("0") && phone.length() > 1) {
            return "+84" + phone.substring(1);
        }
        return null;
    }

    private String resolveBootstrapPhone(String preferredPhone, String username) {
        String normalizedPreferred = normalizeText(preferredPhone);
        if (normalizedPreferred != null) {
            return normalizePhone(normalizedPreferred);
        }
        return isPhoneNumber(username) ? username : null;
    }

    private String resolveBootstrapEmail(String preferredEmail, String username) {
        String normalizedPreferred = normalizeText(preferredEmail);
        if (normalizedPreferred != null) {
            return normalizeEmail(normalizedPreferred);
        }
        return resolveEmail(username);
    }

    private void ensureBootstrapIdentifiersAvailable(String email, String phone) {
        if (email != null && patientRepository.existsByEmailIgnoreCaseAndIdNot(email, -1)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email này đã được sử dụng");
        }
        if (phone != null && patientRepository.existsByPhoneAndIdNot(phone, -1)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So dien thoai da duoc su dung.");
        }
    }
}
