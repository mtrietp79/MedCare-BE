package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.medcare.clinic_backend.entity.Account;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "doctors") // Ánh xạ đúng với tên bảng bạn đã tạo trong pgAdmin
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(length = 15)
    private String phone;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    // ĐÂY LÀ PHẦN QUAN TRỌNG NHẤT: Mối quan hệ Khóa Ngoại
    @ManyToOne
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @OneToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    // Nhớ Generate Getter và Setter cho 'account' ở dưới cùng nhé:
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}