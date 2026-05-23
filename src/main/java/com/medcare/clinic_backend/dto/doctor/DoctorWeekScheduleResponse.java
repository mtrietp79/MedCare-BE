package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorWeekScheduleResponse {
    private String weekRange;
    private List<DayScheduleItem> days;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayScheduleItem {
        private LocalDate date;
        private String dayName;
        private long morningCount;
        private long afternoonCount;
    }
}
