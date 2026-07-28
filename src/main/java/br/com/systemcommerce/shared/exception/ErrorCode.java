package br.com.systemcommerce.shared.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Recurso não encontrado"),
    CONFLICT(HttpStatus.CONFLICT, "Conflito de dados"),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "Violação de regra de negócio"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Erro de validação"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Acesso negado"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Não autenticado"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token inválido"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token expirado"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "Violação de integridade dos dados"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não permitido"),
    MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Limite de tentativas excedido"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor"),
    STORE_CONTEXT_REQUIRED(HttpStatus.BAD_REQUEST, "Contexto de loja obrigatório"),
    STORE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Acesso à loja negado"),
    STORE_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "Loja inativa"),
    RESOURCE_BELONGS_TO_ANOTHER_STORE(HttpStatus.FORBIDDEN, "Recurso pertence a outra loja"),
    STORE_CONTEXT_INVALID(HttpStatus.BAD_REQUEST, "Contexto de loja inválido");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
