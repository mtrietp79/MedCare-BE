package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.contact.ContactMessageCreateRequest;
import com.medcare.clinic_backend.dto.contact.ContactMessageReplyRequest;
import com.medcare.clinic_backend.dto.contact.ContactMessageStatusUpdateRequest;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.ContactMessage;
import com.medcare.clinic_backend.entity.ContactMessageStatus;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.repository.ContactMessageRepository;
import com.medcare.clinic_backend.service.ContactMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactMessageIntegrationTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private ContactMessageService contactMessageService;

    private Account admin;

    @BeforeEach
    void setUp() {
        admin = new Account();
        admin.setId(1);
        admin.setUsername("trietminhpham79@gmail.com");
        admin.setRole("ROLE_ADMIN");
    }

    @Test
    void guestCanSendContactMessageAndSavedToRepository() {
        ContactMessageCreateRequest request = new ContactMessageCreateRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("nguyenvana@gmail.com");
        request.setPhone("0868663667");
        request.setSubject("Can tu van dat lich");
        request.setMessage("Toi muon hoi them ve dich vu kham tong quat.");

        contactMessageService.create(request);

        ArgumentCaptor<ContactMessage> captor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactMessageRepository).save(captor.capture());
        ContactMessage saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(saved.getStatus()).isEqualTo(ContactMessageStatus.NEW);
    }

    @Test
    void adminCanListDetailUpdateStatusReplyAndFilter() {
        ContactMessage message = new ContactMessage();
        message.setId(10);
        message.setFullName("Tran B");
        message.setEmail("tranb@gmail.com");
        message.setMessage("Xin bao gia");
        message.setStatus(ContactMessageStatus.NEW);

        when(contactMessageRepository.searchByKeyword(eq("%tranb@gmail.com%"), eq(ContactMessageStatus.NEW), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(message)));
        when(contactMessageRepository.searchByKeyword(eq("%tranb@gmail.com%"), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(message)));
        when(contactMessageRepository.findById(10)).thenReturn(Optional.of(message));
        when(accountRepository.findByUsername("trietminhpham79@gmail.com")).thenReturn(Optional.of(admin));

        Page<?> page = contactMessageService.getAdminList("tranb@gmail.com", "NEW", 0, 10, "newest");
        assertThat(page.getTotalElements()).isEqualTo(1);

        contactMessageService.getAdminList("tranb@gmail.com", "ALL", 0, 10, "newest");
        verify(contactMessageRepository).searchByKeyword(eq("%tranb@gmail.com%"), isNull(), any(PageRequest.class));

        assertThat(contactMessageService.getDetail(10).getEmail()).isEqualTo("tranb@gmail.com");

        ContactMessageStatusUpdateRequest statusUpdateRequest = new ContactMessageStatusUpdateRequest();
        statusUpdateRequest.setStatus(ContactMessageStatus.IN_PROGRESS);
        contactMessageService.updateStatus(10, statusUpdateRequest);
        assertThat(message.getStatus()).isEqualTo(ContactMessageStatus.IN_PROGRESS);

        ContactMessageReplyRequest replyRequest = new ContactMessageReplyRequest();
        replyRequest.setAdminReply("Cam on ban da lien he");
        replyRequest.setAdminNote("Da phan hoi qua he thong");
        contactMessageService.reply(10, replyRequest, "trietminhpham79@gmail.com");

        assertThat(message.getStatus()).isEqualTo(ContactMessageStatus.REPLIED);
        assertThat(message.getRepliedByAdmin().getUsername()).isEqualTo("trietminhpham79@gmail.com");
    }

    @Test
    void statsAndRoleBoundaryExpectations() {
        when(contactMessageRepository.count()).thenReturn(20L);
        when(contactMessageRepository.countByStatus(ContactMessageStatus.NEW)).thenReturn(5L);
        when(contactMessageRepository.countByStatus(ContactMessageStatus.IN_PROGRESS)).thenReturn(3L);
        when(contactMessageRepository.countByStatus(ContactMessageStatus.REPLIED)).thenReturn(10L);
        when(contactMessageRepository.countByStatus(ContactMessageStatus.CLOSED)).thenReturn(2L);

        var stats = contactMessageService.getStats();
        assertThat(stats.getTotal()).isEqualTo(20L);
        assertThat(stats.getRepliedCount()).isEqualTo(10L);

        verifyNoMoreInteractions(accountRepository);
    }
}