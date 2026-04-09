package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "doctor_schedules")
public class DoctorSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    // Ví dụ: MORNING, AFTERNOON, ALL_DAY
    @Column(nullable = false, length = 20)
    private String shift;

    @Column(name = "max_patients")
    private Integer maxPatients = 20; // Giới hạn số bệnh nhân mỗi ca

    @Column(name = "current_patients")
    private Integer currentPatients = 0; // Số bệnh nhân đã đặt
}