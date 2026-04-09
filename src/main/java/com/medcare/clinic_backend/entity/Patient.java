package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.medcare.clinic_backend.entity.Account;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Data
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 15, unique = true)
    private String phone; // Số điện thoại thường dùng làm tài khoản đăng nhập/đăng ký nên bắt buộc và duy nhất

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    // LIÊN KẾT 1-1 VỚI BẢNG ACCOUNT
    @OneToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    // Nhớ Generate Getter và Setter cho 'account' ở dưới cùng nhé:
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}