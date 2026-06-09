package com.medcare.clinic_backend.dto.contact;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactMessageReplyRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống.")
    private String adminReply;

    private String adminNote;
}
