package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PatientService patientService;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Environment environment;

    @InjectMocks
    private PasswordService passwordService;

    private Account patientAccount;

    @BeforeEach
    void setUp() {
        patientAccount = new Account();
        patientAccount.setId(1);
        patientAccount.setUsername("patient@gmail.com");
        patientAccount.setPassword("encoded");
        patientAccount.setRole("ROLE_PATIENT");
        patientAccount.setIsTestAccount(false);
        patientAccount.setMustChangePassword(false);
    }

    @Test
    void requestOtp_sendsForRealPatientEmail() {
        when(accountRepository.findByUsername("patient@gmail.com")).thenReturn(Optional.of(patientAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(environment.getActiveProfiles()).thenReturn(new String[]{});

        Map<String, String> response = passwordService.requestForgotPasswordOtp("patient@gmail.com");

        assertEquals(PasswordService.GENERIC_OTP_MESSAGE, response.get("message"));
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertNotNull(captor.getValue().getResetOtp());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void requestOtp_returnsGenericMessageWhenEmailNotFound() {
        when(accountRepository.findByUsername("missing@gmail.com")).thenReturn(Optional.empty());
        when(patientService.findLinkedAccountByEmail("missing@gmail.com")).thenReturn(null);

        Map<String, String> response = passwordService.requestForgotPasswordOtp("missing@gmail.com");

        assertEquals(PasswordService.GENERIC_OTP_MESSAGE, response.get("message"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void requestOtp_blocksTestAccount() {
        patientAccount.setIsTestAccount(true);
        when(accountRepository.findByUsername("fake@gmail.com")).thenReturn(Optional.of(patientAccount));

        Map<String, String> response = passwordService.requestForgotPasswordOtp("fake@gmail.com");

        assertEquals(PasswordService.ADMIN_REQUIRED_MESSAGE, response.get("message"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void requestOtp_blocksDoctorAccount() {
        patientAccount.setUsername("doctor1");
        patientAccount.setRole("ROLE_DOCTOR");
        when(accountRepository.findByUsername("doctor1@gmail.com")).thenReturn(Optional.empty());
        when(patientService.findLinkedAccountByEmail("doctor1@gmail.com")).thenReturn(patientAccount);

        Map<String, String> response = passwordService.requestForgotPasswordOtp("doctor1@gmail.com");

        assertEquals(PasswordService.ADMIN_REQUIRED_MESSAGE, response.get("message"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void verifyOtp_returnsResetTokenWhenValid() {
        patientAccount.setResetOtp("123456");
        patientAccount.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        when(accountRepository.findByUsername("patient@gmail.com")).thenReturn(Optional.of(patientAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> response = passwordService.verifyForgotPasswordOtp("patient@gmail.com", "123456");

        assertEquals("Xác nhận OTP thành công", response.get("message"));
        assertNotNull(response.get("resetToken"));
    }

    @Test
    void verifyOtp_rejectsInvalidOtp() {
        patientAccount.setResetOtp("123456");
        patientAccount.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        when(accountRepository.findByUsername("patient@gmail.com")).thenReturn(Optional.of(patientAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordService.verifyForgotPasswordOtp("patient@gmail.com", "000000")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Mã OTP không hợp lệ.", ex.getMessage());
    }

    @Test
    void resetWithToken_updatesPasswordAndClearsRecoveryState() {
        patientAccount.setResetToken("token-abc");
        patientAccount.setResetTokenExpiryTime(LocalDateTime.now().plusMinutes(10));
        when(accountRepository.findByResetToken("token-abc")).thenReturn(Optional.of(patientAccount));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new-encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> response = passwordService.resetPasswordWithToken(
                "token-abc",
                "NewPass@123",
                "NewPass@123"
        );

        assertTrue(response.get("message").contains("Đặt lại mật khẩu thành công"));
        assertEquals("new-encoded", patientAccount.getPassword());
        assertFalse(Boolean.TRUE.equals(patientAccount.getMustChangePassword()));
        assertEquals(null, patientAccount.getResetToken());
    }

    @Test
    void changePassword_rejectsWrongOldPassword() {
        when(accountRepository.findByUsername("patient@gmail.com")).thenReturn(Optional.of(patientAccount));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                passwordService.changePassword("patient@gmail.com", "wrong", "NewPass@123", "NewPass@123")
        );

        assertEquals("Mật khẩu cũ không chính xác.", ex.getMessage());
    }

    @Test
    void adminResetPassword_setsMustChangePasswordTrue() {
        when(accountRepository.findById(1)).thenReturn(Optional.of(patientAccount));
        when(passwordEncoder.encode("Temp@123456")).thenReturn("temp-encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = passwordService.adminResetPassword(1, "Temp@123456");

        assertEquals("Reset mật khẩu thành công", response.get("message"));
        assertEquals("Temp@123456", response.get("temporaryPassword"));
        assertEquals(true, response.get("mustChangePassword"));
        assertTrue(Boolean.TRUE.equals(patientAccount.getMustChangePassword()));
    }
}
