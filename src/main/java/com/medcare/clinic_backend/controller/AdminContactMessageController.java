package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.contact.*;
import com.medcare.clinic_backend.dto.feedback.MessageResponse;
import com.medcare.clinic_backend.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/contact-messages")
public class AdminContactMessageController {

    private final ContactMessageService contactMessageService;

    @GetMapping
    public Page<ContactMessageResponse> list(@RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "ALL") String status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String sort) {
        return contactMessageService.getAdminList(keyword, status, page, size, sort);
    }

    @GetMapping("/{id}")
    public ContactMessageResponse detail(@PathVariable Integer id) {
        return contactMessageService.getDetail(id);
    }

    @PatchMapping("/{id}/status")
    public MessageResponse updateStatus(@PathVariable Integer id,
                                        @Valid @RequestBody ContactMessageStatusUpdateRequest request) {
        return contactMessageService.updateStatus(id, request);
    }

    @PatchMapping("/{id}/reply")
    public MessageResponse reply(@PathVariable Integer id,
                                 @Valid @RequestBody ContactMessageReplyRequest request,
                                 Authentication authentication) {
        return contactMessageService.reply(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable Integer id) {
        return contactMessageService.delete(id);
    }

    @GetMapping("/stats")
    public ContactMessageStatsResponse stats() {
        return contactMessageService.getStats();
    }
}
