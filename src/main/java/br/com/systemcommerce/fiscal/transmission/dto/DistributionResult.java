package br.com.systemcommerce.fiscal.transmission.dto;

public record DistributionResult(boolean success, String cstat, String xmotivo, long latencyMs) {}
