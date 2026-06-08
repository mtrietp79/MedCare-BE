package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AdminResetPasswordRequest;
import com.medcare.clinic_backend.service.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAccountController {

    @Autowired
    private PasswordService passwordService;

    @PostMapping("/{accountId}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable Integer accountId,
            @RequestBody(required = false) AdminResetPasswordRequest request
    ) {
        String temporaryPassword = request == null ? null : request.getTemporaryPassword();
        return ResponseEntity.ok(passwordService.adminResetPassword(accountId, temporaryPassword));
    }
}
