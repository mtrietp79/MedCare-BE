package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.invoice.FinanceSummaryResponse;
import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinanceStatsServiceTest {

    private final FinanceStatsService financeStatsService = new FinanceStatsService();

    @Test
    void buildSummary_shouldExcludeCancelledAppointmentsFromPendingAndRevenue() {
        InvoiceResponse activeUnpaid = invoice("APPOINTMENT-1", "UNPAID", null, false, 200000.0, null);
        InvoiceResponse cancelledUnpaid = invoice("APPOINTMENT-2", "UNPAID", "CANCEL_REQUESTED", true, 270000.0, null);
        InvoiceResponse paidActive = invoice("APPOINTMENT-3", "PAID", "CONFIRMED", false, 300000.0,
                LocalDateTime.now());
        InvoiceResponse paidCancelled = invoice("APPOINTMENT-4", "PAID", "CANCELLED", true, 150000.0,
                LocalDateTime.now());

        FinanceSummaryResponse summary = financeStatsService.buildSummary(List.of(
                activeUnpaid,
                cancelledUnpaid,
                paidActive,
                paidCancelled
        ));

        assertEquals(1, summary.getPendingCount());
        assertEquals(200000.0, summary.getPendingAmount());
        assertEquals(1, summary.getPaidCount());
        assertEquals(300000.0, summary.getTotalRevenue());
    }

    private InvoiceResponse invoice(String uniqueKey,
                                    String paymentStatus,
                                    String appointmentStatus,
                                    boolean cancelled,
                                    double amount,
                                    LocalDateTime paymentDate) {
        InvoiceResponse response = new InvoiceResponse();
        response.setUniqueKey(uniqueKey);
        response.setPaymentStatus(paymentStatus);
        response.setStatus(paymentStatus);
        response.setAppointmentStatus(appointmentStatus);
        response.setIsCancelled(cancelled);
        response.setTotalAmount(amount);
        response.setPaymentDate(paymentDate);
        response.setCreatedAt(paymentDate == null ? LocalDateTime.now() : paymentDate);
        return response;
    }
}
