package com.medcare.clinic_backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequestOtpDto {

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không hợp lệ")
    @JsonAlias({"username", "identifier"})
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
