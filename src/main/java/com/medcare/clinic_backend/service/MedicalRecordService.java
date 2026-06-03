package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.medicalrecord.PatientMedicalRecordDetailResponse;
import com.medcare.clinic_backend.dto.medicalrecord.PatientMedicalRecordListItemResponse;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Service
public class MedicalRecordService {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Autowired
    private ServiceDetailRepository serviceDetailRepository;

    public List<MedicalRecord> getAllRecords() {
        return medicalRecordRepository.findAll();
    }

    public List<MedicalRecord> getRecordsForDoctor(Integer doctorId) {
        return medicalRecordRepository.findByDoctorIdOrderByExaminationDateDesc(doctorId);
    }

    public MedicalRecord getRecordById(Integer id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay ho so benh an ID: " + id));
    }

    public MedicalRecord getRecordByIdForDoctor(Integer id, Integer doctorId) {
        return medicalRecordRepository.findByIdAndDoctorId(id, doctorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay ho so benh an ID: " + id));
    }

    public MedicalRecord getRecordByIdForPatient(Integer id, Integer patientId) {
        return medicalRecordRepository.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay ho so benh an ID: " + id));
    }

    public List<MedicalRecord> getHistoryByPatientId(Integer patientId) {
        return medicalRecordRepository.findByPatientIdOrderByExaminationDateDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<PatientMedicalRecordListItemResponse> getPatientRecordSummaries(Integer patientId) {
        List<Object[]> rows = medicalRecordRepository.findPatientRecordSummaryRows(patientId);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<Integer> recordIds = rows.stream()
                .map(row -> asInteger(row[0]))
                .filter(id -> id != null)
                .toList();
        Map<Integer, PatientMedicalRecordListItemResponse.InvoiceSummary> invoiceByRecordId =
                loadInvoiceSummaryByRecordId(recordIds, patientId);

        List<PatientMedicalRecordListItemResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Integer recordId = asInteger(row[0]);
            String rawStatus = asString(row[7]);
            LocalDateTime recordCreatedAt = asLocalDateTime(row[2]);
            if (recordCreatedAt == null) {
                recordCreatedAt = asLocalDateTime(row[5]);
            }
            if (recordCreatedAt == null) {
                LocalDate examDate = asLocalDate(row[8]);
                recordCreatedAt = examDate == null ? null : examDate.atStartOfDay();
            }
            result.add(new PatientMedicalRecordListItemResponse(
                    recordId,
                    asString(row[1]),
                    recordCreatedAt,
                    asInteger(row[3]),
                    asString(row[4]),
                    asLocalDate(row[8]),
                    asLocalDateTime(row[5]),
                    normalizeAppointmentType(asString(row[6])),
                    normalizeStatus(rawStatus),
                    toStatusDisplay(rawStatus),
                    toStatusColor(rawStatus),
                    asInteger(row[10]),
                    asString(row[11]),
                    asString(row[12]),
                    asString(row[9]),
                    recordId == null ? null : invoiceByRecordId.get(recordId)
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PatientMedicalRecordDetailResponse getPatientRecordDetailById(Integer recordId, Integer patientId) {
        MedicalRecord record = medicalRecordRepository.findByIdAndPatientId(recordId, patientId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay ho so benh an ID: " + recordId
                ));
        return buildPatientRecordDetail(record, patientId);
    }

    @Transactional(readOnly = true)
    public PatientMedicalRecordDetailResponse getPatientRecordDetailByAppointmentId(Integer appointmentId, Integer patientId) {
        MedicalRecord record = medicalRecordRepository.findByAppointmentIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Lich hen nay chua co ho so benh an."
                ));
        return buildPatientRecordDetail(record, patientId);
    }

    public List<MedicalRecord> getHistoryByPatientIdForDoctor(Integer patientId, Integer doctorId) {
        return medicalRecordRepository.findByPatientIdAndDoctorIdOrderByExaminationDateDesc(patientId, doctorId);
    }

    @Transactional
    public MedicalRecord createRecord(MedicalRecord record) {
        if (record == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu ho so benh an khong hop le.");
        }
        if (record.getAppointment() == null || record.getAppointment().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ho so benh an phai co appointmentId.");
        }
        if (record.getDiagnosis() == null || record.getDiagnosis().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chan doan khong duoc de trong.");
        }

        Appointment appointment = appointmentRepository.findById(record.getAppointment().getId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Khong tim thay lich hen ID: " + record.getAppointment().getId()
                ));

        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen chua co bac si.");
        }
        if (appointment.getPatient() == null || appointment.getPatient().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen chua co benh nhan.");
        }
        String appointmentStatus = appointment.getStatus() == null ? "" : appointment.getStatus().trim().toUpperCase();
        if ("CANCELLED".equals(appointmentStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong the tao ho so benh an cho lich hen da huy.");
        }
        if ("PENDING".equals(appointmentStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chi tao ho so benh an cho lich hen da duoc xac nhan.");
        }
        if (medicalRecordRepository.existsByAppointmentId(appointment.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Lich hen nay da co ho so benh an.");
        }

        Integer requestedDoctorId = record.getDoctor() == null ? null : record.getDoctor().getId();
        if (requestedDoctorId != null && !requestedDoctorId.equals(appointment.getDoctor().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si trong ho so khong khop voi bac si cua lich hen.");
        }

        Integer requestedPatientId = record.getPatient() == null ? null : record.getPatient().getId();
        if (requestedPatientId != null && !requestedPatientId.equals(appointment.getPatient().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Benh nhan trong ho so khong khop voi benh nhan cua lich hen.");
        }

        record.setAppointment(appointment);
        record.setDoctor(appointment.getDoctor());
        record.setPatient(appointment.getPatient());
        record.setType(normalizeAppointmentType(appointment.getAppointmentType()));
        if (record.getExaminationDate() == null) {
            record.setExaminationDate(LocalDate.now());
        }
        if (trimToNull(record.getMedicalRecordCode()) == null) {
            record.setMedicalRecordCode(generateMedicalRecordCode());
        }
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(LocalDateTime.now());
        }
        if (!"COMPLETED".equals(appointmentStatus)) {
            appointment.setStatus("COMPLETED");
            appointmentRepository.save(appointment);
        }

        MedicalRecord savedRecord = medicalRecordRepository.save(record);
        invoiceService.createInvoiceFromRecord(savedRecord);
        return savedRecord;
    }

    public MedicalRecord updateRecord(Integer id, MedicalRecord details) {
        MedicalRecord record = getRecordById(id);
        applyMutableFields(record, details);
        return medicalRecordRepository.save(record);
    }

    public MedicalRecord updateRecordForDoctor(Integer id, Integer doctorId, MedicalRecord details) {
        MedicalRecord record = getRecordByIdForDoctor(id, doctorId);
        applyMutableFields(record, details);
        return medicalRecordRepository.save(record);
    }

    public void deleteRecord(Integer id) {
        if (!medicalRecordRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay ho so benh an ID: " + id);
        }
        medicalRecordRepository.deleteById(id);
    }

    private void applyMutableFields(MedicalRecord record, MedicalRecord details) {
        if (details == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu cap nhat khong hop le.");
        }
        if (details.getDiagnosis() != null && details.getDiagnosis().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chan doan khong duoc de trong.");
        }

        if (details.getExaminationDate() != null) {
            record.setExaminationDate(details.getExaminationDate());
        }
        if (details.getDiagnosis() != null) {
            record.setDiagnosis(details.getDiagnosis());
        }
        if (details.getTreatmentPlan() != null) {
            record.setTreatmentPlan(details.getTreatmentPlan());
        }
        if (details.getPrescription() != null) {
            record.setPrescription(details.getPrescription());
        }
        if (details.getDoctorAdvice() != null) {
            record.setDoctorAdvice(details.getDoctorAdvice());
        }
    }

    private PatientMedicalRecordDetailResponse buildPatientRecordDetail(MedicalRecord record, Integer patientId) {
        Integer recordId = record == null ? null : record.getId();
        if (recordId == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Du lieu benh an khong hop le.");
        }

        Appointment appointment = record.getAppointment();
        String appointmentStatus = appointment == null ? null : appointment.getStatus();
        PatientMedicalRecordDetailResponse.AppointmentInfo appointmentInfo =
                new PatientMedicalRecordDetailResponse.AppointmentInfo(
                        appointment == null ? null : appointment.getId(),
                        appointment == null ? null : appointment.getAppointmentCode(),
                        appointment == null ? null : appointment.getAppointmentDate(),
                        normalizeAppointmentType(record.getType() == null
                                ? (appointment == null ? null : appointment.getAppointmentType())
                                : record.getType()),
                        normalizeStatus(appointmentStatus),
                        toStatusDisplay(appointmentStatus),
                        toStatusColor(appointmentStatus),
                        appointment == null ? null : appointment.getSymptoms(),
                        appointment == null ? null : appointment.getNotes()
                );

        Doctor doctor = record.getDoctor();
        String specialtyName = doctor != null && doctor.getSpecialty() != null
                ? doctor.getSpecialty().getName()
                : null;
        PatientMedicalRecordDetailResponse.DoctorInfo doctorInfo =
                new PatientMedicalRecordDetailResponse.DoctorInfo(
                        doctor == null ? null : doctor.getId(),
                        doctor == null ? null : doctor.getFullName(),
                        doctor == null ? null : doctor.getPhone(),
                        doctor == null ? null : doctor.getEmail(),
                        specialtyName
                );

        List<PatientMedicalRecordDetailResponse.MedicineItem> medicines = prescriptionDetailRepository
                .findPatientMedicineRowsByRecordId(recordId, patientId)
                .stream()
                .map(this::toMedicineItem)
                .toList();

        List<PatientMedicalRecordDetailResponse.ServiceItem> services = serviceDetailRepository
                .findPatientServiceRowsByRecordId(recordId, patientId)
                .stream()
                .map(this::toServiceItem)
                .toList();

        PatientMedicalRecordDetailResponse.InvoiceInfo invoiceInfo = invoiceRepository
                .findByMedicalRecordIdAndMedicalRecordPatientId(recordId, patientId)
                .map(this::toInvoiceInfo)
                .orElse(null);

        PatientMedicalRecordDetailResponse.FollowUpInfo followUpInfo = toFollowUpInfo(record.getFollowUpAppointment());

        return new PatientMedicalRecordDetailResponse(
                recordId,
                record.getMedicalRecordCode(),
                resolveRecordCreatedAt(record),
                record.getExaminationDate(),
                record.getDiagnosis(),
                record.getDoctorAdvice(),
                record.getTreatmentPlan(),
                record.getPrescription(),
                appointmentInfo,
                doctorInfo,
                medicines,
                services,
                invoiceInfo,
                followUpInfo
        );
    }

    private PatientMedicalRecordDetailResponse.MedicineItem toMedicineItem(Object[] row) {
        Integer quantity = asInteger(row[3]);
        double unitPrice = safeDouble(asDouble(row[6]));
        double totalPrice = unitPrice * (quantity == null ? 0 : quantity);
        return new PatientMedicalRecordDetailResponse.MedicineItem(
                asInteger(row[0]),
                asString(row[1]),
                asString(row[2]),
                quantity,
                asString(row[4]),
                asString(row[5]),
                unitPrice,
                totalPrice
        );
    }

    private PatientMedicalRecordDetailResponse.ServiceItem toServiceItem(Object[] row) {
        Integer quantity = asInteger(row[2]);
        double unitPrice = safeDouble(asDouble(row[4]));
        double totalPrice = unitPrice * (quantity == null ? 0 : quantity);
        return new PatientMedicalRecordDetailResponse.ServiceItem(
                asInteger(row[0]),
                asString(row[1]),
                quantity,
                asString(row[3]),
                unitPrice,
                totalPrice
        );
    }

    private PatientMedicalRecordDetailResponse.InvoiceInfo toInvoiceInfo(Invoice invoice) {
        Integer invoiceId = invoice == null ? null : invoice.getId();
        String status = normalizeInvoiceStatus(invoice == null ? null : invoice.getStatus());
        double consultationFee = safeDouble(invoice == null ? null : invoice.getConsultationFee());
        double medicineFee = safeDouble(invoice == null ? null : invoice.getMedicineFee());
        double serviceFee = safeDouble(invoice == null ? null : invoice.getServiceFee());
        double totalAmount = safeDouble(invoice == null ? null : invoice.getTotalAmount());
        if (totalAmount <= 0) {
            totalAmount = consultationFee + medicineFee + serviceFee;
        }

        boolean canPayOnline = ("UNPAID".equals(status) || "PENDING".equals(status)) && totalAmount > 0;
        return new PatientMedicalRecordDetailResponse.InvoiceInfo(
                invoiceId,
                invoiceId == null ? null : "INV" + String.format("%06d", invoiceId),
                status,
                consultationFee,
                medicineFee,
                serviceFee,
                totalAmount,
                canPayOnline,
                invoice == null ? null : invoice.getCreatedAt()
        );
    }

    private PatientMedicalRecordDetailResponse.FollowUpInfo toFollowUpInfo(Appointment followUpAppointment) {
        Integer followUpId = followUpAppointment == null ? null : followUpAppointment.getId();
        if (followUpId == null) {
            return null;
        }
        String status = followUpAppointment.getStatus();
        return new PatientMedicalRecordDetailResponse.FollowUpInfo(
                followUpId,
                followUpAppointment.getAppointmentCode(),
                followUpAppointment.getAppointmentDate(),
                normalizeAppointmentType(followUpAppointment.getAppointmentType()),
                normalizeStatus(status),
                toStatusDisplay(status),
                toStatusColor(status),
                trimToNull(followUpAppointment.getFollowUpNote()) == null
                        ? followUpAppointment.getNotes()
                        : followUpAppointment.getFollowUpNote()
        );
    }

    private Map<Integer, PatientMedicalRecordListItemResponse.InvoiceSummary> loadInvoiceSummaryByRecordId(
            List<Integer> recordIds,
            Integer patientId
    ) {
        Map<Integer, PatientMedicalRecordListItemResponse.InvoiceSummary> result = new HashMap<>();
        if (recordIds == null || recordIds.isEmpty()) {
            return result;
        }

        for (Object[] row : invoiceRepository.findPatientInvoiceSummaryRowsByRecordIds(recordIds, patientId)) {
            Integer recordId = asInteger(row[0]);
            Integer invoiceId = asInteger(row[1]);
            if (recordId == null || invoiceId == null) {
                continue;
            }
            result.put(recordId, new PatientMedicalRecordListItemResponse.InvoiceSummary(
                    invoiceId,
                    "INV" + String.format("%06d", invoiceId),
                    normalizeInvoiceStatus(asString(row[2])),
                    safeDouble(asDouble(row[3]))
            ));
        }
        return result;
    }

    private String normalizeAppointmentType(String type) {
        String normalized = trimToNull(type);
        if (normalized == null) {
            return "Kh\u00e1m b\u1ec7nh";
        }
        String folded = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT);
        if (folded.contains("tai") && folded.contains("kham")) {
            return "T\u00e1i kh\u00e1m";
        }
        return "Kh\u00e1m b\u1ec7nh";
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "PENDING";
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.contains("CANCEL")) {
            return "CANCELLED";
        }
        if (upper.contains("COMPLETED")) {
            return "COMPLETED";
        }
        if (upper.contains("CONFIRM")) {
            return "CONFIRMED";
        }
        if (upper.contains("PENDING")) {
            return "PENDING";
        }
        return upper;
    }

    private String toStatusDisplay(String status) {
        String normalized = normalizeStatus(status);
        return switch (normalized) {
            case "CANCELLED" -> "\u0110\u00e3 h\u1ee7y";
            case "COMPLETED" -> "\u0110\u00e3 kh\u00e1m";
            default -> "Ch\u01b0a kh\u00e1m";
        };
    }

    private String toStatusColor(String status) {
        String normalized = normalizeStatus(status);
        return switch (normalized) {
            case "CANCELLED" -> "red";
            case "COMPLETED" -> "green";
            default -> "yellow";
        };
    }

    private String normalizeInvoiceStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "UNPAID";
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.contains("UNPAID")) {
            return "UNPAID";
        }
        if (upper.equals("PAID") || upper.contains("PAID_ONLINE")) {
            return "PAID";
        }
        if (upper.contains("PENDING")) {
            return "PENDING";
        }
        if (upper.contains("FAIL")) {
            return "FAILED";
        }
        if (upper.contains("CANCEL")) {
            return "CANCELLED";
        }
        return upper;
    }

    private LocalDateTime resolveRecordCreatedAt(MedicalRecord record) {
        if (record == null) {
            return null;
        }
        if (record.getCreatedAt() != null) {
            return record.getCreatedAt();
        }
        Appointment appointment = record.getAppointment();
        if (appointment != null && appointment.getAppointmentDate() != null) {
            return appointment.getAppointmentDate();
        }
        return record.getExaminationDate() == null ? null : record.getExaminationDate().atStartOfDay();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateMedicalRecordCode() {
        String code;
        do {
            code = "BA-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000);
        } while (medicalRecordRepository.existsByMedicalRecordCode(code));
        return code;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.doubleValue();
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private LocalDate asLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
