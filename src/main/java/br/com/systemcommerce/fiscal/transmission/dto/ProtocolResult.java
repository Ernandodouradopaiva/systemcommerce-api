package br.com.systemcommerce.fiscal.transmission.dto;

public record ProtocolResult(
        boolean authorized,
        String cstat,
        String xmotivo,
        String protocolNumber,
        String accessKey,
        long latencyMs) {}
