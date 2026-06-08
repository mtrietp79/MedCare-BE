package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PatientService patientService;

    @Mock
    private OtpDeliveryService otpDeliveryService;

    @InjectMocks
    private AuthService authService;

    @ParameterizedTest
    @ValueSource(strings = {
            "benhnhan1@gmail.com",
            "2321001183@sv.ufm.edu.vn",
            "triet.pham@hutech.edu.vn",
            "abc@outlook.com"
    })
    void register_acceptsValidNonGmailEmails(String email) {
        Account account = new Account(email, "secret", "ROLE_PATIENT");

        when(accountRepository.findByUsername(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        String result = authService.register(
                account,
                "Test User",
                "0899370425",
                email
        );

        assertEquals("Dang ky thanh cong!", result);
        verify(patientService).createInitialProfileForAccount(any(Account.class), eq("Test User"), eq("0899370425"), eq(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "abc@", "abc@gmail", "abc.com"})
    void register_rejectsInvalidEmailFormat(String invalidEmail) {
        Account account = new Account(invalidEmail, "secret", "ROLE_PATIENT");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.register(account, "Test User", "0899370425", invalidEmail)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Email không hợp lệ", ex.getMessage());
    }

    @Test
    void normalizeLoginIdentifier_lowercasesAnyValidEmail() {
        assertEquals("triet.pham@hutech.edu.vn", authService.normalizeLoginIdentifier("Triet.Pham@Hutech.Edu.Vn"));
    }
}
