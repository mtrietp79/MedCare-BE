package com.medcare.clinic_backend.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String FOLLOW_UP_VALIDATION_CODE = "FOLLOW_UP_VALIDATION_ERROR";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "Du lieu khong hop le." : error.getDefaultMessage())
                .findFirst()
                .orElse("Du lieu khong hop le.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        if (ex.getCode() != null && !ex.getCode().isBlank()) {
            body.put("code", ex.getCode());
        }
        if (FOLLOW_UP_VALIDATION_CODE.equals(ex.getCode()) || !ex.getFieldErrors().isEmpty()) {
            body.put("fieldErrors", ex.getFieldErrors());
        }
        return ResponseEntity.status(ex.getStatus())
                .body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        logger.error("Data integrity violation", ex);
        if (isFollowUpEndpoint(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildFollowUpValidationBody(
                            "Khong the tao lich tai kham do du lieu lich hen goc khong hop le hoac da bi trung.",
                            Map.of()
                    ));
        }
        if (isDoctorCompleteEndpoint(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Khong the hoan tat lich kham do du lieu lich hen hoac benh an khong hop le."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Du lieu dang ky khong hop le hoac da ton tai (email/so dien thoai/username)."));
    }

    @ExceptionHandler({JpaObjectRetrievalFailureException.class, EntityNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleEntityReferenceException(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        logger.error("Entity reference resolution failed", ex);
        if (isFollowUpEndpoint(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildFollowUpValidationBody(
                            "Du lieu lich hen goc hoac lien ket tai kham khong hop le.",
                            Map.of()
                    ));
        }
        if (isDoctorCompleteEndpoint(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Du lieu lich hen, benh an hoac lien ket tai kham khong hop le."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Du lieu lien ket khong hop le hoac khong con ton tai."));
    }

    @ExceptionHandler({JpaSystemException.class, TransactionSystemException.class, UnexpectedRollbackException.class})
    public ResponseEntity<Map<String, Object>> handlePersistenceSystemException(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        logger.error("Persistence system exception", ex);
        if (isFollowUpEndpoint(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildFollowUpValidationBody(
                            "Khong the tao lich tai kham do du lieu lien ket hoac rang buoc luu tru khong hop le.",
                            Map.of()
                    ));
        }
        if (isDoctorCompleteEndpoint(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Khong the hoan tat lich kham do du lieu benh an hoac lich tai kham khong hop le."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Khong the luu du lieu do rang buoc he thong khong hop le."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        logger.error("Malformed request payload", ex);
        if (isFollowUpEndpoint(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildFollowUpValidationBody(
                            "Du lieu gui len khong dung dinh dang JSON hoac sai kieu du lieu.",
                            Map.of()
                    ));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.<String, Object>of("message", "Du lieu gui len khong dung dinh dang JSON hoac sai ten truong."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String parameterName = ex.getName() == null ? "tham so" : ex.getName();
        if (isFollowUpEndpoint(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildFollowUpValidationBody(
                            "Gia tri cua '" + parameterName + "' khong hop le.",
                            Map.of()
                    ));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.<String, Object>of("message", "Gia tri cua '" + parameterName + "' khong hop le."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Ten dang nhap hoac mat khau khong chinh xac."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Ban khong co quyen thuc hien thao tac nay."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        logger.error("Unhandled runtime exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Da xay ra loi he thong. Vui long thu lai sau."));
    }

    private boolean isFollowUpEndpoint(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri != null
                && uri.contains("/api/doctor/medical-records/")
                && uri.endsWith("/follow-up");
    }

    private boolean isDoctorCompleteEndpoint(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri != null
                && uri.contains("/api/doctor/appointments/")
                && uri.endsWith("/complete");
    }

    private Map<String, Object> buildFollowUpValidationBody(String message, Map<String, String> fieldErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("code", FOLLOW_UP_VALIDATION_CODE);
        body.put("fieldErrors", fieldErrors == null ? Map.of() : fieldErrors);
        return body;
    }
}
