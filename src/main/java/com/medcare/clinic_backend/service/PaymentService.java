package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.config.VNPayConfig;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.ServicePackageBooking;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.repository.ServicePackageBookingRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PaymentService {

    private static final String SERVICE_PACKAGE_TXN_PREFIX = "SPB-";
    private static final String INVOICE_TXN_PREFIX = "INV-";

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ServicePackageBookingRepository servicePackageBookingRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Value("${vnpay.hashSecret}")
    private String secretKey;

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.payUrl}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl}")
    private String vnpReturnUrl;

    public long resolvePaymentAmount(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay lich hen ID: " + appointmentId
                ));

        if (appointment.getConsultationFee() == null || appointment.getConsultationFee() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen chua co phi kham hop le de thanh toan.");
        }

        return Math.round(appointment.getConsultationFee());
    }

    public long resolveServicePackageBookingAmount(Integer bookingId) {
        ServicePackageBooking booking = servicePackageBookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay phieu dich vu ID: " + bookingId
                ));

        if (booking.getTotalAmount() == null || booking.getTotalAmount() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Phieu dich vu chua co tong tien hop le de thanh toan.");
        }

        return Math.round(booking.getTotalAmount());
    }

    public long resolveInvoiceAmount(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay hoa don ID: " + invoiceId
                ));

        if (invoice.getTotalAmount() == null || invoice.getTotalAmount() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Hoa don chua co tong tien hop le de thanh toan.");
        }

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Hoa don nay da duoc thanh toan.");
        }

        return Math.round(invoice.getTotalAmount());
    }

    @Transactional
    public Integer createPendingTransaction(Integer appointmentId, Double amount) {
        if (appointmentId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu appointmentId.");
        }
        TransactionLog log = new TransactionLog();
        log.setAppointmentId(appointmentId);
        log.setServicePackageBookingId(null);
        log.setInvoiceId(null);
        log.setVnpTxnRef(String.valueOf(appointmentId));
        log.setAmount(amount == null ? 0.0 : amount);
        log.setResponseCode("PENDING");
        return transactionLogRepository.save(log).getId();
    }

    @Transactional
    public Integer createPendingTransactionForServicePackage(Integer bookingId, Double amount) {
        if (bookingId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu bookingId.");
        }
        TransactionLog log = new TransactionLog();
        log.setAppointmentId(null);
        log.setServicePackageBookingId(bookingId);
        log.setInvoiceId(null);
        log.setVnpTxnRef(servicePackageTxnRef(bookingId));
        log.setAmount(amount == null ? 0.0 : amount);
        log.setResponseCode("PENDING");
        return transactionLogRepository.save(log).getId();
    }

    public String createPaymentUrl(Integer appointmentId, String ipAddress, String username) {
        assertVnpayConfiguration();
        if (appointmentId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu appointmentId.");
        }

        Patient patient = getCurrentPatientOrThrow(username);
        Appointment appointment = appointmentRepository.findByIdAndPatientId(appointmentId, patient.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich hen ID: " + appointmentId));

        String status = appointment.getStatus() == null ? "" : appointment.getStatus().trim().toUpperCase();
        if ("CANCELLED".equals(status)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong the thanh toan lich hen da huy.");
        }
        if ("PAID".equalsIgnoreCase(appointment.getPaymentStatus())
                || "PAID_ONLINE".equalsIgnoreCase(appointment.getPaymentStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen nay da duoc thanh toan.");
        }

        long amount = resolvePaymentAmount(appointmentId);
        return buildPaymentUrl(
                String.valueOf(appointmentId),
                "Thanh toan lich hen ID: " + appointmentId,
                amount,
                withReturnParam("appointmentId", appointmentId),
                ipAddress
        );
    }

    public String createInvoicePaymentUrl(Integer invoiceId, String ipAddress, String username) {
        assertVnpayConfiguration();
        if (invoiceId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu invoiceId.");
        }

        Patient patient = getCurrentPatientOrThrow(username);
        Invoice invoice = invoiceRepository.findByIdAndMedicalRecordPatientId(invoiceId, patient.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay hoa don ID: " + invoiceId));
        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Hoa don nay da duoc thanh toan.");
        }

        long amount = resolveInvoiceAmount(invoiceId);
        return buildPaymentUrl(
                invoiceTxnRef(invoiceId),
                "Thanh toan hoa don kham benh ID: " + invoiceId,
                amount,
                withReturnParam("invoiceId", invoiceId),
                ipAddress
        );
    }

    public String createServicePackagePaymentUrl(Integer bookingId, String ipAddress) {
        assertVnpayConfiguration();
        if (bookingId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu bookingId.");
        }
        ServicePackageBooking booking = servicePackageBookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay phieu dich vu ID: " + bookingId));

        long amount = resolveServicePackageBookingAmount(bookingId);
        String displayCode = booking.getBookingCode() == null ? String.valueOf(bookingId) : booking.getBookingCode();
        return buildPaymentUrl(
                servicePackageTxnRef(bookingId),
                "Thanh toan phieu dich vu: " + displayCode,
                amount,
                withReturnParam("bookingId", bookingId),
                ipAddress
        );
    }

    @Transactional
    public Appointment choosePayAtClinic(Integer appointmentId, String username) {
        throw new BusinessException(HttpStatus.GONE, "He thong da ngung ho tro thanh toan tai phong kham. Vui long thanh toan qua VNPay.");
    }

    @Transactional
    public String processVnpayReturn(Map<String, String> vnpParams, Integer appointmentId) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VNPay hash secret chua duoc cau hinh.");
        }
        if (appointmentId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu appointmentId.");
        }

        String responseCode = vnpParams.getOrDefault("vnp_ResponseCode", "");
        String vnpTxnRef = vnpParams.get("vnp_TxnRef");
        String vnpAmountRaw = vnpParams.get("vnp_Amount");
        String receivedSecureHash = vnpParams.get("vnp_SecureHash");

        if (receivedSecureHash == null || receivedSecureHash.isBlank()) {
            saveTransactionLogForAppointment(vnpParams, appointmentId, "MISSING_SIGNATURE", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu chu ky bao mat tu VNPay.");
        }

        if (vnpTxnRef == null || !vnpTxnRef.equals(String.valueOf(appointmentId))) {
            saveTransactionLogForAppointment(vnpParams, appointmentId, "INVALID_TXN_REF", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thong tin giao dich khong khop voi lich hen.");
        }

        if (!isValidVnpaySignature(vnpParams, receivedSecureHash)) {
            saveTransactionLogForAppointment(vnpParams, appointmentId, "INVALID_SIGNATURE", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chu ky giao dich VNPay khong hop le.");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay lich hen ID: " + appointmentId
                ));

        if (appointment.getConsultationFee() == null || appointment.getConsultationFee() <= 0) {
            saveTransactionLogForAppointment(vnpParams, appointmentId, "INVALID_APPOINTMENT_FEE", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen chua co phi kham hop le.");
        }

        long expectedAmount = Math.round(appointment.getConsultationFee() * 100);
        long paidAmount = parseVnpAmount(vnpAmountRaw);
        if (expectedAmount != paidAmount) {
            saveTransactionLogForAppointment(vnpParams, appointmentId, "AMOUNT_MISMATCH", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So tien thanh toan khong khop voi phi kham.");
        }

        saveTransactionLogForAppointment(vnpParams, appointmentId, responseCode, parseAmount(vnpAmountRaw));

        if ("00".equals(responseCode)) {
            if (!"PAID_ONLINE".equalsIgnoreCase(appointment.getPaymentStatus())) {
                appointment.setPaymentStatus("PAID_ONLINE");
            }
            if ("PENDING_PAYMENT".equalsIgnoreCase(appointment.getStatus())) {
                appointment.setStatus("PENDING");
            }
            appointmentRepository.save(appointment);
            return "THANH TOAN THANH CONG! Trang thai lich hen da duoc cap nhat.";
        }

        if ("24".equals(responseCode)) {
            appointment.setPaymentStatus("CANCELLED");
        } else {
            appointment.setPaymentStatus("FAILED");
            if ("PENDING_PAYMENT".equalsIgnoreCase(appointment.getStatus())) {
                appointment.setStatus("PENDING_PAYMENT");
            }
        }
        appointmentRepository.save(appointment);

        return "THANH TOAN THAT BAI HOAC BI HUY BO. Ma loi VNPay: " + responseCode;
    }

    @Transactional
    public String processServicePackageBookingVnpayReturn(Map<String, String> vnpParams, Integer bookingId) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VNPay hash secret chua duoc cau hinh.");
        }
        if (bookingId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu bookingId.");
        }

        String responseCode = vnpParams.getOrDefault("vnp_ResponseCode", "");
        String vnpTxnRef = vnpParams.get("vnp_TxnRef");
        String vnpAmountRaw = vnpParams.get("vnp_Amount");
        String receivedSecureHash = vnpParams.get("vnp_SecureHash");

        if (receivedSecureHash == null || receivedSecureHash.isBlank()) {
            saveTransactionLogForServicePackage(vnpParams, bookingId, "MISSING_SIGNATURE", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu chu ky bao mat tu VNPay.");
        }

        String expectedTxnRef = servicePackageTxnRef(bookingId);
        if (vnpTxnRef == null || !expectedTxnRef.equals(vnpTxnRef)) {
            saveTransactionLogForServicePackage(vnpParams, bookingId, "INVALID_TXN_REF", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thong tin giao dich khong khop voi phieu dich vu.");
        }

        if (!isValidVnpaySignature(vnpParams, receivedSecureHash)) {
            saveTransactionLogForServicePackage(vnpParams, bookingId, "INVALID_SIGNATURE", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chu ky giao dich VNPay khong hop le.");
        }

        ServicePackageBooking booking = servicePackageBookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay phieu dich vu ID: " + bookingId
                ));

        if (booking.getTotalAmount() == null || booking.getTotalAmount() <= 0) {
            saveTransactionLogForServicePackage(vnpParams, bookingId, "INVALID_BOOKING_AMOUNT", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Phieu dich vu chua co tong tien hop le.");
        }

        long expectedAmount = Math.round(booking.getTotalAmount() * 100);
        long paidAmount = parseVnpAmount(vnpAmountRaw);
        if (expectedAmount != paidAmount) {
            saveTransactionLogForServicePackage(vnpParams, bookingId, "AMOUNT_MISMATCH", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So tien thanh toan khong khop voi phieu dich vu.");
        }

        saveTransactionLogForServicePackage(vnpParams, bookingId, responseCode, parseAmount(vnpAmountRaw));

        if ("00".equals(responseCode)) {
            booking.setPaymentStatus("PAID");
            booking.setStatus("PAID");
            servicePackageBookingRepository.save(booking);
            return "THANH TOAN THANH CONG! Phieu dich vu da duoc cap nhat.";
        }

        if ("24".equals(responseCode)) {
            booking.setPaymentStatus("CANCELLED");
            booking.setStatus("CANCELLED");
        } else {
            booking.setPaymentStatus("FAILED");
            booking.setStatus("PENDING_PAYMENT");
        }
        servicePackageBookingRepository.save(booking);
        return "THANH TOAN THAT BAI HOAC BI HUY BO. Ma loi VNPay: " + responseCode;
    }

    @Transactional
    public String processInvoiceVnpayReturn(Map<String, String> vnpParams, Integer invoiceId) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VNPay hash secret chua duoc cau hinh.");
        }
        if (invoiceId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu invoiceId.");
        }

        String responseCode = vnpParams.getOrDefault("vnp_ResponseCode", "");
        String vnpTxnRef = vnpParams.get("vnp_TxnRef");
        String vnpAmountRaw = vnpParams.get("vnp_Amount");
        String receivedSecureHash = vnpParams.get("vnp_SecureHash");

        if (receivedSecureHash == null || receivedSecureHash.isBlank()) {
            saveTransactionLogForInvoice(vnpParams, invoiceId, "MISSING_SIGNATURE", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu chu ky bao mat tu VNPay.");
        }

        String expectedTxnRef = invoiceTxnRef(invoiceId);
        if (vnpTxnRef == null || !expectedTxnRef.equals(vnpTxnRef)) {
            saveTransactionLogForInvoice(vnpParams, invoiceId, "INVALID_TXN_REF", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thong tin giao dich khong khop voi hoa don.");
        }

        if (!isValidVnpaySignature(vnpParams, receivedSecureHash)) {
            saveTransactionLogForInvoice(vnpParams, invoiceId, "INVALID_SIGNATURE", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chu ky giao dich VNPay khong hop le.");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay hoa don ID: " + invoiceId));

        if (invoice.getTotalAmount() == null || invoice.getTotalAmount() <= 0) {
            saveTransactionLogForInvoice(vnpParams, invoiceId, "INVALID_INVOICE_AMOUNT", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Hoa don chua co tong tien hop le.");
        }

        long expectedAmount = Math.round(invoice.getTotalAmount() * 100);
        long paidAmount = parseVnpAmount(vnpAmountRaw);
        if (expectedAmount != paidAmount) {
            saveTransactionLogForInvoice(vnpParams, invoiceId, "AMOUNT_MISMATCH", parseAmount(vnpAmountRaw));
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So tien thanh toan khong khop voi hoa don.");
        }

        saveTransactionLogForInvoice(vnpParams, invoiceId, responseCode, parseAmount(vnpAmountRaw));

        if ("00".equals(responseCode)) {
            invoice.setStatus("PAID");
            invoiceRepository.save(invoice);
            return "THANH TOAN THANH CONG! Hoa don da duoc cap nhat.";
        }

        if (!"PAID".equalsIgnoreCase(invoice.getStatus())) {
            invoice.setStatus("UNPAID");
            invoiceRepository.save(invoice);
        }
        return "THANH TOAN THAT BAI HOAC BI HUY BO. Ma loi VNPay: " + responseCode;
    }

    private String buildPaymentUrl(
            String txnRef,
            String orderInfo,
            long amount,
            String callbackUrl,
            String ipAddress
    ) {
        String vnpVersion = "2.1.0";
        String vnpCommand = "pay";
        String orderType = "other";
        String vnpIpAddr = (ipAddress == null || ipAddress.isBlank()) ? "127.0.0.1" : ipAddress;
        long vnpAmount = amount * 100;

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", orderType);
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", callbackUrl);
        vnpParams.put("vnp_IpAddr", vnpIpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        boolean first = true;
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    hashData.append('&');
                    query.append('&');
                }
                hashData.append(fieldName).append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                first = false;
            }
        }

        String vnpSecureHash = VNPayConfig.hmacSHA512(secretKey, hashData.toString());
        String queryUrl = query + "&vnp_SecureHash=" + vnpSecureHash;
        return vnpPayUrl + "?" + queryUrl;
    }

    private String withReturnParam(String key, Integer value) {
        String separator = vnpReturnUrl.contains("?") ? "&" : "?";
        return vnpReturnUrl + separator + key + "=" + value;
    }

    private void assertVnpayConfiguration() {
        if (vnpTmnCode == null || vnpTmnCode.isBlank()
                || secretKey == null || secretKey.isBlank()
                || vnpPayUrl == null || vnpPayUrl.isBlank()
                || vnpReturnUrl == null || vnpReturnUrl.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VNPay chua duoc cau hinh day du tren server.");
        }
    }

    private String servicePackageTxnRef(Integer bookingId) {
        return SERVICE_PACKAGE_TXN_PREFIX + bookingId;
    }

    private String invoiceTxnRef(Integer invoiceId) {
        return INVOICE_TXN_PREFIX + invoiceId;
    }

    private Patient getCurrentPatientOrThrow(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return patientRepository.findByAccount_Username(username)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan cua ban chua duoc lien ket voi ho so benh nhan."
                ));
    }

    private void saveTransactionLogForAppointment(Map<String, String> vnpParams, Integer appointmentId, String responseCode, double amount) {
        String vnpTransactionNo = vnpParams.get("vnp_TransactionNo");
        if (vnpTransactionNo != null
                && !vnpTransactionNo.isBlank()
                && transactionLogRepository.existsByVnpTransactionNo(vnpTransactionNo)) {
            return;
        }

        TransactionLog log = new TransactionLog();
        log.setAppointmentId(appointmentId);
        log.setServicePackageBookingId(null);
        log.setInvoiceId(null);
        log.setVnpTxnRef(vnpParams.get("vnp_TxnRef"));
        log.setVnpTransactionNo(vnpTransactionNo);
        log.setBankCode(vnpParams.get("vnp_BankCode"));
        log.setAmount(amount);
        log.setResponseCode(responseCode);
        transactionLogRepository.save(log);
    }

    private void saveTransactionLogForServicePackage(Map<String, String> vnpParams, Integer bookingId, String responseCode, double amount) {
        String vnpTransactionNo = vnpParams.get("vnp_TransactionNo");
        if (vnpTransactionNo != null
                && !vnpTransactionNo.isBlank()
                && transactionLogRepository.existsByVnpTransactionNo(vnpTransactionNo)) {
            return;
        }

        TransactionLog log = new TransactionLog();
        log.setAppointmentId(null);
        log.setServicePackageBookingId(bookingId);
        log.setInvoiceId(null);
        log.setVnpTxnRef(vnpParams.get("vnp_TxnRef"));
        log.setVnpTransactionNo(vnpTransactionNo);
        log.setBankCode(vnpParams.get("vnp_BankCode"));
        log.setAmount(amount);
        log.setResponseCode(responseCode);
        transactionLogRepository.save(log);
    }

    private void saveTransactionLogForInvoice(Map<String, String> vnpParams, Integer invoiceId, String responseCode, double amount) {
        String vnpTransactionNo = vnpParams.get("vnp_TransactionNo");
        if (vnpTransactionNo != null
                && !vnpTransactionNo.isBlank()
                && transactionLogRepository.existsByVnpTransactionNo(vnpTransactionNo)) {
            return;
        }

        TransactionLog log = new TransactionLog();
        log.setAppointmentId(null);
        log.setServicePackageBookingId(null);
        log.setInvoiceId(invoiceId);
        log.setVnpTxnRef(vnpParams.get("vnp_TxnRef"));
        log.setVnpTransactionNo(vnpTransactionNo);
        log.setBankCode(vnpParams.get("vnp_BankCode"));
        log.setAmount(amount);
        log.setResponseCode(responseCode);
        transactionLogRepository.save(log);
    }

    private boolean isValidVnpaySignature(Map<String, String> vnpParams, String expectedHash) {
        Map<String, String> filteredParams = new HashMap<>();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith("vnp_")) {
                continue;
            }
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) {
                continue;
            }
            filteredParams.put(key, entry.getValue());
        }

        List<String> fieldNames = new ArrayList<>(filteredParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        boolean first = true;
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = filteredParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    hashData.append('&');
                }
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                first = false;
            }
        }

        String calculatedHash = VNPayConfig.hmacSHA512(secretKey, hashData.toString());
        return calculatedHash.equalsIgnoreCase(expectedHash);
    }

    private long parseVnpAmount(String vnpAmountRaw) {
        if (vnpAmountRaw == null || vnpAmountRaw.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong nhan duoc so tien tu VNPay.");
        }
        try {
            return Long.parseLong(vnpAmountRaw);
        } catch (NumberFormatException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So tien VNPay khong hop le.");
        }
    }

    private double parseAmount(String vnpAmountRaw) {
        if (vnpAmountRaw == null || vnpAmountRaw.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(vnpAmountRaw) / 100.0;
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }
}
