package br.com.systemcommerce.user.dto;

import java.util.UUID;

public record RoleSummaryResponse(UUID id, String code, String name, String description) {}
