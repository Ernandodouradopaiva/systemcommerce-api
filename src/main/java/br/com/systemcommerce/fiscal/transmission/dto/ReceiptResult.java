package br.com.systemcommerce.fiscal.transmission.dto;

public record ReceiptResult(boolean success, String cstat, String xmotivo, String protocolNumber, long latencyMs) {}
