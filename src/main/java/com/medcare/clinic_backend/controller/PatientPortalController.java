package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.cancellation.CreateCancellationRequestDto;
import com.medcare.clinic_backend.dto.cancellation.CreateCancellationRequestResponse;
import com.medcare.clinic_backend.dto.cancellation.PatientCancellationRequestSummary;
import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import com.medcare.clinic_backend.dto.invoice.PatientInvoiceDetailResponse;
import com.medcare.clinic_backend.dto.patient.PatientAppointmentResponse;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.service.AppointmentCancellationService;
import com.medcare.clinic_backend.service.AppointmentService;
import com.medcare.clinic_backend.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasAuthority('ROLE_PATIENT')")
public class PatientPortalController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private AppointmentCancellationService appointmentCancellationService;

    @GetMapping("/appointments")
    public List<PatientAppointmentResponse> getMyAppointments(Authentication authentication) {
        Patient patient = getCurrentPatientOrThrow(authentication);
        return appointmentService.getAppointmentResponsesForPatient(patient.getId());
    }

    @GetMapping("/appointments/{appointmentId}")
    public PatientAppointmentResponse getMyAppointmentDetail(
            @PathVariable Integer appointmentId,
            Authentication authentication
    ) {
        Patient patient = getCurrentPatientOrThrow(authentication);
        return appointmentService.getAppointmentResponseForPatient(patient.getId(), appointmentId);
    }

    @PostMapping("/appointments/{appointmentId}/cancel-request")
    public CreateCancellationRequestResponse submitCancellationRequest(
            @PathVariable Integer appointmentId,
            @RequestBody CreateCancellationRequestDto request,
            Authentication authentication
    ) {
        Patient patient = getCurrentPatientOrThrow(authentication);
        return appointmentCancellationService.createCancellationRequest(appointmentId, patient.getId(), request);
    }

    @GetMapping("/cancellation-requests")
    public List<PatientCancellationRequestSummary> getMyCancellationRequests(Authentication authentication) {
        Patient patient = getCurrentPatientOrThrow(authentication);
        return appointmentCancellationService.getPatientCancellationRequests(patient.getId());
    }

    @GetMapping({"/invoices", "/appointments/invoices"})
    public List<InvoiceResponse> getMyInvoices(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type,
            Authentication authentication
    ) {
        Patient patient = getCurrentPatientOrThrow(authentication);
        return financeService.getInvoiceResponsesForPatient(patient.getId(), keyword, status, category, type);
    }

    @GetMapping({"/invoices/{id}", "/appointments/invoices/{id}"})
    public PatientInvoiceDetailResponse getMyInvoiceDetail(
            @PathVariable Integer id,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "uniqueKey", required = false) String uniqueKey,
            Authentication authentication
    ) {
        Patient patient = getCurrentPatientOrThrow(authentication);
        return financeService.getPatientInvoiceDetail(patient.getId(), id, sourceType, uniqueKey);
    }

    private Patient getCurrentPatientOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return patientRepository.findByAccount_Username(authentication.getName())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan cua ban chua duoc lien ket voi ho so benh nhan."
                ));
    }
}
