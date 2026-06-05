package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.DashboardSummaryResponse;
import com.medcare.clinic_backend.dto.MonthlyPatientResponse;
import com.medcare.clinic_backend.dto.MonthlyRevenueResponse;
import com.medcare.clinic_backend.dto.RecentAppointmentResponse;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.ServicePackageBooking;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.ServicePackageBookingRepository;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter REPORT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Locale VIETNAMESE_LOCALE = new Locale("vi", "VN");

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

    public byte[] exportDashboardReport(Integer year) {
        int targetYear = year == null ? LocalDateTime.now().getYear() : year;
        LocalDateTime exportedAt = LocalDateTime.now();
        DashboardSummaryResponse summary = getSummary();
        List<MonthlyPatientResponse> monthlyPatients = getMonthlyPatients(targetYear);
        List<MonthlyRevenueResponse> monthlyRevenue = getMonthlyRevenue(targetYear);
        List<Long> monthlyAppointments = getMonthlyAppointmentCounts(targetYear);
        List<RecentAppointmentResponse> recentAppointments = getRecentAppointments();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DashboardReportStyles styles = createReportStyles(workbook);

            writeSummarySheet(workbook, styles, summary, targetYear, exportedAt);
            writeMonthlyOverviewSheet(
                    workbook,
                    styles,
                    monthlyAppointments,
                    monthlyPatients,
                    monthlyRevenue,
                    targetYear,
                    exportedAt
            );
            writeMonthlyPatientsSheet(workbook, styles, monthlyPatients, targetYear, exportedAt);
            writeMonthlyRevenueSheet(workbook, styles, monthlyRevenue, targetYear, exportedAt);
            writeRecentAppointmentsSheet(workbook, styles, recentAppointments, targetYear, exportedAt);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể xuất báo cáo dashboard dưới dạng Excel."
            );
        }
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

    private List<Long> getMonthlyAppointmentCounts(int targetYear) {
        List<Long> monthlyCounts = new ArrayList<>(Collections.nCopies(12, 0L));

        for (Appointment appointment : safeList(() -> appointmentRepository.findAll())) {
            if (appointment == null || appointment.getAppointmentDate() == null) {
                continue;
            }
            if (isCancelledStatus(appointment.getStatus())) {
                continue;
            }
            if (appointment.getAppointmentDate().getYear() != targetYear) {
                continue;
            }
            int monthIndex = appointment.getAppointmentDate().getMonthValue() - 1;
            monthlyCounts.set(monthIndex, monthlyCounts.get(monthIndex) + 1);
        }

        return monthlyCounts;
    }

    private void writeSummarySheet(
            Workbook workbook,
            DashboardReportStyles styles,
            DashboardSummaryResponse summary,
            int year,
            LocalDateTime exportedAt
    ) {
        Sheet sheet = workbook.createSheet("Tổng quan");
        prepareSheet(sheet);

        writeMergedTitle(sheet, 0, 2, "Báo cáo Dashboard Admin", styles.titleStyle);
        writeLabelValueRow(sheet, 2, "Năm báo cáo", String.valueOf(year), styles.labelStyle, styles.textStyle);
        writeLabelValueRow(
                sheet,
                3,
                "Thời gian xuất",
                exportedAt.format(REPORT_DATE_TIME_FORMATTER),
                styles.labelStyle,
                styles.textStyle
        );
        writeMergedTitle(sheet, 5, 2, "Chỉ số vận hành", styles.sectionStyle);
        writeHeaderRow(sheet, 6, styles.headerStyle, "Chỉ số", "Giá trị", "Diễn giải");

        writeMetricRow(
                sheet,
                7,
                "Tổng lịch hẹn",
                summary.getTotalAppointments(),
                styles.numberStyle,
                "Tổng số lịch hẹn không bị hủy trên toàn hệ thống.",
                styles
        );
        writeMetricRow(
                sheet,
                8,
                "Bệnh nhân đang hoạt động",
                summary.getActivePatients(),
                styles.numberStyle,
                "Bệnh nhân có phát sinh lịch khám hoặc gói dịch vụ.",
                styles
        );
        writeMetricRow(
                sheet,
                9,
                "Bác sĩ đang làm việc",
                summary.getWorkingDoctors(),
                styles.numberStyle,
                "Bác sĩ đang ở trạng thái hoạt động.",
                styles
        );
        writeMetricRow(
                sheet,
                10,
                "Doanh thu tháng hiện tại",
                summary.getMonthlyRevenue(),
                styles.currencyStyle,
                "Chỉ tính doanh thu đã thanh toán của tháng hiện tại.",
                styles
        );
        writeMetricRow(
                sheet,
                11,
                "Tăng trưởng lịch hẹn",
                summary.getAppointmentGrowthPercent() / 100.0,
                styles.signedPercentStyle,
                "So sánh số lịch hẹn tháng hiện tại với tháng trước.",
                styles
        );
        writeMetricRow(
                sheet,
                12,
                "Tăng trưởng bệnh nhân",
                summary.getPatientGrowthPercent() / 100.0,
                styles.signedPercentStyle,
                "So sánh số bệnh nhân tháng hiện tại với tháng trước.",
                styles
        );
        writeMetricRow(
                sheet,
                13,
                "Biến động bác sĩ",
                summary.getDoctorGrowth(),
                styles.signedNumberStyle,
                "Chênh lệch số bác sĩ hoạt động giữa hai tháng gần nhất.",
                styles
        );
        writeMetricRow(
                sheet,
                14,
                "Tăng trưởng doanh thu",
                summary.getRevenueGrowthPercent() / 100.0,
                styles.signedPercentStyle,
                "So sánh doanh thu tháng hiện tại với tháng trước.",
                styles
        );
        writeMergedNote(
                sheet,
                16,
                2,
                "Ghi chú: Doanh thu chỉ tính các khoản đã thanh toán. Tỷ lệ tăng trưởng được so sánh với tháng trước.",
                styles.noteStyle
        );

        sheet.createFreezePane(0, 7);
        autoSize(sheet, 3);
    }

    private void writeMonthlyOverviewSheet(
            Workbook workbook,
            DashboardReportStyles styles,
            List<Long> monthlyAppointments,
            List<MonthlyPatientResponse> monthlyPatients,
            List<MonthlyRevenueResponse> monthlyRevenue,
            int year,
            LocalDateTime exportedAt
    ) {
        Sheet sheet = workbook.createSheet("Báo cáo theo tháng");
        prepareSheet(sheet);

        writeMergedTitle(sheet, 0, 6, "Báo cáo tổng hợp theo tháng", styles.titleStyle);
        writeLabelValueRow(sheet, 2, "Năm báo cáo", String.valueOf(year), styles.labelStyle, styles.textStyle);
        writeLabelValueRow(
                sheet,
                3,
                "Thời gian xuất",
                exportedAt.format(REPORT_DATE_TIME_FORMATTER),
                styles.labelStyle,
                styles.textStyle
        );
        writeHeaderRow(
                sheet,
                5,
                styles.headerStyle,
                "Tháng",
                "Tổng lịch hẹn",
                "Bệnh nhân trong tháng",
                "Doanh thu đã thanh toán",
                "Tăng trưởng lịch hẹn",
                "Tăng trưởng bệnh nhân",
                "Tăng trưởng doanh thu"
        );

        long totalAppointmentsInYear = 0;
        double totalRevenueInYear = 0.0;
        long peakAppointments = -1;
        int peakAppointmentMonth = 1;
        double peakRevenue = -1.0;
        int peakRevenueMonth = 1;

        for (int i = 0; i < 12; i++) {
            long appointmentCount = i < monthlyAppointments.size() ? monthlyAppointments.get(i) : 0;
            long patientCount = i < monthlyPatients.size() ? monthlyPatients.get(i).getTotal() : 0;
            double revenue = i < monthlyRevenue.size() ? safeDouble(monthlyRevenue.get(i).getRevenue()) : 0.0;

            totalAppointmentsInYear += appointmentCount;
            totalRevenueInYear += revenue;

            if (appointmentCount > peakAppointments) {
                peakAppointments = appointmentCount;
                peakAppointmentMonth = i + 1;
            }
            if (revenue > peakRevenue) {
                peakRevenue = revenue;
                peakRevenueMonth = i + 1;
            }

            Row row = sheet.createRow(6 + i);
            setTextCell(row, 0, getMonthLabel(i + 1), styles.textStyle);
            setLongCell(row, 1, appointmentCount, styles.numberStyle);
            setLongCell(row, 2, patientCount, styles.numberStyle);
            setDoubleCell(row, 3, revenue, styles.currencyStyle);

            if (i == 0) {
                setTextCell(row, 4, "-", styles.centerTextStyle);
                setTextCell(row, 5, "-", styles.centerTextStyle);
                setTextCell(row, 6, "-", styles.centerTextStyle);
            } else {
                long previousAppointments = monthlyAppointments.get(i - 1);
                long previousPatients = monthlyPatients.get(i - 1).getTotal();
                double previousRevenue = safeDouble(monthlyRevenue.get(i - 1).getRevenue());

                setDoubleCell(
                        row,
                        4,
                        calculateGrowthRatio(appointmentCount, previousAppointments),
                        styles.signedPercentStyle
                );
                setDoubleCell(
                        row,
                        5,
                        calculateGrowthRatio(patientCount, previousPatients),
                        styles.signedPercentStyle
                );
                setDoubleCell(
                        row,
                        6,
                        calculateGrowthRatio(revenue, previousRevenue),
                        styles.signedPercentStyle
                );
            }
        }

        writeMergedTitle(sheet, 19, 1, "Điểm nhấn năm " + year, styles.sectionStyle);
        writeHeaderRow(sheet, 20, styles.headerStyle, "Chỉ số", "Giá trị");
        writeMetricValueRow(sheet, 21, "Tổng lịch hẹn trong năm", totalAppointmentsInYear, styles.numberStyle, styles);
        writeMetricValueRow(sheet, 22, "Tổng doanh thu trong năm", totalRevenueInYear, styles.currencyStyle, styles);
        writeMetricValueRow(
                sheet,
                23,
                "Doanh thu trung bình trên lịch hẹn",
                totalAppointmentsInYear == 0 ? 0.0 : totalRevenueInYear / totalAppointmentsInYear,
                styles.currencyStyle,
                styles
        );
        writeMetricValueRow(
                sheet,
                24,
                "Tháng có nhiều lịch hẹn nhất",
                buildPeakText(peakAppointmentMonth, peakAppointments, "lịch hẹn"),
                styles.textStyle,
                styles
        );
        writeMetricValueRow(
                sheet,
                25,
                "Tháng có doanh thu cao nhất",
                buildPeakRevenueText(peakRevenueMonth, peakRevenue),
                styles.textStyle,
                styles
        );

        sheet.createFreezePane(0, 6);
        sheet.setAutoFilter(new CellRangeAddress(5, 17, 0, 6));
        autoSize(sheet, 7);
    }

    private void writeMonthlyPatientsSheet(
            Workbook workbook,
            DashboardReportStyles styles,
            List<MonthlyPatientResponse> monthlyPatients,
            int year,
            LocalDateTime exportedAt
    ) {
        Sheet sheet = workbook.createSheet("Bệnh nhân theo tháng");
        prepareSheet(sheet);

        writeMergedTitle(sheet, 0, 1, "Thống kê bệnh nhân theo tháng", styles.titleStyle);
        writeLabelValueRow(sheet, 2, "Năm báo cáo", String.valueOf(year), styles.labelStyle, styles.textStyle);
        writeLabelValueRow(
                sheet,
                3,
                "Thời gian xuất",
                exportedAt.format(REPORT_DATE_TIME_FORMATTER),
                styles.labelStyle,
                styles.textStyle
        );
        writeHeaderRow(sheet, 5, styles.headerStyle, "Tháng", "Số bệnh nhân");

        long totalPatientsByMonth = 0;
        for (int i = 0; i < monthlyPatients.size(); i++) {
            MonthlyPatientResponse item = monthlyPatients.get(i);
            Row row = sheet.createRow(6 + i);
            setTextCell(row, 0, getMonthLabel(i + 1), styles.textStyle);
            setLongCell(row, 1, item.getTotal(), styles.numberStyle);
            totalPatientsByMonth += item.getTotal();
        }

        writeMetricValueRow(sheet, 19, "Lũy kế bệnh nhân theo 12 tháng", totalPatientsByMonth, styles.numberStyle, styles);
        writeMetricValueRow(
                sheet,
                20,
                "Bình quân bệnh nhân / tháng",
                monthlyPatients.isEmpty() ? 0.0 : (double) totalPatientsByMonth / monthlyPatients.size(),
                styles.decimalStyle,
                styles
        );

        sheet.createFreezePane(0, 6);
        sheet.setAutoFilter(new CellRangeAddress(5, 17, 0, 1));
        autoSize(sheet, 2);
    }

    private void writeMonthlyRevenueSheet(
            Workbook workbook,
            DashboardReportStyles styles,
            List<MonthlyRevenueResponse> monthlyRevenue,
            int year,
            LocalDateTime exportedAt
    ) {
        Sheet sheet = workbook.createSheet("Doanh thu theo tháng");
        prepareSheet(sheet);

        writeMergedTitle(sheet, 0, 1, "Thống kê doanh thu theo tháng", styles.titleStyle);
        writeLabelValueRow(sheet, 2, "Năm báo cáo", String.valueOf(year), styles.labelStyle, styles.textStyle);
        writeLabelValueRow(
                sheet,
                3,
                "Thời gian xuất",
                exportedAt.format(REPORT_DATE_TIME_FORMATTER),
                styles.labelStyle,
                styles.textStyle
        );
        writeHeaderRow(sheet, 5, styles.headerStyle, "Tháng", "Doanh thu");

        double totalRevenue = 0.0;
        for (int i = 0; i < monthlyRevenue.size(); i++) {
            MonthlyRevenueResponse item = monthlyRevenue.get(i);
            Row row = sheet.createRow(6 + i);
            setTextCell(row, 0, getMonthLabel(i + 1), styles.textStyle);
            setDoubleCell(row, 1, safeDouble(item.getRevenue()), styles.currencyStyle);
            totalRevenue += safeDouble(item.getRevenue());
        }

        writeMetricValueRow(sheet, 19, "Tổng doanh thu 12 tháng", totalRevenue, styles.currencyStyle, styles);
        writeMetricValueRow(
                sheet,
                20,
                "Bình quân doanh thu / tháng",
                monthlyRevenue.isEmpty() ? 0.0 : totalRevenue / monthlyRevenue.size(),
                styles.currencyStyle,
                styles
        );

        sheet.createFreezePane(0, 6);
        sheet.setAutoFilter(new CellRangeAddress(5, 17, 0, 1));
        autoSize(sheet, 2);
    }

    private void writeRecentAppointmentsSheet(
            Workbook workbook,
            DashboardReportStyles styles,
            List<RecentAppointmentResponse> recentAppointments,
            int year,
            LocalDateTime exportedAt
    ) {
        Sheet sheet = workbook.createSheet("Lịch hẹn gần đây");
        prepareSheet(sheet);

        writeMergedTitle(sheet, 0, 7, "Danh sách lịch hẹn gần đây", styles.titleStyle);
        writeLabelValueRow(sheet, 2, "Năm báo cáo", String.valueOf(year), styles.labelStyle, styles.textStyle);
        writeLabelValueRow(
                sheet,
                3,
                "Thời gian xuất",
                exportedAt.format(REPORT_DATE_TIME_FORMATTER),
                styles.labelStyle,
                styles.textStyle
        );
        writeHeaderRow(
                sheet,
                5,
                styles.headerStyle,
                "Bệnh nhân",
                "Bác sĩ",
                "Chuyên khoa",
                "Ngày khám",
                "Giờ khám",
                "Trạng thái",
                "Mã trạng thái",
                "Thời điểm lịch hẹn"
        );

        int rowIndex = 6;
        if (recentAppointments == null || recentAppointments.isEmpty()) {
            writeMergedNote(sheet, rowIndex, 7, "Không có lịch hẹn gần đây để hiển thị.", styles.noteStyle);
        } else {
            for (RecentAppointmentResponse item : recentAppointments) {
                Row row = sheet.createRow(rowIndex++);
                setTextCell(row, 0, safeText(item.getPatientName()), styles.textStyle);
                setTextCell(row, 1, safeText(item.getDoctorName()), styles.textStyle);
                setTextCell(row, 2, safeText(item.getSpecialtyName()), styles.textStyle);
                setTextCell(row, 3, formatDisplayDate(item.getDate()), styles.textStyle);
                setTextCell(row, 4, safeText(item.getTime()), styles.centerTextStyle);
                setTextCell(row, 5, safeText(item.getStatus()), styles.textStyle);
                setTextCell(row, 6, safeText(item.getStatusCode()), styles.centerTextStyle);
                setTextCell(row, 7, formatDisplayDateTime(item.getAppointmentDateTime()), styles.textStyle);
            }
            sheet.setAutoFilter(new CellRangeAddress(5, rowIndex - 1, 0, 7));
        }

        sheet.createFreezePane(0, 6);
        autoSize(sheet, 8);
    }

    private DashboardReportStyles createReportStyles(Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        Font titleFont = workbook.createFont();
        titleFont.setFontName("Arial");
        titleFont.setBold(true);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleFont.setFontHeightInPoints((short) 15);

        Font sectionFont = workbook.createFont();
        sectionFont.setFontName("Arial");
        sectionFont.setBold(true);
        sectionFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        sectionFont.setFontHeightInPoints((short) 12);

        Font headerFont = workbook.createFont();
        headerFont.setFontName("Arial");
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        Font labelFont = workbook.createFont();
        labelFont.setFontName("Arial");
        labelFont.setBold(true);

        Font bodyFont = workbook.createFont();
        bodyFont.setFontName("Arial");

        Font noteFont = workbook.createFont();
        noteFont.setFontName("Arial");
        noteFont.setItalic(true);
        noteFont.setColor(IndexedColors.GREY_80_PERCENT.getIndex());

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle sectionStyle = workbook.createCellStyle();
        sectionStyle.setFont(sectionFont);
        sectionStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sectionStyle.setAlignment(HorizontalAlignment.LEFT);
        sectionStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle headerStyle = workbook.createCellStyle();
        applyBorders(headerStyle);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);

        CellStyle labelStyle = workbook.createCellStyle();
        applyBorders(labelStyle);
        labelStyle.setFont(labelFont);
        labelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        labelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        labelStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle textStyle = workbook.createCellStyle();
        applyBorders(textStyle);
        textStyle.setFont(bodyFont);
        textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        textStyle.setWrapText(true);

        CellStyle centerTextStyle = workbook.createCellStyle();
        centerTextStyle.cloneStyleFrom(textStyle);
        centerTextStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(textStyle);
        numberStyle.setDataFormat(dataFormat.getFormat("#,##0"));

        CellStyle signedNumberStyle = workbook.createCellStyle();
        signedNumberStyle.cloneStyleFrom(textStyle);
        signedNumberStyle.setAlignment(HorizontalAlignment.CENTER);
        signedNumberStyle.setDataFormat(dataFormat.getFormat("+0;-0;0"));

        CellStyle decimalStyle = workbook.createCellStyle();
        decimalStyle.cloneStyleFrom(textStyle);
        decimalStyle.setDataFormat(dataFormat.getFormat("#,##0.0"));

        CellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.cloneStyleFrom(textStyle);
        currencyStyle.setDataFormat(dataFormat.getFormat("#,##0 \"VND\""));

        CellStyle signedPercentStyle = workbook.createCellStyle();
        signedPercentStyle.cloneStyleFrom(textStyle);
        signedPercentStyle.setAlignment(HorizontalAlignment.CENTER);
        signedPercentStyle.setDataFormat(dataFormat.getFormat("+0.0%;-0.0%;0.0%"));

        CellStyle noteStyle = workbook.createCellStyle();
        noteStyle.cloneStyleFrom(textStyle);
        noteStyle.setFont(noteFont);
        noteStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        noteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return new DashboardReportStyles(
                titleStyle,
                sectionStyle,
                headerStyle,
                labelStyle,
                textStyle,
                centerTextStyle,
                numberStyle,
                signedNumberStyle,
                decimalStyle,
                currencyStyle,
                signedPercentStyle,
                noteStyle
        );
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void prepareSheet(Sheet sheet) {
        sheet.setDisplayGridlines(false);
        sheet.setDefaultColumnWidth(18);
    }

    private void writeMergedTitle(Sheet sheet, int rowIndex, int lastColumn, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(24f);
        for (int i = 0; i <= lastColumn; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(style);
            if (i == 0) {
                cell.setCellValue(value);
            }
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastColumn));
    }

    private void writeLabelValueRow(
            Sheet sheet,
            int rowIndex,
            String label,
            String value,
            CellStyle labelStyle,
            CellStyle valueStyle
    ) {
        Row row = sheet.createRow(rowIndex);
        setTextCell(row, 0, label, labelStyle);
        setTextCell(row, 1, value, valueStyle);
    }

    private void writeHeaderRow(Sheet sheet, int rowIndex, CellStyle style, String... headers) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(20f);
        for (int i = 0; i < headers.length; i++) {
            setTextCell(row, i, headers[i], style);
        }
    }

    private void writeMetricRow(
            Sheet sheet,
            int rowIndex,
            String label,
            long value,
            CellStyle valueStyle,
            String description,
            DashboardReportStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        setTextCell(row, 0, label, styles.textStyle);
        setLongCell(row, 1, value, valueStyle);
        setTextCell(row, 2, description, styles.textStyle);
    }

    private void writeMetricRow(
            Sheet sheet,
            int rowIndex,
            String label,
            double value,
            CellStyle valueStyle,
            String description,
            DashboardReportStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        setTextCell(row, 0, label, styles.textStyle);
        setDoubleCell(row, 1, value, valueStyle);
        setTextCell(row, 2, description, styles.textStyle);
    }

    private void writeMetricValueRow(
            Sheet sheet,
            int rowIndex,
            String label,
            long value,
            CellStyle valueStyle,
            DashboardReportStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        setTextCell(row, 0, label, styles.textStyle);
        setLongCell(row, 1, value, valueStyle);
    }

    private void writeMetricValueRow(
            Sheet sheet,
            int rowIndex,
            String label,
            double value,
            CellStyle valueStyle,
            DashboardReportStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        setTextCell(row, 0, label, styles.textStyle);
        setDoubleCell(row, 1, value, valueStyle);
    }

    private void writeMetricValueRow(
            Sheet sheet,
            int rowIndex,
            String label,
            String value,
            CellStyle valueStyle,
            DashboardReportStyles styles
    ) {
        Row row = sheet.createRow(rowIndex);
        setTextCell(row, 0, label, styles.textStyle);
        setTextCell(row, 1, value, valueStyle);
    }

    private void writeMergedNote(Sheet sheet, int rowIndex, int lastColumn, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(32f);
        for (int i = 0; i <= lastColumn; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(style);
            if (i == 0) {
                cell.setCellValue(value);
            }
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastColumn));
    }

    private void setTextCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        cell.setCellValue(value == null ? "" : value);
    }

    private void setLongCell(Row row, int columnIndex, long value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }

    private void setDoubleCell(Row row, int columnIndex, double value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int paddedWidth = Math.min(sheet.getColumnWidth(i) + 1024, 20000);
            sheet.setColumnWidth(i, paddedWidth);
        }
    }

    private double calculateGrowthRatio(double currentValue, double previousValue) {
        if (previousValue <= 0) {
            return currentValue > 0 ? 1.0 : 0.0;
        }
        return (currentValue - previousValue) / previousValue;
    }

    private String getMonthLabel(int month) {
        return "Tháng " + month;
    }

    private String buildPeakText(int month, long value, String suffix) {
        if (value <= 0) {
            return "Không có dữ liệu";
        }
        return getMonthLabel(month) + " (" + formatWholeNumber(value) + " " + suffix + ")";
    }

    private String buildPeakRevenueText(int month, double value) {
        if (value <= 0) {
            return "Không có dữ liệu";
        }
        return getMonthLabel(month) + " (" + formatWholeNumber(value) + " VND)";
    }

    private String formatWholeNumber(double value) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(VIETNAMESE_LOCALE);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        return numberFormat.format(value);
    }

    private String formatDisplayDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return "";
        }
        try {
            return LocalDate.parse(rawDate).format(REPORT_DATE_FORMATTER);
        } catch (Exception ignored) {
            return rawDate;
        }
    }

    private String formatDisplayDateTime(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isBlank()) {
            return "";
        }
        try {
            return LocalDateTime.parse(rawDateTime).format(REPORT_DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return rawDateTime;
        }
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

    private record DashboardReportStyles(
            CellStyle titleStyle,
            CellStyle sectionStyle,
            CellStyle headerStyle,
            CellStyle labelStyle,
            CellStyle textStyle,
            CellStyle centerTextStyle,
            CellStyle numberStyle,
            CellStyle signedNumberStyle,
            CellStyle decimalStyle,
            CellStyle currencyStyle,
            CellStyle signedPercentStyle,
            CellStyle noteStyle
    ) {
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }
}
