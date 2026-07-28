package br.com.systemcommerce.access.dto;

import java.util.List;
import java.util.UUID;

public record AccessCatalogResponse(List<ModuleNode> modules) {

    public record ModuleNode(
            UUID id, String code, String name, String description, String icon, Integer sortOrder, List<ResourceNode> resources) {}

    public record ResourceNode(
            UUID id, String code, String name, String description, String adminRoute, Integer sortOrder, List<ActionNode> actions) {}

    /**
     * Nó de permissão no catálogo (ação + metadados da Permission).
     * Nomes/códigos vêm do banco — o frontend não deve inventar labels.
     */
    public record ActionNode(
            UUID id,
            String code,
            String name,
            String description,
            String permissionCode,
            UUID permissionId,
            String riskLevel,
            String defaultScope,
            Boolean sensitive,
            Boolean requiresJustification,
            Boolean active) {}
}
