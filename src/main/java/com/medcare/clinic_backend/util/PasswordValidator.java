package com.medcare.clinic_backend.util;

import com.medcare.clinic_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

public final class PasswordValidator {

    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};:'\",.<>/\\\\|`~]).{8,}$"
    );

    private PasswordValidator() {
    }

    public static void validateNewPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được để trống.");
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt."
            );
        }
    }

    public static void validatePasswordPair(String newPassword, String confirmPassword) {
        validateNewPassword(newPassword);
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp.");
        }
    }
}
