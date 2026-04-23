package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 15, unique = true)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 10)
    private String gender;

    @Column(name = "national_id", length = 12, unique = true)
    private String nationalId;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "profile_completed")
    private Boolean profileCompleted = false;

    @OneToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;
}
