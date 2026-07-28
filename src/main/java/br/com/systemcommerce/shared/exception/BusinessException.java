package br.com.systemcommerce.shared.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;
    private final List<ApiErrorResponse.FieldErrorDetail> details;

    public BusinessException(String message) {
        this(ErrorCode.BUSINESS_RULE_VIOLATION, message, null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(
            ErrorCode errorCode, String message, List<ApiErrorResponse.FieldErrorDetail> details) {
        super(message != null ? message : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.status = errorCode.getHttpStatus();
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<ApiErrorResponse.FieldErrorDetail> getDetails() {
        return details;
    }
}
