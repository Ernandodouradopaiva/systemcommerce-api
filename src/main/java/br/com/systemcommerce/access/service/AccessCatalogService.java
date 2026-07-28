package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.dto.AccessCatalogResponse;
import br.com.systemcommerce.access.dto.AccessCatalogResponse.ActionNode;
import br.com.systemcommerce.access.dto.AccessCatalogResponse.ModuleNode;
import br.com.systemcommerce.access.dto.AccessCatalogResponse.ResourceNode;
import br.com.systemcommerce.access.entity.SystemAction;
import br.com.systemcommerce.access.entity.SystemModule;
import br.com.systemcommerce.access.entity.SystemResource;
import br.com.systemcommerce.access.repository.SystemActionRepository;
import br.com.systemcommerce.access.repository.SystemModuleRepository;
import br.com.systemcommerce.access.repository.SystemResourceRepository;
import br.com.systemcommerce.user.entity.Permission;
import br.com.systemcommerce.user.repository.PermissionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessCatalogService {

    private static final Map<String, String> LEGACY_MODULE_TO_CATALOG = Map.ofEntries(
            Map.entry("USER", "ACCESS"),
            Map.entry("ROLE", "ACCESS"),
            Map.entry("ACCESS", "ACCESS"),
            Map.entry("CUSTOMER", "CADASTROS"),
            Map.entry("SUPPLIER", "CADASTROS"),
            Map.entry("EMPLOYEE", "CADASTROS"),
            Map.entry("SELLER", "CADASTROS"),
            Map.entry("SALESPERSON", "CADASTROS"),
            Map.entry("CATEGORY", "PRODUCTS"),
            Map.entry("PRODUCT", "PRODUCTS"),
            Map.entry("BRAND", "PRODUCTS"),
            Map.entry("MANUFACTURER", "PRODUCTS"),
            Map.entry("PRODUCT_LINE", "PRODUCTS"),
            Map.entry("PURCHASE", "PURCHASES"),
            Map.entry("PURCHASES", "PURCHASES"),
            Map.entry("INVENTORY", "INVENTORY"),
            Map.entry("STOCK", "INVENTORY"),
            Map.entry("SALE", "SALES"),
            Map.entry("SALES", "SALES"),
            Map.entry("QUOTE", "SALES"),
            Map.entry("POS", "POS"),
            Map.entry("CASH", "POS"),
            Map.entry("PAYMENT", "FINANCE"),
            Map.entry("FINANCE", "FINANCE"),
            Map.entry("FISCAL", "FISCAL"),
            Map.entry("REPORT", "REPORTS"),
            Map.entry("REPORTS", "REPORTS"),
            Map.entry("DASHBOARD", "REPORTS"),
            Map.entry("AUDIT", "AUDIT"),
            Map.entry("INTEGRATION", "INTEGRATIONS"),
            Map.entry("INTEGRATIONS", "INTEGRATIONS"),
            Map.entry("ADMIN", "ADMIN"),
            Map.entry("ORGANIZATION", "ADMIN"),
            Map.entry("STORE", "ADMIN"));

    private final SystemModuleRepository moduleRepository;
    private final SystemResourceRepository resourceRepository;
    private final SystemActionRepository actionRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Catálogo completo para administração de grupos: todas as permissões ativas,
     * agrupadas em Módulo → Recurso → Permissão. Permissões ainda sem resource_id
     * entram em recursos sintéticos para não sumirem da matriz.
     */
    @Transactional(readOnly = true)
    public AccessCatalogResponse getGroupedCatalog() {
        List<SystemModule> modules = moduleRepository.findByActiveTrueAndAdminVisibleTrueOrderBySortOrderAsc();
        Map<String, SystemModule> modulesByCode = modules.stream()
                .collect(Collectors.toMap(m -> m.getCode().toUpperCase(Locale.ROOT), m -> m, (a, b) -> a));

        List<SystemAction> allActions = actionRepository.findByActiveTrueOrderByCodeAsc();
        Map<UUID, SystemAction> actionsById =
                allActions.stream().collect(Collectors.toMap(SystemAction::getId, a -> a, (a, b) -> a, LinkedHashMap::new));
        Map<String, SystemAction> actionsByCode = allActions.stream()
                .collect(Collectors.toMap(a -> a.getCode().toUpperCase(Locale.ROOT), a -> a, (a, b) -> a));

        List<Permission> allPermissions = permissionRepository.findAllByActiveTrueOrderByModuleAscCodeAsc();
        Set<UUID> placed = new HashSet<>();

        Map<UUID, List<SystemResource>> resourcesByModule = new LinkedHashMap<>();
        for (SystemModule module : modules) {
            resourcesByModule.put(
                    module.getId(), resourceRepository.findByModuleIdAndActiveTrueOrderBySortOrderAsc(module.getId()));
        }

        Map<UUID, List<Permission>> linkedByResource = allPermissions.stream()
                .filter(p -> p.getResourceId() != null)
                .collect(Collectors.groupingBy(Permission::getResourceId, LinkedHashMap::new, Collectors.toList()));

        List<ModuleNode> moduleNodes = new ArrayList<>();
        for (SystemModule module : modules) {
            List<ResourceNode> resourceNodes = new ArrayList<>();
            List<SystemResource> resources = resourcesByModule.getOrDefault(module.getId(), List.of());

            for (SystemResource resource : resources) {
                List<Permission> perms = new ArrayList<>(linkedByResource.getOrDefault(resource.getId(), List.of()));
                perms.sort(Comparator.comparing(Permission::getCode, String.CASE_INSENSITIVE_ORDER));
                if (perms.isEmpty()) {
                    continue;
                }
                for (Permission p : perms) {
                    placed.add(p.getId());
                }
                resourceNodes.add(toResourceNode(resource, perms, actionsById, actionsByCode));
            }

            List<Permission> orphansInModule = allPermissions.stream()
                    .filter(p -> !placed.contains(p.getId()))
                    .filter(p -> belongsToModule(p, module, modulesByCode))
                    .sorted(Comparator.comparing(Permission::getCode, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            if (!orphansInModule.isEmpty()) {
                Map<String, List<Permission>> bySyntheticResource = new LinkedHashMap<>();
                for (Permission p : orphansInModule) {
                    String resourceCode = deriveResourceCode(p);
                    bySyntheticResource.computeIfAbsent(resourceCode, k -> new ArrayList<>()).add(p);
                }
                int sort = 900;
                for (Map.Entry<String, List<Permission>> entry : bySyntheticResource.entrySet()) {
                    String code = entry.getKey();
                    List<Permission> perms = entry.getValue();
                    for (Permission p : perms) {
                        placed.add(p.getId());
                    }
                    resourceNodes.add(new ResourceNode(
                            null,
                            code,
                            humanize(code),
                            "Permissões vinculadas automaticamente ao módulo " + module.getName(),
                            null,
                            sort++,
                            toActionNodes(perms, actionsById, actionsByCode)));
                }
            }

            if (!resourceNodes.isEmpty()) {
                moduleNodes.add(new ModuleNode(
                        module.getId(),
                        module.getCode(),
                        module.getName(),
                        module.getDescription(),
                        module.getIcon(),
                        module.getSortOrder(),
                        resourceNodes));
            }
        }

        List<Permission> leftover = allPermissions.stream()
                .filter(p -> !placed.contains(p.getId()))
                .sorted(Comparator.comparing(Permission::getCode, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (!leftover.isEmpty()) {
            Map<String, List<Permission>> byLegacyModule = new LinkedHashMap<>();
            for (Permission p : leftover) {
                String key = p.getModule() != null && !p.getModule().isBlank()
                        ? p.getModule().toUpperCase(Locale.ROOT)
                        : "OUTROS";
                byLegacyModule.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }
            List<ResourceNode> otherResources = new ArrayList<>();
            int sort = 10;
            for (Map.Entry<String, List<Permission>> entry : byLegacyModule.entrySet()) {
                otherResources.add(new ResourceNode(
                        null,
                        entry.getKey(),
                        humanize(entry.getKey()),
                        "Permissões sem vínculo completo no catálogo",
                        null,
                        sort++,
                        toActionNodes(entry.getValue(), actionsById, actionsByCode)));
            }
            moduleNodes.add(new ModuleNode(
                    null,
                    "OUTROS",
                    "Outros",
                    "Permissões ainda não classificadas no catálogo de módulos",
                    "folder",
                    9990,
                    otherResources));
        }

        return new AccessCatalogResponse(moduleNodes);
    }

    private boolean belongsToModule(Permission p, SystemModule module, Map<String, SystemModule> modulesByCode) {
        if (p.getModuleId() != null) {
            return p.getModuleId().equals(module.getId());
        }
        String catalogCode = resolveCatalogModuleCode(p.getModule(), p.getCode(), modulesByCode);
        return module.getCode().equalsIgnoreCase(catalogCode);
    }

    private String resolveCatalogModuleCode(String legacyModule, String permissionCode, Map<String, SystemModule> modulesByCode) {
        if (legacyModule != null && !legacyModule.isBlank()) {
            String upper = legacyModule.toUpperCase(Locale.ROOT);
            if (modulesByCode.containsKey(upper)) {
                return upper;
            }
            String mapped = LEGACY_MODULE_TO_CATALOG.get(upper);
            if (mapped != null && modulesByCode.containsKey(mapped)) {
                return mapped;
            }
        }
        if (permissionCode != null) {
            String prefix = permissionCode.toUpperCase(Locale.ROOT);
            if (prefix.startsWith("USER_") || prefix.startsWith("ROLE_") || prefix.startsWith("ACCESS_")) {
                return "ACCESS";
            }
            if (prefix.startsWith("CUSTOMER_") || prefix.startsWith("SUPPLIER_") || prefix.startsWith("EMPLOYEE_")
                    || prefix.startsWith("SELLER_") || prefix.startsWith("SALESPERSON_")) {
                return "CADASTROS";
            }
            if (prefix.startsWith("PRODUCT_") || prefix.startsWith("CATEGORY_") || prefix.startsWith("BRAND_")
                    || prefix.startsWith("MANUFACTURER_") || prefix.startsWith("PRODUCT_LINE_")) {
                return "PRODUCTS";
            }
            if (prefix.startsWith("PURCHASE_") || prefix.startsWith("SUPPLIER_RETURN_") || prefix.startsWith("QUOTE_REQUEST_")) {
                return "PURCHASES";
            }
            if (prefix.startsWith("INVENTORY_") || prefix.startsWith("STOCK_") || prefix.startsWith("TRANSFER_")) {
                return "INVENTORY";
            }
            if (prefix.startsWith("SALE_") || prefix.startsWith("SALES_") || prefix.startsWith("QUOTE_")) {
                return "SALES";
            }
            if (prefix.startsWith("POS_") || prefix.startsWith("CASH_")) {
                return "POS";
            }
            if (prefix.startsWith("FINANCE_") || prefix.startsWith("PAYABLE_") || prefix.startsWith("RECEIVABLE_")
                    || prefix.startsWith("PAYMENT_") || prefix.startsWith("RECONCILIATION_")) {
                return "FINANCE";
            }
            if (prefix.startsWith("FISCAL_") || prefix.startsWith("NFE_") || prefix.startsWith("NFCE_")) {
                return "FISCAL";
            }
            if (prefix.startsWith("REPORT_") || prefix.startsWith("DASHBOARD_") || prefix.startsWith("EXECUTIVE_")
                    || prefix.startsWith("ANALYTICS_")) {
                return "REPORTS";
            }
            if (prefix.startsWith("AUDIT_") || prefix.startsWith("ACCESS_AUDIT_")) {
                return "AUDIT";
            }
            if (prefix.startsWith("INTEGRATION_") || prefix.startsWith("WEBHOOK_") || prefix.startsWith("MARKETPLACE_")) {
                return "INTEGRATIONS";
            }
            if (prefix.startsWith("ORGANIZATION_") || prefix.startsWith("STORE_") || prefix.startsWith("ADMIN_")) {
                return "ADMIN";
            }
        }
        return "OUTROS";
    }

    private static String deriveResourceCode(Permission p) {
        String code = p.getCode() != null ? p.getCode().toUpperCase(Locale.ROOT) : "GENERAL";
        int idx = code.lastIndexOf('_');
        if (idx > 0) {
            return code.substring(0, idx);
        }
        if (p.getModule() != null && !p.getModule().isBlank()) {
            return p.getModule().toUpperCase(Locale.ROOT);
        }
        return "GENERAL";
    }

    private ResourceNode toResourceNode(
            SystemResource resource,
            List<Permission> perms,
            Map<UUID, SystemAction> actionsById,
            Map<String, SystemAction> actionsByCode) {
        return new ResourceNode(
                resource.getId(),
                resource.getCode(),
                resource.getName(),
                resource.getDescription(),
                resource.getAdminRoute(),
                resource.getSortOrder(),
                toActionNodes(perms, actionsById, actionsByCode));
    }

    private List<ActionNode> toActionNodes(
            List<Permission> perms, Map<UUID, SystemAction> actionsById, Map<String, SystemAction> actionsByCode) {
        List<ActionNode> actionNodes = new ArrayList<>();
        for (Permission perm : perms) {
            SystemAction action = perm.getActionId() != null ? actionsById.get(perm.getActionId()) : null;
            if (action == null) {
                String hint = extractActionHint(perm.getCode());
                action = actionsByCode.get(hint);
            }
            actionNodes.add(new ActionNode(
                    action != null ? action.getId() : null,
                    action != null ? action.getCode() : extractActionHint(perm.getCode()),
                    perm.getName(),
                    perm.getDescription() != null
                            ? perm.getDescription()
                            : (action != null ? action.getDescription() : null),
                    perm.getCode(),
                    perm.getId(),
                    perm.getRiskLevel() != null ? perm.getRiskLevel() : "MEDIUM",
                    "ORGANIZATION",
                    Boolean.TRUE.equals(perm.getSensitive()),
                    Boolean.TRUE.equals(perm.getRequiresJustification()),
                    Boolean.TRUE.equals(perm.getActive())));
        }
        return actionNodes;
    }

    private static String extractActionHint(String permissionCode) {
        if (permissionCode == null || !permissionCode.contains("_")) {
            return permissionCode;
        }
        return permissionCode.substring(permissionCode.lastIndexOf('_') + 1);
    }

    private static String humanize(String code) {
        if (code == null || code.isBlank()) {
            return "Geral";
        }
        String[] parts = code.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
