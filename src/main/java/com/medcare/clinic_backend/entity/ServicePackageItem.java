package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "service_package_items")
public class ServicePackageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private ServicePackage servicePackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_service_id", nullable = false)
    private MedicalService medicalService;
}
