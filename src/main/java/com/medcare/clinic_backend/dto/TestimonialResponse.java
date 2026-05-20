package com.medcare.clinic_backend.dto;

import com.medcare.clinic_backend.entity.Feedback;

import java.time.LocalDateTime;

public class TestimonialResponse {

    private Integer id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private Integer patientId;
    private String patientName;
    private Integer doctorId;
    private String doctorName;
    private String specialtyName;

    public TestimonialResponse(Feedback feedback) {
        this.id = feedback.getId();
        this.rating = feedback.getRating();
        this.comment = feedback.getComment();
        this.createdAt = feedback.getCreatedAt();

        if (feedback.getPatient() != null) {
            this.patientId = feedback.getPatient().getId();
            this.patientName = feedback.getPatient().getFullName();
        }

        if (feedback.getDoctor() != null) {
            this.doctorId = feedback.getDoctor().getId();
            this.doctorName = feedback.getDoctor().getFullName();
            if (feedback.getDoctor().getSpecialty() != null) {
                this.specialtyName = feedback.getDoctor().getSpecialty().getName();
            }
        }
    }

    public Integer getId() { return id; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Integer getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public Integer getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public String getSpecialtyName() { return specialtyName; }
}
