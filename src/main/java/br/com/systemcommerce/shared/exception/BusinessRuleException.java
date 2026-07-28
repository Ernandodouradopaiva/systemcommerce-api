package br.com.systemcommerce.shared.exception;

public class BusinessRuleException extends BusinessException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
