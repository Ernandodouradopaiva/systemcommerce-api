package br.com.systemcommerce.user.dto;

import java.util.UUID;

public record PermissionSummaryResponse(UUID id, String code, String name, String module) {}
