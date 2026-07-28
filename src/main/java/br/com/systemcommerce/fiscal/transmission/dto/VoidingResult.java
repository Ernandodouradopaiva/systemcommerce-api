package br.com.systemcommerce.fiscal.transmission.dto;

public record VoidingResult(
        boolean success, String cstat, String xmotivo, String protocolNumber, String xmlRef, long latencyMs) {}
