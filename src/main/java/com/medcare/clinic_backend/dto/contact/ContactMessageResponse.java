package com.medcare.clinic_backend.dto.contact;

import com.medcare.clinic_backend.entity.ContactMessageStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContactMessageResponse {
    private Integer id;
    private String fullName;
    private String email;
    private String phone;
    private String subject;
    private String message;
    private ContactMessageStatus status;
    private String adminReply;
    private Integer repliedByAdminId;
    private String repliedByAdminEmail;
    private LocalDateTime repliedAt;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
