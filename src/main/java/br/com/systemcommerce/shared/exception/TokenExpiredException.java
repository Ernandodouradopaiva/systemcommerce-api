package br.com.systemcommerce.shared.exception;

public class TokenExpiredException extends BusinessException {

    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message);
    }
}
