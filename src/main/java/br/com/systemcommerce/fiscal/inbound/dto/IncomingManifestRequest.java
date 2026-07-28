package br.com.systemcommerce.fiscal.inbound.dto;

import jakarta.validation.constraints.NotBlank;

public record IncomingManifestRequest(@NotBlank String manifestType, @NotBlank String justification) {}
