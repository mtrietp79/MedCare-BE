package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.config.VNPayConfig;
import com.medcare.clinic_backend.dto.feedback.MessageResponse;
import com.medcare.clinic_backend.dto.servicepackage.*;
import com.medcare.clinic_backend.entity.*;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServicePackageService {

    private static final Set<String> ADMIN_ALLOWED_BOOKING_STATUSES = Set.of(
            "PENDING_PAYMENT",
            "PAID",
            "RECEIVED",
            "COMPLETED",
            "CANCELLED"
    );

    @Autowired
    private ServicePackageRepository servicePackageRepository;

    @Autowired
    private MedicalServiceRepository medicalServiceRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ServicePackageBookingRepository servicePackageBookingRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Transactional(readOnly = true)
    public List<PublicServicePackageResponse> getActivePublicPackages() {
        return servicePackageRepository.findByIsActiveTrueOrderByIdDesc().stream()
                .map(this::toPublicResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PublicServicePackageDetailResponse getPublicPackageDetail(Integer id) {
        ServicePackage servicePackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay goi dich vu ID: " + id));
        if (!Boolean.TRUE.equals(servicePackage.getIsActive())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Goi dich vu khong ton tai hoac dang tam an.");
        }
        return toPublicDetailResponse(servicePackage);
    }

    @Transactional(readOnly = true)
    public List<AdminServicePackageResponse> getAllForAdmin() {
        return getAllForAdmin(null, null, null);
    }

    @Transactional(readOnly = true)
    public List<AdminServicePackageResponse> getAllForAdmin(String keyword, Boolean active, Boolean configured) {
        List<ServicePackageBooking> allBookings = servicePackageBookingRepository.findAll();
        Map<Integer, PackageBookingStats> bookingStatsByPackageId = buildBookingStatsByPackageId(allBookings);
        String normalizedKeyword = normalizeKeyword(keyword);

        return servicePackageRepository.findAll().stream()
                .sorted(Comparator.comparing(ServicePackage::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(servicePackage -> matchesAdminPackageKeyword(servicePackage, normalizedKeyword))
                .filter(servicePackage -> matchesAdminPackageActive(servicePackage, active))
                .filter(servicePackage -> matchesAdminPackageConfigured(servicePackage, configured))
                .map(servicePackage -> toAdminResponse(servicePackage, bookingStatsByPackageId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminServicePackageSummaryResponse getAdminSummary() {
        List<ServicePackage> packages = servicePackageRepository.findAll();
        Map<Integer, PackageBookingStats> bookingStatsByPackageId =
                buildBookingStatsByPackageId(servicePackageBookingRepository.findAll());

        long totalPackages = packages.size();
        long activePackages = packages.stream()
                .filter(servicePackage -> Boolean.TRUE.equals(servicePackage.getIsActive()))
                .count();
        long inactivePackages = totalPackages - activePackages;
        long packagesWithBookings = packages.stream()
                .filter(servicePackage -> hasPackageBookings(servicePackage, bookingStatsByPackageId))
                .count();
        long packagesWithoutItems = packages.stream()
                .filter(servicePackage -> countPackageItems(servicePackage) == 0)
                .count();

        return new AdminServicePackageSummaryResponse(
                totalPackages,
                activePackages,
                inactivePackages,
                packagesWithBookings,
                packagesWithoutItems
        );
    }

    @Transactional(readOnly = true)
    public AdminServicePackageResponse getByIdForAdmin(Integer id) {
        ServicePackage servicePackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay goi dich vu ID: " + id));
        Map<Integer, PackageBookingStats> bookingStatsByPackageId =
                buildBookingStatsByPackageId(servicePackageBookingRepository.findAll());
        return toAdminResponse(servicePackage, bookingStatsByPackageId);
    }

    @Transactional
    public AdminServicePackageResponse createForAdmin(AdminServicePackageRequest request) {
        ServicePackage servicePackage = new ServicePackage();
        applyRequest(servicePackage, request);
        ServicePackage saved = servicePackageRepository.save(servicePackage);
        return toAdminResponse(saved, Map.of());
    }

    @Transactional
    public AdminServicePackageResponse updateForAdmin(Integer id, AdminServicePackageRequest request) {
        ServicePackage servicePackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay goi dich vu ID: " + id));
        applyRequest(servicePackage, request);
        ServicePackage saved = servicePackageRepository.save(servicePackage);
        return toAdminResponse(saved, Map.of());
    }

    @Transactional
    public AdminServicePackageResponse setActiveForAdmin(Integer id, Boolean active) {
        if (active == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "active khong duoc de trong.");
        }
        ServicePackage servicePackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay goi dich vu ID: " + id));
        servicePackage.setIsActive(active);
        ServicePackage saved = servicePackageRepository.save(servicePackage);
        Map<Integer, PackageBookingStats> bookingStatsByPackageId =
                buildBookingStatsByPackageId(servicePackageBookingRepository.findAll());
        return toAdminResponse(saved, bookingStatsByPackageId);
    }

    @Transactional
    public MessageResponse deleteForAdmin(Integer id) {
        if (!servicePackageRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay goi dich vu ID: " + id);
        }
        if (servicePackageBookingRepository.existsByServicePackage_Id(id)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Khong the xoa goi dich vu da co booking. Vui long chuyen sang tam ngung."
            );
        }
        servicePackageRepository.deleteById(id);
        return new MessageResponse("Da xoa goi dich vu.");
    }

    @Transactional
    public ServicePackageBookingResponse bookPackage(
            String username,
            ServicePackageBookingRequest request,
            HttpServletRequest httpServletRequest
    ) {
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu dat goi dich vu khong hop le.");
        }
        if (request.getPackageId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "packageId khong duoc de trong.");
        }
        if (request.getBookingDate() == null || request.getBookingTime() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui long chon ngay gio den co so.");
        }

        Patient patient = patientRepository.findByAccount_Username(username)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan cua ban chua duoc lien ket voi ho so benh nhan."
                ));
        patientService.ensureProfileCompleted(patient.getId());

        ServicePackage servicePackage = servicePackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay goi dich vu ID: " + request.getPackageId()));
        if (!Boolean.TRUE.equals(servicePackage.getIsActive())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu nay dang tam ngung.");
        }
        if (servicePackage.getPrice() == null || servicePackage.getPrice() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu chua co gia hop le.");
        }

        LocalDateTime bookingDateTime = LocalDateTime.of(request.getBookingDate(), request.getBookingTime());
        if (bookingDateTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong the dat o thoi diem da qua.");
        }

        ServicePackageBooking booking = new ServicePackageBooking();
        booking.setPatient(patient);
        booking.setServicePackage(servicePackage);
        booking.setBookingDate(request.getBookingDate());
        booking.setBookingTime(request.getBookingTime());
        booking.setNote(trimToNull(request.getNote()));
        booking.setTotalAmount(servicePackage.getPrice());
        booking.setPaymentStatus("PENDING");
        booking.setStatus("PENDING_PAYMENT");

        ServicePackageBooking saved = servicePackageBookingRepository.save(booking);
        saved.setBookingCode(generateBookingCode(saved.getId()));
        saved = servicePackageBookingRepository.save(saved);

        String ipAddress = httpServletRequest == null ? "127.0.0.1" : VNPayConfig.getIpAddress(httpServletRequest);
        paymentService.createPendingTransactionForServicePackage(saved.getId(), saved.getTotalAmount());
        String paymentUrl = paymentService.createServicePackagePaymentUrl(saved.getId(), ipAddress, username);

        return new ServicePackageBookingResponse(
                saved.getId(),
                saved.getBookingCode(),
                paymentUrl,
                "Tao dat goi dich vu thanh cong, vui long thanh toan"
        );
    }

    @Transactional(readOnly = true)
    public List<ServicePackageBookingListItemResponse> getPatientBookings(String username) {
        Patient patient = patientRepository.findByAccount_Username(username)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan cua ban chua duoc lien ket voi ho so benh nhan."
                ));

        return servicePackageBookingRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(this::toPatientBookingListItem)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServicePackageBookingDetailResponse getPatientBookingDetail(String username, Integer bookingId) {
        Patient patient = patientRepository.findByAccount_Username(username)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan cua ban chua duoc lien ket voi ho so benh nhan."
                ));

        ServicePackageBooking booking = servicePackageBookingRepository.findByIdAndPatientId(bookingId, patient.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay dat goi dich vu ID: " + bookingId));

        return toPatientBookingDetail(booking);
    }

    @Transactional(readOnly = true)
    public List<AdminServicePackageBookingResponse> getAllBookingsForAdmin(String status, String keyword) {
        String normalizedStatus = normalizeBookingStatus(trimToNull(status));
        String normalizedKeyword = normalizeKeyword(keyword);

        return servicePackageBookingRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(booking -> normalizedStatus == null || normalizedStatus.equals(normalizeBookingStatus(booking.getStatus())))
                .filter(booking -> matchesKeyword(booking, normalizedKeyword))
                .map(this::toAdminBookingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminServicePackageBookingResponse updateBookingStatusForAdmin(Integer bookingId, String status) {
        String normalized = normalizeBookingStatus(trimToNull(status));
        if (normalized == null || !ADMIN_ALLOWED_BOOKING_STATUSES.contains(normalized)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Trang thai khong hop le. Chi cho phep: PENDING_PAYMENT, PAID, RECEIVED, COMPLETED, CANCELLED."
            );
        }

        ServicePackageBooking booking = servicePackageBookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay dat goi dich vu ID: " + bookingId));

        booking.setStatus(normalized);

        String currentPaymentStatus = normalizePaymentStatus(booking.getPaymentStatus());
        if ("PAID".equals(normalized)) {
            booking.setPaymentStatus("PAID");
        } else if ("PENDING_PAYMENT".equals(normalized)) {
            booking.setPaymentStatus("PENDING");
        } else if ("CANCELLED".equals(normalized)) {
            booking.setPaymentStatus("CANCELLED");
        } else if (("RECEIVED".equals(normalized) || "COMPLETED".equals(normalized)) && !"PAID".equals(currentPaymentStatus)) {
            booking.setPaymentStatus("PAID");
        }

        return toAdminBookingResponse(servicePackageBookingRepository.save(booking));
    }

    private void applyRequest(ServicePackage servicePackage, AdminServicePackageRequest request) {
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu goi dich vu khong hop le.");
        }
        String normalizedName = trimToNull(request.getName());
        if (normalizedName == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ten goi dich vu khong duoc de trong.");
        }
        if (request.getPrice() == null || request.getPrice() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Gia goi dich vu khong hop le.");
        }
        if (request.getDurationMinutes() != null && request.getDurationMinutes() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thoi luong du kien khong hop le.");
        }

        servicePackage.setName(normalizedName);
        servicePackage.setDescription(trimToNull(request.getDescription()));
        servicePackage.setPrice(request.getPrice());
        servicePackage.setDurationMinutes(request.getDurationMinutes());
        servicePackage.setImageUrl(trimToNull(request.getImageUrl()));
        servicePackage.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());

        List<ServicePackageItem> resolvedItems = new ArrayList<>();
        if (request.getMedicalServiceIds() != null) {
            Set<Integer> uniqueMedicalServiceIds = new LinkedHashSet<>(request.getMedicalServiceIds());
            for (Integer medicalServiceId : uniqueMedicalServiceIds) {
                if (medicalServiceId == null) {
                    continue;
                }
                MedicalService medicalService = medicalServiceRepository.findById(medicalServiceId)
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay medical_service ID: " + medicalServiceId));
                ServicePackageItem item = new ServicePackageItem();
                item.setServicePackage(servicePackage);
                item.setMedicalService(medicalService);
                resolvedItems.add(item);
            }
        }

        servicePackage.getItems().clear();
        servicePackage.getItems().addAll(resolvedItems);
    }

    private PublicServicePackageResponse toPublicResponse(ServicePackage servicePackage) {
        if (servicePackage == null) {
            return new PublicServicePackageResponse(null, null, null, null, null, null);
        }
        return new PublicServicePackageResponse(
                servicePackage.getId(),
                servicePackage.getName(),
                servicePackage.getDescription(),
                servicePackage.getPrice(),
                servicePackage.getDurationMinutes(),
                servicePackage.getImageUrl()
        );
    }

    private PublicServicePackageDetailResponse toPublicDetailResponse(ServicePackage servicePackage) {
        List<PublicServicePackageDetailResponse.Item> items = servicePackage.getItems() == null
                ? List.of()
                : servicePackage.getItems().stream()
                .filter(Objects::nonNull)
                .map(item -> new PublicServicePackageDetailResponse.Item(
                        item.getMedicalService() == null ? null : item.getMedicalService().getId(),
                        item.getMedicalService() == null ? null : item.getMedicalService().getName(),
                        item.getMedicalService() == null ? null : item.getMedicalService().getPrice()
                ))
                .collect(Collectors.toList());

        return new PublicServicePackageDetailResponse(
                servicePackage.getId(),
                servicePackage.getName(),
                servicePackage.getDescription(),
                servicePackage.getPrice(),
                servicePackage.getDurationMinutes(),
                servicePackage.getImageUrl(),
                items
        );
    }

    private AdminServicePackageResponse toAdminResponse(
            ServicePackage servicePackage,
            Map<Integer, PackageBookingStats> bookingStatsByPackageId
    ) {
        PublicServicePackageDetailResponse detail = toPublicDetailResponse(servicePackage);
        PackageBookingStats stats = bookingStatsByPackageId.getOrDefault(
                servicePackage == null ? null : servicePackage.getId(),
                PackageBookingStats.empty()
        );
        return new AdminServicePackageResponse(
                servicePackage.getId(),
                servicePackage.getName(),
                servicePackage.getDescription(),
                servicePackage.getPrice(),
                servicePackage.getDurationMinutes(),
                servicePackage.getImageUrl(),
                servicePackage.getIsActive(),
                servicePackage.getCreatedAt(),
                servicePackage.getUpdatedAt(),
                stats.totalBooked(),
                stats.totalCompleted(),
                stats.totalPaid(),
                stats.totalPending(),
                countPackageItems(servicePackage),
                resolvePackageStatus(servicePackage),
                resolvePackageStatusDisplay(servicePackage),
                hasPackageBookings(servicePackage, bookingStatsByPackageId),
                !hasPackageBookings(servicePackage, bookingStatsByPackageId),
                detail.getItems()
        );
    }

    private ServicePackageBookingListItemResponse toPatientBookingListItem(ServicePackageBooking booking) {
        return new ServicePackageBookingListItemResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getServicePackage() == null ? null : booking.getServicePackage().getName(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getTotalAmount(),
                normalizePaymentStatus(booking.getPaymentStatus()),
                normalizeBookingStatus(booking.getStatus())
        );
    }

    private ServicePackageBookingDetailResponse toPatientBookingDetail(ServicePackageBooking booking) {
        Patient patient = booking.getPatient();
        ServicePackage servicePackage = booking.getServicePackage();

        ServicePackageBookingDetailResponse.PatientInfo patientInfo =
                patient == null ? null : new ServicePackageBookingDetailResponse.PatientInfo(
                        patient.getId(),
                        patient.getFullName(),
                        patient.getPhone(),
                        patient.getEmail()
                );

        ServicePackageBookingDetailResponse.PackageInfo packageInfo =
                servicePackage == null ? null : new ServicePackageBookingDetailResponse.PackageInfo(
                        servicePackage.getId(),
                        servicePackage.getName(),
                        servicePackage.getDescription(),
                        servicePackage.getPrice(),
                        servicePackage.getDurationMinutes(),
                        servicePackage.getImageUrl()
                );

        TransactionLog latestPaidLog = transactionLogRepository
                .findTopByServicePackageBookingIdAndResponseCodeOrderByCreatedAtDesc(booking.getId(), "00");
        String invoiceCode = latestPaidLog == null ? null : latestPaidLog.getVnpTransactionNo();
        boolean canPayOnline = !"PAID".equals(normalizePaymentStatus(booking.getPaymentStatus()))
                && !"CANCELLED".equals(normalizePaymentStatus(booking.getPaymentStatus()))
                && booking.getTotalAmount() != null
                && booking.getTotalAmount() > 0;

        return new ServicePackageBookingDetailResponse(
                booking.getId(),
                booking.getBookingCode(),
                patientInfo,
                packageInfo,
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getNote(),
                booking.getTotalAmount(),
                "SERVICE_PACKAGE",
                "H\u00f3a \u0111\u01a1n g\u00f3i d\u1ecbch v\u1ee5",
                normalizePaymentStatus(booking.getPaymentStatus()),
                normalizeBookingStatus(booking.getStatus()),
                invoiceCode,
                canPayOnline,
                latestPaidLog == null ? null : latestPaidLog.getCreatedAt(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    private AdminServicePackageBookingResponse toAdminBookingResponse(ServicePackageBooking booking) {
        Patient patient = booking.getPatient();
        ServicePackage servicePackage = booking.getServicePackage();
        String normalizedPaymentStatus = normalizePaymentStatus(booking.getPaymentStatus());
        Double amount = booking.getTotalAmount() == null ? 0.0 : booking.getTotalAmount();
        Double paidAmount = "PAID".equals(normalizedPaymentStatus) ? amount : 0.0;
        return new AdminServicePackageBookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                patient == null ? null : patient.getFullName(),
                patient == null ? null : patient.getPhone(),
                servicePackage == null ? null : servicePackage.getName(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                amount,
                paidAmount,
                normalizedPaymentStatus,
                normalizeBookingStatus(booking.getStatus()),
                booking.getCreatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeKeyword(String keyword) {
        String trimmed = trimToNull(keyword);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private boolean matchesKeyword(ServicePackageBooking booking, String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return true;
        }
        String bookingCode = safeLower(booking.getBookingCode());
        String patientName = safeLower(booking.getPatient() == null ? null : booking.getPatient().getFullName());
        String patientPhone = safeLower(booking.getPatient() == null ? null : booking.getPatient().getPhone());
        String packageName = safeLower(booking.getServicePackage() == null ? null : booking.getServicePackage().getName());
        return bookingCode.contains(normalizedKeyword)
                || patientName.contains(normalizedKeyword)
                || patientPhone.contains(normalizedKeyword)
                || packageName.contains(normalizedKeyword);
    }

    private boolean matchesAdminPackageKeyword(ServicePackage servicePackage, String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return true;
        }
        if (servicePackage == null) {
            return false;
        }
        if (safeLower(servicePackage.getName()).contains(normalizedKeyword)
                || safeLower(servicePackage.getDescription()).contains(normalizedKeyword)) {
            return true;
        }
        return servicePackage.getItems() != null && servicePackage.getItems().stream()
                .filter(Objects::nonNull)
                .map(ServicePackageItem::getMedicalService)
                .filter(Objects::nonNull)
                .map(MedicalService::getName)
                .anyMatch(name -> safeLower(name).contains(normalizedKeyword));
    }

    private boolean matchesAdminPackageActive(ServicePackage servicePackage, Boolean active) {
        if (active == null) {
            return true;
        }
        return servicePackage != null && Objects.equals(Boolean.TRUE.equals(servicePackage.getIsActive()), active);
    }

    private boolean matchesAdminPackageConfigured(ServicePackage servicePackage, Boolean configured) {
        if (configured == null) {
            return true;
        }
        boolean hasItems = countPackageItems(servicePackage) > 0;
        return configured.equals(hasItems);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private int countPackageItems(ServicePackage servicePackage) {
        if (servicePackage == null || servicePackage.getItems() == null) {
            return 0;
        }
        return (int) servicePackage.getItems().stream()
                .filter(Objects::nonNull)
                .count();
    }

    private String resolvePackageStatus(ServicePackage servicePackage) {
        return Boolean.TRUE.equals(servicePackage == null ? null : servicePackage.getIsActive())
                ? "ACTIVE"
                : "INACTIVE";
    }

    private String resolvePackageStatusDisplay(ServicePackage servicePackage) {
        return "ACTIVE".equals(resolvePackageStatus(servicePackage))
                ? "Dang hoat dong"
                : "Tam ngung";
    }

    private boolean hasPackageBookings(
            ServicePackage servicePackage,
            Map<Integer, PackageBookingStats> bookingStatsByPackageId
    ) {
        if (servicePackage == null || servicePackage.getId() == null || bookingStatsByPackageId == null) {
            return false;
        }
        PackageBookingStats stats = bookingStatsByPackageId.get(servicePackage.getId());
        return stats != null && stats.totalHistory() > 0;
    }

    private String normalizeBookingStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "PENDING_PAYMENT";
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (ADMIN_ALLOWED_BOOKING_STATUSES.contains(upper)) {
            return upper;
        }
        if (upper.contains("CHO") || upper.contains("PENDING")) {
            return "PENDING_PAYMENT";
        }
        if (upper.contains("THANH TOAN") || upper.contains("PAID")) {
            return "PAID";
        }
        if (upper.contains("TIEP NHAN") || upper.contains("RECEIVED")) {
            return "RECEIVED";
        }
        if (upper.contains("HOAN THANH") || upper.contains("COMPLETED")) {
            return "COMPLETED";
        }
        if (upper.contains("HUY") || upper.contains("CANCEL")) {
            return "CANCELLED";
        }
        return upper;
    }

    private String normalizePaymentStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "PENDING";
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.contains("PAID")) {
            return "PAID";
        }
        if (upper.contains("FAIL")) {
            return "FAILED";
        }
        if (upper.contains("CANCEL")) {
            return "CANCELLED";
        }
        if (upper.contains("PENDING")) {
            return "PENDING";
        }
        return upper;
    }

    private String generateBookingCode(Integer bookingId) {
        if (bookingId == null) {
            return null;
        }
        return String.format("PKG%06d", bookingId);
    }

    private Map<Integer, PackageBookingStats> buildBookingStatsByPackageId(List<ServicePackageBooking> bookings) {
        Map<Integer, MutablePackageBookingStats> mutableStats = new HashMap<>();
        if (bookings == null || bookings.isEmpty()) {
            return Map.of();
        }

        for (ServicePackageBooking booking : bookings) {
            if (booking == null || booking.getServicePackage() == null || booking.getServicePackage().getId() == null) {
                continue;
            }
            Integer packageId = booking.getServicePackage().getId();
            MutablePackageBookingStats bucket = mutableStats.computeIfAbsent(packageId, key -> new MutablePackageBookingStats());
            bucket.totalHistory++;

            String normalizedStatus = normalizeBookingStatus(booking.getStatus());
            String normalizedPaymentStatus = normalizePaymentStatus(booking.getPaymentStatus());
            if (!"CANCELLED".equals(normalizedStatus)) {
                bucket.totalBooked++;
            }
            if ("COMPLETED".equals(normalizedStatus)) {
                bucket.totalCompleted++;
            }
            if ("PAID".equals(normalizedPaymentStatus)) {
                bucket.totalPaid++;
            }
            if ("PENDING_PAYMENT".equals(normalizedStatus)) {
                bucket.totalPending++;
            }
        }

        Map<Integer, PackageBookingStats> finalizedStats = new HashMap<>();
        for (Map.Entry<Integer, MutablePackageBookingStats> entry : mutableStats.entrySet()) {
            MutablePackageBookingStats value = entry.getValue();
            finalizedStats.put(entry.getKey(), new PackageBookingStats(
                    value.totalHistory,
                    value.totalBooked,
                    value.totalCompleted,
                    value.totalPaid,
                    value.totalPending
            ));
        }
        return finalizedStats;
    }

    private record PackageBookingStats(Long totalHistory, Long totalBooked, Long totalCompleted, Long totalPaid, Long totalPending) {
        private static PackageBookingStats empty() {
            return new PackageBookingStats(0L, 0L, 0L, 0L, 0L);
        }
    }

    private static class MutablePackageBookingStats {
        private long totalHistory = 0L;
        private long totalBooked = 0L;
        private long totalCompleted = 0L;
        private long totalPaid = 0L;
        private long totalPending = 0L;
    }
}
