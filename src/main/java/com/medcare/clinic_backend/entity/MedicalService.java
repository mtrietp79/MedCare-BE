package com.medcare.clinic_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "medical_services")
public class MedicalService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    @Transient
    private String imageUrl;

    @Column(nullable = false)
    private Boolean active = true;

    @Transient
    private String status;

    @Column(nullable = false)
    private Boolean advertised = false;

    @ManyToOne
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @JsonIgnoreProperties({"account"})
    @ManyToOne
    @JoinColumn(name = "assigned_doctor_id")
    private Doctor assignedDoctor;

    @Transient
    private Integer specialtyId;

    @Transient
    private Integer assignedDoctorId;

    @JsonIgnoreProperties("medicalService")
    @OneToMany(mappedBy = "medicalService", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalServicePrescriptionItem> prescriptionItems = new ArrayList<>();
}
