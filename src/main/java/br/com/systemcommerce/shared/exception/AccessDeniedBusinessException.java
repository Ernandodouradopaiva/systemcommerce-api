package br.com.systemcommerce.shared.exception;

public class AccessDeniedBusinessException extends BusinessException {

    public AccessDeniedBusinessException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }
}
