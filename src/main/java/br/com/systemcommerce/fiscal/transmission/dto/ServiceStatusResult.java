package br.com.systemcommerce.fiscal.transmission.dto;

public record ServiceStatusResult(
        boolean available, String cstat, String xmotivo, long latencyMs, boolean circuitOpen) {}
