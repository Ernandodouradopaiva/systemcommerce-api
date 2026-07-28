package br.com.systemcommerce.fiscal.transmission.dto;

public record EventResult(
        boolean success, String cstat, String xmotivo, String protocolNumber, String eventXml, long latencyMs) {}
