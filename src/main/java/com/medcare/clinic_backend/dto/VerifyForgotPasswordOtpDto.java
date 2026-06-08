package com.medcare.clinic_backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyForgotPasswordOtpDto {

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không hợp lệ")
    @JsonAlias({"username", "identifier"})
    private String email;

    @NotBlank(message = "Mã OTP không được để trống.")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm 6 chữ số.")
    private String otp;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
