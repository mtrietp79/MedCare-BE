package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Người đánh giá (Bệnh nhân)
    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Bác sĩ bị đánh giá
    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // Số sao (từ 1 đến 5)
    @Column(nullable = false)
    private Integer rating;

    // Lời nhận xét
    @Column(columnDefinition = "TEXT")
    private String comment;

    // Thời gian đánh giá
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- ÔNG TỰ GENERATE GETTER & SETTER VÀ CONSTRUCTOR NHÉ ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}