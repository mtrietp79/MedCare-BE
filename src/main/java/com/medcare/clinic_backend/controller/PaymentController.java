package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.config.VNPayConfig;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret}")
    private String secretKey;

    @Value("${vnpay.payUrl}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl}")
    private String vnpReturnUrl;

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/create-url")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public String createPaymentUrl(@RequestParam("appointmentId") Integer appointmentId,
                                   HttpServletRequest request) {
        if (vnpTmnCode == null || vnpTmnCode.isBlank()
                || secretKey == null || secretKey.isBlank()
                || vnpPayUrl == null || vnpPayUrl.isBlank()
                || vnpReturnUrl == null || vnpReturnUrl.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VNPay chua duoc cau hinh day du tren server.");
        }

        String vnpVersion = "2.1.0";
        String vnpCommand = "pay";
        String vnpOrderInfo = "Thanh toan lich hen ID: " + appointmentId;
        String orderType = "other";

        String vnpTxnRef = String.valueOf(appointmentId);
        String vnpIpAddr = VNPayConfig.getIpAddress(request);
        long amount = paymentService.resolvePaymentAmount(appointmentId);
        long vnpAmount = amount * 100;

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", vnpOrderInfo);
        vnpParams.put("vnp_OrderType", orderType);
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl + "?appointmentId=" + appointmentId);
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

    @GetMapping("/vnpay-return")
    public String paymentReturn(@RequestParam Map<String, String> queryParams,
                                @RequestParam("appointmentId") Integer appointmentId) {
        return paymentService.processVnpayReturn(queryParams, appointmentId);
    }
}
