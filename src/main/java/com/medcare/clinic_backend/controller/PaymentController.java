package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.config.VNPayConfig;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/create-url")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public String createPaymentUrl(@RequestParam("appointmentId") Integer appointmentId,
                                   Authentication authentication,
                                   HttpServletRequest request) {
        if (appointmentId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu appointmentId.");
        }
        return paymentService.createPaymentUrl(
                appointmentId,
                VNPayConfig.getIpAddress(request),
                authentication == null ? null : authentication.getName()
        );
    }

    @GetMapping("/create-invoice-url")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public String createInvoicePaymentUrl(@RequestParam("invoiceId") Integer invoiceId,
                                          Authentication authentication,
                                          HttpServletRequest request) {
        if (invoiceId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu invoiceId.");
        }
        return paymentService.createInvoicePaymentUrl(
                invoiceId,
                VNPayConfig.getIpAddress(request),
                authentication == null ? null : authentication.getName()
        );
    }

    @GetMapping("/vnpay-return")
    public String paymentReturn(
            @RequestParam Map<String, String> queryParams,
            @RequestParam(value = "appointmentId", required = false) Integer appointmentId,
            @RequestParam(value = "bookingId", required = false) Integer bookingId,
            @RequestParam(value = "invoiceId", required = false) Integer invoiceId
    ) {
        if (bookingId != null) {
            return paymentService.processServicePackageBookingVnpayReturn(queryParams, bookingId);
        }
        if (appointmentId != null) {
            return paymentService.processVnpayReturn(queryParams, appointmentId);
        }
        if (invoiceId != null) {
            return paymentService.processInvoiceVnpayReturn(queryParams, invoiceId);
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu appointmentId, bookingId hoac invoiceId.");
    }
}
