package com.medcare.clinic_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class ResetPasswordWithTokenDto {

    @NotBlank(message = "Reset token không được để trống.")
    private String resetToken;

    @NotBlank(message = "Mật khẩu mới không được để trống.")
    private String newPassword;

    @NotBlank(message = "Mật khẩu xác nhận không được để trống.")
    private String confirmPassword;

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
