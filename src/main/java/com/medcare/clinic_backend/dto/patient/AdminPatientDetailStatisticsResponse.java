package com.medcare.clinic_backend.dto.patient;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminPatientDetailStatisticsResponse {
    private long appointmentCount;
    private long completedAppointmentCount;
    private long cancelledAppointmentCount;
    private long medicalRecordCount;
    private long invoiceCount;
    private long totalPaidAmount;
}