package com.medcare.clinic_backend.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactMessageCreateRequest {

    @NotBlank(message = "Họ tên không được để trống.")
    private String fullName;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    private String email;

    private String phone;

    private String subject;

    @NotBlank(message = "Nội dung tin nhắn không được để trống.")
    private String message;
}
