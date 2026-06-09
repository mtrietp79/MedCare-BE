package com.medcare.clinic_backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @JsonIgnore
    @Column(name = "reset_otp")
    private String resetOtp;

    @JsonIgnore
    @Column(name = "otp_expiry_time")
    private LocalDateTime otpExpiryTime;

    @JsonIgnore
    @Column(name = "reset_token")
    private String resetToken;

    @JsonIgnore
    @Column(name = "reset_token_expiry_time")
    private LocalDateTime resetTokenExpiryTime;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "is_test_account", nullable = false)
    private Boolean isTestAccount = false;

    @JsonIgnore
    @Column(name = "otp_last_sent_at")
    private LocalDateTime otpLastSentAt;

    @JsonIgnore
    @Column(name = "otp_failed_attempts")
    private Integer otpFailedAttempts = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Account() {}

    public Account(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getResetOtp() { return resetOtp; }
    public void setResetOtp(String resetOtp) { this.resetOtp = resetOtp; }

    public LocalDateTime getOtpExpiryTime() { return otpExpiryTime; }
    public void setOtpExpiryTime(LocalDateTime otpExpiryTime) { this.otpExpiryTime = otpExpiryTime; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiryTime() { return resetTokenExpiryTime; }
    public void setResetTokenExpiryTime(LocalDateTime resetTokenExpiryTime) {
        this.resetTokenExpiryTime = resetTokenExpiryTime;
    }

    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public Boolean getIsTestAccount() { return isTestAccount; }
    public void setIsTestAccount(Boolean isTestAccount) { this.isTestAccount = isTestAccount; }

    public LocalDateTime getOtpLastSentAt() { return otpLastSentAt; }
    public void setOtpLastSentAt(LocalDateTime otpLastSentAt) { this.otpLastSentAt = otpLastSentAt; }

    public Integer getOtpFailedAttempts() { return otpFailedAttempts; }
    public void setOtpFailedAttempts(Integer otpFailedAttempts) { this.otpFailedAttempts = otpFailedAttempts; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void clearPasswordRecoveryState() {
        this.resetOtp = null;
        this.otpExpiryTime = null;
        this.resetToken = null;
        this.resetTokenExpiryTime = null;
        this.otpFailedAttempts = 0;
    }
}
