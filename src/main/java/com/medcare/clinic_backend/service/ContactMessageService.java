package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.contact.*;
import com.medcare.clinic_backend.dto.feedback.MessageResponse;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.ContactMessage;
import com.medcare.clinic_backend.entity.ContactMessageStatus;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public MessageResponse create(ContactMessageCreateRequest request) {
        ContactMessage contactMessage = new ContactMessage();
        contactMessage.setFullName(trimToNull(request.getFullName()));
        contactMessage.setEmail(trimToNull(request.getEmail()));
        contactMessage.setPhone(trimToNull(request.getPhone()));
        contactMessage.setSubject(trimToNull(request.getSubject()));
        contactMessage.setMessage(trimToNull(request.getMessage()));
        contactMessage.setStatus(ContactMessageStatus.NEW);
        contactMessageRepository.save(contactMessage);
        return new MessageResponse("Gửi tin nhắn thành công. Chúng tôi sẽ phản hồi trong thời gian sớm nhất.");
    }

    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getAdminList(String keyword,
                                                     String status,
                                                     int page,
                                                     int size,
                                                     String sort) {
        Sort defaultSort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), parseSort(sort, defaultSort));
        ContactMessageStatus statusFilter = parseStatus(status);
        String keywordPattern = toLikePattern(trimToNull(keyword));
        Page<ContactMessage> items = keywordPattern == null
                ? contactMessageRepository.findAdminMessages(statusFilter, pageable)
                : contactMessageRepository.searchByKeyword(keywordPattern, statusFilter, pageable);
        List<ContactMessageResponse> content = items.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageImpl<>(content, items.getPageable(), items.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ContactMessageResponse getDetail(Integer id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Transactional
    public MessageResponse updateStatus(Integer id, ContactMessageStatusUpdateRequest request) {
        ContactMessage message = findByIdOrThrow(id);
        message.setStatus(request.getStatus());
        contactMessageRepository.save(message);
        return new MessageResponse("Cập nhật trạng thái tin nhắn thành công.");
    }

    @Transactional
    public MessageResponse reply(Integer id, ContactMessageReplyRequest request, String adminUsername) {
        ContactMessage message = findByIdOrThrow(id);
        Account admin = accountRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản admin đăng nhập."));
        message.setAdminReply(trimToNull(request.getAdminReply()));
        message.setAdminNote(trimToNull(request.getAdminNote()));
        message.setRepliedByAdmin(admin);
        message.setRepliedAt(LocalDateTime.now());
        message.setStatus(ContactMessageStatus.REPLIED);
        contactMessageRepository.save(message);
        return new MessageResponse("Phản hồi tin nhắn thành công.");
    }

    @Transactional
    public MessageResponse delete(Integer id) {
        ContactMessage message = findByIdOrThrow(id);
        contactMessageRepository.delete(message);
        return new MessageResponse("Xóa tin nhắn thành công.");
    }

    @Transactional(readOnly = true)
    public ContactMessageStatsResponse getStats() {
        return new ContactMessageStatsResponse(
                contactMessageRepository.count(),
                contactMessageRepository.countByStatus(ContactMessageStatus.NEW),
                contactMessageRepository.countByStatus(ContactMessageStatus.IN_PROGRESS),
                contactMessageRepository.countByStatus(ContactMessageStatus.REPLIED),
                contactMessageRepository.countByStatus(ContactMessageStatus.CLOSED)
        );
    }

    private ContactMessage findByIdOrThrow(Integer id) {
        return contactMessageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy tin nhắn liên hệ."));
    }

    private ContactMessageResponse toResponse(ContactMessage message) {
        Account repliedByAdmin = message.getRepliedByAdmin();
        return ContactMessageResponse.builder()
                .id(message.getId())
                .fullName(message.getFullName())
                .email(message.getEmail())
                .phone(message.getPhone())
                .subject(message.getSubject())
                .message(message.getMessage())
                .status(message.getStatus())
                .adminReply(message.getAdminReply())
                .repliedByAdminId(repliedByAdmin == null ? null : repliedByAdmin.getId())
                .repliedByAdminEmail(repliedByAdmin == null ? null : repliedByAdmin.getUsername())
                .repliedAt(message.getRepliedAt())
                .adminNote(message.getAdminNote())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    private Sort parseSort(String sort, Sort defaultSort) {
        if (sort == null || sort.isBlank()) {
            return defaultSort;
        }
        String normalizedSort = sort.trim().toLowerCase();
        if ("newest".equals(normalizedSort)) {
            return defaultSort;
        }
        if ("oldest".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        if ("name_asc".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.ASC, "fullName");
        }
        if ("name_desc".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.DESC, "fullName");
        }
        return defaultSort;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toLikePattern(String keyword) {
        return keyword == null ? null : "%" + keyword + "%";
    }

    private ContactMessageStatus parseStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        try {
            return ContactMessageStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
