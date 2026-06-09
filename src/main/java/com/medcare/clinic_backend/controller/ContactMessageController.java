package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.contact.ContactMessageCreateRequest;
import com.medcare.clinic_backend.dto.feedback.MessageResponse;
import com.medcare.clinic_backend.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    @PostMapping("/api/contact-messages")
    public MessageResponse create(@Valid @RequestBody ContactMessageCreateRequest request) {
        return contactMessageService.create(request);
    }
}
