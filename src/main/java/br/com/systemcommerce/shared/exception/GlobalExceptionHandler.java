package br.com.systemcommerce.shared.exception;

import br.com.systemcommerce.shared.web.CorrelationIdContext;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        log.warn(
                "Erro de negocio code={} path={} correlationId={} message={}",
                ex.getErrorCode().name(),
                request.getRequestURI(),
                CorrelationIdContext.current(),
                ex.getMessage());
        return build(ex.getErrorCode(), ex.getMessage(), request, ex.getDetails());
    }

    @ExceptionHandler({EntityNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
            Exception ex, HttpServletRequest request) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage(), request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest request) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, "Endpoint não encontrado", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .collect(Collectors.toList());
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldErrorDetail> details = ex.getConstraintViolations().stream()
                .map(v -> ApiErrorResponse.FieldErrorDetail.builder()
                        .field(v.getPropertyPath().toString())
                        .message(v.getMessage())
                        .rejectedValue(safeRejected(v.getPropertyPath().toString(), v.getInvalidValue()))
                        .build())
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request, details);
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        ErrorCode code = ex instanceof HttpMessageNotReadableException
                ? ErrorCode.MESSAGE_NOT_READABLE
                : ErrorCode.VALIDATION_ERROR;
        return build(code, code.getDefaultMessage(), request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getDefaultMessage(), request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn(
                "Violacao de integridade path={} correlationId={}",
                request.getRequestURI(),
                CorrelationIdContext.current());
        return build(
                ErrorCode.DATA_INTEGRITY_VIOLATION,
                ErrorCode.DATA_INTEGRITY_VIOLATION.getDefaultMessage(),
                request,
                null);
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiErrorResponse> handleAuth(Exception ex, HttpServletRequest request) {
        return build(ErrorCode.UNAUTHENTICATED, ErrorCode.UNAUTHENTICATED.getDefaultMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage(), request, null);
    }

    @ExceptionHandler({
        ObjectOptimisticLockingFailureException.class,
        OptimisticLockException.class
    })
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(Exception ex, HttpServletRequest request) {
        log.warn(
                "Conflito de concorrência path={} correlationId={}",
                request.getRequestURI(),
                CorrelationIdContext.current());
        return build(ErrorCode.CONFLICT, "Conflito de concorrência. Tente novamente.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error(
                "Erro interno path={} correlationId={}",
                request.getRequestURI(),
                CorrelationIdContext.current(),
                ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), request, null);
    }

    private ApiErrorResponse.FieldErrorDetail toDetail(FieldError error) {
        return ApiErrorResponse.FieldErrorDetail.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .rejectedValue(safeRejected(error.getField(), error.getRejectedValue()))
                .build();
    }

    private String safeRejected(String field, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveField(field)) {
            return "[protegido]";
        }
        String text = String.valueOf(value);
        if (text.length() > 100) {
            return text.substring(0, 100) + "...";
        }
        String lower = text.toLowerCase();
        if (lower.contains("password") || lower.contains("senha") || lower.contains("token")) {
            return "[protegido]";
        }
        return text;
    }

    private boolean isSensitiveField(String field) {
        if (field == null || field.isBlank()) {
            return false;
        }
        String lower = field.toLowerCase();
        return lower.contains("password")
                || lower.contains("senha")
                || lower.contains("token")
                || lower.contains("secret")
                || lower.contains("refresh");
    }

    private ResponseEntity<ApiErrorResponse> build(
            ErrorCode errorCode,
            String message,
            HttpServletRequest request,
            List<ApiErrorResponse.FieldErrorDetail> details) {
        HttpStatus status = errorCode.getHttpStatus();
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(errorCode.name())
                .message(message)
                .path(request.getRequestURI())
                .correlationId(CorrelationIdContext.current())
                .details(details)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
