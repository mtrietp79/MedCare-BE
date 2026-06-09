package com.medcare.clinic_backend.dto.contact;

import com.medcare.clinic_backend.entity.ContactMessageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContactMessageStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống.")
    private ContactMessageStatus status;
}
