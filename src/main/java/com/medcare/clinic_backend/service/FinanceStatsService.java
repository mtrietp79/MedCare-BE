package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.invoice.FinanceSummaryResponse;
import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import com.medcare.clinic_backend.util.FinanceInvoiceRules;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinanceStatsService {

    public FinanceSummaryResponse buildSummary(List<InvoiceResponse> invoices) {
        LocalDateTime startOfCurrentMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfNextMonth = startOfCurrentMonth.plusMonths(1);

        double totalRevenue = 0.0;
        double monthlyRevenue = 0.0;
        double pendingAmount = 0.0;
        long paidCount = 0;
        long pendingCount = 0;

        if (invoices != null) {
            for (InvoiceResponse invoice : invoices) {
                if (invoice == null) {
                    continue;
                }
                double totalAmount = safeDouble(invoice.getTotalAmount());
                if (FinanceInvoiceRules.countsTowardRevenue(invoice)) {
                    paidCount++;
                    totalRevenue += totalAmount;
                    LocalDateTime revenueTime = invoice.getPaymentDate() != null
                            ? invoice.getPaymentDate()
                            : invoice.getCreatedAt();
                    if (revenueTime != null
                            && !revenueTime.isBefore(startOfCurrentMonth)
                            && revenueTime.isBefore(startOfNextMonth)) {
                        monthlyRevenue += totalAmount;
                    }
                } else if (FinanceInvoiceRules.countsTowardPending(invoice)) {
                    pendingCount++;
                    pendingAmount += totalAmount;
                }
            }
        }

        return new FinanceSummaryResponse(
                totalRevenue,
                monthlyRevenue,
                pendingAmount,
                paidCount,
                pendingCount,
                invoices == null ? 0 : invoices.size()
        );
    }

    public double calculateRevenueBetween(List<InvoiceResponse> invoices, LocalDateTime start, LocalDateTime end) {
        if (invoices == null || start == null || end == null) {
            return 0.0;
        }
        double total = 0.0;
        for (InvoiceResponse invoice : invoices) {
            if (!FinanceInvoiceRules.countsTowardRevenue(invoice)) {
                continue;
            }
            LocalDateTime revenueTime = invoice.getPaymentDate() != null
                    ? invoice.getPaymentDate()
                    : invoice.getCreatedAt();
            if (revenueTime == null || revenueTime.isBefore(start) || !revenueTime.isBefore(end)) {
                continue;
            }
            total += safeDouble(invoice.getTotalAmount());
        }
        return total;
    }

    public Map<Integer, Double> calculateMonthlyRevenueByYear(List<InvoiceResponse> invoices, int year) {
        Map<Integer, Double> revenueByMonth = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            revenueByMonth.put(month, 0.0);
        }
        if (invoices == null) {
            return revenueByMonth;
        }
        for (InvoiceResponse invoice : invoices) {
            if (!FinanceInvoiceRules.countsTowardRevenue(invoice)) {
                continue;
            }
            LocalDateTime revenueTime = invoice.getPaymentDate() != null
                    ? invoice.getPaymentDate()
                    : invoice.getCreatedAt();
            if (revenueTime == null || revenueTime.getYear() != year) {
                continue;
            }
            int month = revenueTime.getMonthValue();
            revenueByMonth.merge(month, safeDouble(invoice.getTotalAmount()), Double::sum);
        }
        return revenueByMonth;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }
}
