package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorMedicalRecordPatientItemResponse {
    private Integer patientId;
    private String fullName;
    private String phone;
    private String email;
    private String gender;
    private long newExamCount;
    private long followUpCount;
    private long totalVisitCount;
    private LocalDate latestVisitDate;

    public long getVisitCount() {
        return totalVisitCount;
    }
}
