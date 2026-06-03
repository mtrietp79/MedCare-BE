package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.DashboardSummaryResponse;
import com.medcare.clinic_backend.dto.MonthlyPatientResponse;
import com.medcare.clinic_backend.dto.MonthlyRevenueResponse;
import com.medcare.clinic_backend.dto.RecentAppointmentResponse;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.ServicePackageBooking;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.ServicePackageBookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ServicePackageBookingRepository servicePackageBookingRepository;

    public DashboardSummaryResponse getSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfNextMonth = startOfCurrentMonth.plusMonths(1);
        LocalDateTime startOfPreviousMonth = startOfCurrentMonth.minusMonths(1);

        List<Appointment> allAppointments = safeList(() -> appointmentRepository.findAll());

        long totalAppointments = allAppointments.stream()
                .filter(Objects::nonNull)
                .filter(appointment -> !isCancelledStatus(appointment.getStatus()))
                .count();
        long activePatients = countDistinctActivePatientsOverall();
        long workingDoctors = safeLong(() -> doctorRepository.countByIsActiveTrue());

        long currentMonthAppointments = countAppointmentsBetweenExcludingCancelled(
                allAppointments,
                startOfCurrentMonth,
                startOfNextMonth
        );
        long previousMonthAppointments = countAppointmentsBetweenExcludingCancelled(
                allAppointments,
                startOfPreviousMonth,
                startOfCurrentMonth
        );
        int appointmentGrowthPercent = calculateGrowthPercent(currentMonthAppointments, previousMonthAppointments);

        long currentMonthPatients = countDistinctPatientsBetweenOverall(startOfCurrentMonth, startOfNextMonth);
        long previousMonthPatients = countDistinctPatientsBetweenOverall(startOfPreviousMonth, startOfCurrentMonth);
        int patientGrowthPercent = calculateGrowthPercent(currentMonthPatients, previousMonthPatients);

        long currentMonthDoctors = countDistinctDoctorsBetween(startOfCurrentMonth, startOfNextMonth);
        long previousMonthDoctors = countDistinctDoctorsBetween(startOfPreviousMonth, startOfCurrentMonth);
        int doctorGrowth = (int) (currentMonthDoctors - previousMonthDoctors);

        double monthlyRevenue = calculateMonthlyRevenue(startOfCurrentMonth, startOfNextMonth);
        double previousMonthRevenue = calculateMonthlyRevenue(startOfPreviousMonth, startOfCurrentMonth);
        double revenueGrowthPercent = calculateRevenueGrowthPercent(monthlyRevenue, previousMonthRevenue);

        return new DashboardSummaryResponse(
                totalAppointments,
                activePatients,
                workingDoctors,
                monthlyRevenue,
                appointmentGrowthPercent,
                patientGrowthPercent,
                doctorGrowth,
                revenueGrowthPercent
        );
    }

    public List<MonthlyPatientResponse> getMonthlyPatients(Integer year) {
        int targetYear = year == null ? LocalDateTime.now().getYear() : year;
        List<MonthlyPatientResponse> monthly = init12Months();
        Map<Integer, Set<Integer>> uniquePatientsByMonth = new HashMap<>();

        for (Appointment appointment : safeList(() -> appointmentRepository.findAll())) {
            if (appointment == null || appointment.getAppointmentDate() == null || appointment.getPatient() == null) {
                continue;
            }
            if (isCancelledStatus(appointment.getStatus())) {
                continue;
            }
            if (appointment.getAppointmentDate().getYear() != targetYear) {
                continue;
            }
            Integer patientId = appointment.getPatient().getId();
            if (patientId == null) {
                continue;
            }
            int month = appointment.getAppointmentDate().getMonthValue();
            uniquePatientsByMonth.computeIfAbsent(month, key -> new HashSet<>()).add(patientId);
        }

        for (ServicePackageBooking booking : safeList(() -> servicePackageBookingRepository.findAll())) {
            if (booking == null || booking.getBookingDate() == null || booking.getPatient() == null) {
                continue;
            }
            if (isCancelledStatus(booking.getStatus())) {
                continue;
            }
            if (booking.getBookingDate().getYear() != targetYear) {
                continue;
            }
            Integer patientId = booking.getPatient().getId();
            if (patientId == null) {
                continue;
            }
            int month = booking.getBookingDate().getMonthValue();
            uniquePatientsByMonth.computeIfAbsent(month, key -> new HashSet<>()).add(patientId);
        }

        for (int i = 0; i < monthly.size(); i++) {
            int month = i + 1;
            long total = uniquePatientsByMonth.getOrDefault(month, Collections.emptySet()).size();
            monthly.get(i).setTotal(total);
        }
        return monthly;
    }

    public List<MonthlyRevenueResponse> getMonthlyRevenue(Integer year) {
        int targetYear = year == null ? LocalDateTime.now().getYear() : year;
        List<MonthlyRevenueResponse> monthly = init12MonthRevenue();
        Map<Integer, Double> revenueByMonth = new HashMap<>();

        for (Invoice invoice : safeList(() -> invoiceRepository.findAll())) {
            if (invoice == null || invoice.getCreatedAt() == null) {
                continue;
            }
            if (invoice.getCreatedAt().getYear() != targetYear) {
                continue;
            }
            if (!isPaidInvoiceStatus(invoice.getStatus())) {
                continue;
            }
            int month = invoice.getCreatedAt().getMonthValue();
            revenueByMonth.merge(month, safeDouble(invoice.getTotalAmount()), Double::sum);
        }

        for (ServicePackageBooking booking : safeList(() -> servicePackageBookingRepository.findAll())) {
            if (booking == null) {
                continue;
            }
            LocalDateTime paidTime = booking.getUpdatedAt() != null ? booking.getUpdatedAt() : booking.getCreatedAt();
            if (paidTime == null || paidTime.getYear() != targetYear) {
                continue;
            }
            if (!isPaidBooking(booking)) {
                continue;
            }
            int month = paidTime.getMonthValue();
            revenueByMonth.merge(month, safeDouble(booking.getTotalAmount()), Double::sum);
        }

        for (int i = 0; i < monthly.size(); i++) {
            int month = i + 1;
            monthly.get(i).setRevenue(roundToOneDecimal(revenueByMonth.getOrDefault(month, 0.0)));
        }
        return monthly;
    }

    public List<RecentAppointmentResponse> getRecentAppointments() {
        List<Appointment> appointments = safeList(() -> appointmentRepository.findTop10ByOrderByAppointmentDateDesc());
        if (appointments.isEmpty()) {
            return List.of();
        }

        List<RecentAppointmentResponse> result = new ArrayList<>();
        for (Appointment appointment : appointments) {
            LocalDateTime appointmentDateTime = appointment == null ? null : appointment.getAppointmentDate();
            String time = appointment != null && appointment.getAppointmentDate() != null
                    ? appointment.getAppointmentDate().toLocalTime().format(TIME_FORMATTER)
                    : null;
            String date = appointmentDateTime == null ? null : appointmentDateTime.toLocalDate().toString();
            String statusCode = resolveStatusCode(appointment == null ? null : appointment.getStatus());
            result.add(new RecentAppointmentResponse(
                    appointment == null ? null : safeText(appointment.getPatientName()),
                    appointment == null ? null : safeText(appointment.getDoctorName()),
                    resolveSpecialtyName(appointment),
                    date,
                    time,
                    mapStatus(statusCode),
                    statusCode,
                    appointmentDateTime == null ? null : appointmentDateTime.toString()
            ));
        }
        return result;
    }

    private long countDistinctPatientsBetweenOverall(LocalDateTime start, LocalDateTime end) {
        try {
            Set<Integer> patientIds = new HashSet<>();
            for (Appointment appointment : safeList(() -> appointmentRepository.findAll())) {
                if (appointment == null || appointment.getAppointmentDate() == null || appointment.getPatient() == null) {
                    continue;
                }
                LocalDateTime dt = appointment.getAppointmentDate();
                if (dt.isBefore(start) || !dt.isBefore(end)) {
                    continue;
                }
                if (isCancelledStatus(appointment.getStatus())) {
                    continue;
                }
                Integer patientId = appointment.getPatient().getId();
                if (patientId != null) {
                    patientIds.add(patientId);
                }
            }
            for (ServicePackageBooking booking : safeList(() -> servicePackageBookingRepository.findAll())) {
                if (booking == null || booking.getBookingDate() == null || booking.getPatient() == null) {
                    continue;
                }
                LocalDateTime dt = booking.getBookingDate().atTime(
                        booking.getBookingTime() == null ? LocalTime.MIDNIGHT : booking.getBookingTime()
                );
                if (dt.isBefore(start) || !dt.isBefore(end)) {
                    continue;
                }
                if (isCancelledStatus(booking.getStatus())) {
                    continue;
                }
                Integer patientId = booking.getPatient().getId();
                if (patientId != null) {
                    patientIds.add(patientId);
                }
            }
            return patientIds.size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private long countDistinctActivePatientsOverall() {
        try {
            Set<Integer> patientIds = new HashSet<>();
            for (Appointment appointment : safeList(() -> appointmentRepository.findAll())) {
                if (appointment == null || appointment.getPatient() == null || isCancelledStatus(appointment.getStatus())) {
                    continue;
                }
                Integer patientId = appointment.getPatient().getId();
                if (patientId != null) {
                    patientIds.add(patientId);
                }
            }
            for (ServicePackageBooking booking : safeList(() -> servicePackageBookingRepository.findAll())) {
                if (booking == null || booking.getPatient() == null || isCancelledStatus(booking.getStatus())) {
                    continue;
                }
                Integer patientId = booking.getPatient().getId();
                if (patientId != null) {
                    patientIds.add(patientId);
                }
            }
            return patientIds.size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private long countDistinctDoctorsBetween(LocalDateTime start, LocalDateTime end) {
        try {
            Set<Integer> doctorIds = new HashSet<>();
            for (Appointment appointment : safeList(() -> appointmentRepository.findAll())) {
                if (appointment == null || appointment.getAppointmentDate() == null || appointment.getDoctor() == null) {
                    continue;
                }
                LocalDateTime dt = appointment.getAppointmentDate();
                if (dt.isBefore(start) || !dt.isBefore(end)) {
                    continue;
                }
                if (isCancelledStatus(appointment.getStatus())) {
                    continue;
                }
                Integer doctorId = appointment.getDoctor().getId();
                if (doctorId != null && Boolean.TRUE.equals(appointment.getDoctor().getIsActive())) {
                    doctorIds.add(doctorId);
                }
            }
            return doctorIds.size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double calculateMonthlyRevenue(LocalDateTime start, LocalDateTime end) {
        double totalRevenue = 0.0;

        for (Invoice invoice : safeList(() -> invoiceRepository.findAll())) {
            if (invoice == null || invoice.getCreatedAt() == null) {
                continue;
            }
            if (invoice.getCreatedAt().isBefore(start) || !invoice.getCreatedAt().isBefore(end)) {
                continue;
            }
            if (!isPaidInvoiceStatus(invoice.getStatus())) {
                continue;
            }
            totalRevenue += safeDouble(invoice.getTotalAmount());
        }

        for (ServicePackageBooking booking : safeList(() -> servicePackageBookingRepository.findAll())) {
            if (booking == null) {
                continue;
            }
            LocalDateTime paidTime = booking.getUpdatedAt() != null ? booking.getUpdatedAt() : booking.getCreatedAt();
            if (paidTime == null || paidTime.isBefore(start) || !paidTime.isBefore(end)) {
                continue;
            }
            if (!isPaidBooking(booking)) {
                continue;
            }
            totalRevenue += safeDouble(booking.getTotalAmount());
        }

        return totalRevenue;
    }

    private long countAppointmentsBetweenExcludingCancelled(
            List<Appointment> appointments,
            LocalDateTime start,
            LocalDateTime end
    ) {
        if (appointments == null || appointments.isEmpty()) {
            return 0;
        }
        return appointments.stream()
                .filter(Objects::nonNull)
                .filter(appointment -> appointment.getAppointmentDate() != null)
                .filter(appointment -> !appointment.getAppointmentDate().isBefore(start))
                .filter(appointment -> appointment.getAppointmentDate().isBefore(end))
                .filter(appointment -> !isCancelledStatus(appointment.getStatus()))
                .count();
    }

    private List<MonthlyPatientResponse> init12Months() {
        List<MonthlyPatientResponse> monthly = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            monthly.add(new MonthlyPatientResponse("Thang " + month, 0));
        }
        return monthly;
    }

    private List<MonthlyRevenueResponse> init12MonthRevenue() {
        List<MonthlyRevenueResponse> monthly = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            monthly.add(new MonthlyRevenueResponse("Thang " + month, 0.0));
        }
        return monthly;
    }

    private String mapStatus(String statusCode) {
        if (statusCode == null) {
            return "Ch\u01b0a kh\u00e1m";
        }
        if ("CANCELLED".equals(statusCode)) {
            return "H\u1ee7y l\u1ecbch";
        }
        if ("COMPLETED".equals(statusCode)) {
            return "\u0110\u00e3 kh\u00e1m";
        }
        if ("CONFIRMED".equals(statusCode)) {
            return "\u0110\u00e3 x\u00e1c nh\u1eadn";
        }
        return "Ch\u01b0a kh\u00e1m";
    }

    private String resolveStatusCode(String rawStatus) {
        String normalized = normalizeStatus(rawStatus);
        if ("CANCELLED".equals(normalized) || "CANCELED".equals(normalized) || "HUY_LICH".equals(normalized)) {
            return "CANCELLED";
        }
        if ("COMPLETED".equals(normalized) || "DA_KHAM".equals(normalized)) {
            return "COMPLETED";
        }
        if ("CONFIRMED".equals(normalized) || "DA_XAC_NHAN".equals(normalized)) {
            return "CONFIRMED";
        }
        return "PENDING";
    }

    private int calculateGrowthPercent(long currentValue, long previousValue) {
        if (previousValue <= 0) {
            return currentValue > 0 ? 100 : 0;
        }
        return (int) Math.round(((double) (currentValue - previousValue) / previousValue) * 100);
    }

    private double calculateRevenueGrowthPercent(double currentRevenue, double previousRevenue) {
        if (previousRevenue <= 0) {
            return currentRevenue > 0 ? 100.0 : 0.0;
        }
        double value = ((currentRevenue - previousRevenue) / previousRevenue) * 100;
        return Math.round(value * 10.0) / 10.0;
    }

    private boolean isCompletedStatus(String status) {
        String normalized = normalizeStatus(status);
        return "COMPLETED".equals(normalized) || "DA_KHAM".equals(normalized);
    }

    private boolean isCancelledStatus(String status) {
        String normalized = normalizeStatus(status);
        return "CANCELLED".equals(normalized)
                || "CANCELED".equals(normalized)
                || "HUY_LICH".equals(normalized);
    }

    private boolean isPaidInvoiceStatus(String status) {
        String normalized = normalizeStatus(status);
        return "PAID".equals(normalized)
                || "DA_THANH_TOAN".equals(normalized)
                || "THANH_TOAN_THANH_CONG".equals(normalized);
    }

    private boolean isPaidBooking(ServicePackageBooking booking) {
        if (booking == null) {
            return false;
        }
        return isPaidInvoiceStatus(booking.getPaymentStatus()) || "PAID".equals(normalizeStatus(booking.getStatus()));
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "";
        }
        String upper = status.trim().toUpperCase(Locale.ROOT)
                .replace('Đ', 'D')
                .replace(' ', '_')
                .replace('-', '_');
        String normalized = Normalizer.normalize(upper, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Z0-9_]", "_")
                .replaceAll("_+", "_");
        if (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String resolveSpecialtyName(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        String directSpecialtyName = safeText(appointment.getSpecialtyName());
        if (directSpecialtyName != null) {
            return directSpecialtyName;
        }
        if (appointment.getDoctor() != null && appointment.getDoctor().getSpecialty() != null) {
            String doctorSpecialtyName = safeText(appointment.getDoctor().getSpecialty().getName());
            if (doctorSpecialtyName != null) {
                return doctorSpecialtyName;
            }
        }
        if (appointment.getMedicalService() != null && appointment.getMedicalService().getSpecialty() != null) {
            return safeText(appointment.getMedicalService().getSpecialty().getName());
        }
        return null;
    }

    private long safeLong(SupplierWithException<Long> supplier) {
        try {
            Long value = supplier.get();
            return value == null ? 0 : value;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double safeDouble(Double value) {
        return value == null ? 0 : value;
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private <T> List<T> safeList(SupplierWithException<List<T>> supplier) {
        try {
            List<T> value = supplier.get();
            return value == null ? List.of() : value;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }
}
