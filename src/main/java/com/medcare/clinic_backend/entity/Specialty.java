package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data // Annotation của Lombok giúp tự động tạo Getter, Setter
@Entity // Báo cho Spring biết đây là một Entity
@Table(name = "specialties") // Ánh xạ chính xác với tên bảng trong PostgreSQL
public class Specialty {

    @Id // Đánh dấu đây là khóa chính (Primary Key)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng ID (giống SERIAL trong DB)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private Long totalDoctors;

    @Transient
    private Long doctorCount;
}
