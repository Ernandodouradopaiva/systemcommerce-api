package br.com.systemcommerce.fiscal.transmission.dto;

public record AuthorizationResult(
        boolean success,
        String cstat,
        String xmotivo,
        String protocolNumber,
        String receiptNumber,
        String authorizedXml,
        long latencyMs) {}
