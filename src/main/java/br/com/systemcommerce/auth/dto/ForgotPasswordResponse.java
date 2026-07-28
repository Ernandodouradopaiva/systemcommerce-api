package br.com.systemcommerce.auth.dto;

/** Resposta interna de forgot password (em teste pode expor token). */
public record ForgotPasswordResponse(String message, String resetToken) {}
