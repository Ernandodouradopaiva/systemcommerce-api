package br.com.systemcommerce.settings.service;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pos.terminal.repository.PosTerminalRepository;
import br.com.systemcommerce.pricing.entity.StoreGroup;
import br.com.systemcommerce.pricing.repository.StoreGroupMemberRepository;
import br.com.systemcommerce.pricing.repository.StoreGroupRepository;
import br.com.systemcommerce.settings.dto.SystemEffectiveSettingResponse;
import br.com.systemcommerce.settings.dto.SystemSettingCopyRequest;
import br.com.systemcommerce.settings.dto.SystemSettingResponse;
import br.com.systemcommerce.settings.dto.SystemSettingUpsertRequest;
import br.com.systemcommerce.settings.entity.SystemSetting;
import br.com.systemcommerce.settings.entity.SystemSettingKeys;
import br.com.systemcommerce.settings.entity.SystemSettingScope;
import br.com.systemcommerce.settings.repository.SystemSettingRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository settingRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final StoreRepository storeRepository;
    private final StoreGroupRepository storeGroupRepository;
    private final StoreGroupMemberRepository storeGroupMemberRepository;
    private final PosTerminalRepository terminalRepository;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public SystemEffectiveSettingResponse effective(
            String settingKey,
            UUID organizationId,
            UUID storeGroupId,
            UUID storeId,
            UUID terminalId,
            UUID userId) {
        assertRead();
        return resolveEffective(settingKey, organizationId, storeGroupId, storeId, terminalId, userId);
    }

    @Transactional(readOnly = true)
    public SystemEffectiveSettingResponse origin(
            String settingKey,
            UUID organizationId,
            UUID storeGroupId,
            UUID storeId,
            UUID terminalId,
            UUID userId) {
        assertRead();
        return resolveEffective(settingKey, organizationId, storeGroupId, storeId, terminalId, userId);
    }

    /** Resolução sem checagem de permissão — uso interno por serviços. */
    @Transactional(readOnly = true)
    public SystemEffectiveSettingResponse resolveEffective(
            String settingKey,
            UUID organizationId,
            UUID storeGroupId,
            UUID storeId,
            UUID terminalId,
            UUID userId) {
        String key = normalizeKey(settingKey);
        assertKnownKey(key);
        ResolvedContext ctx = resolveContext(organizationId, storeGroupId, storeId, terminalId);

        if (userId != null && SystemSettingKeys.USER_SCOPE_WHITELIST.contains(key)) {
            var userOverride = settingRepository.findActiveOverride(
                    key, SystemSettingScope.USER, null, null, null, null, userId);
            if (userOverride.isPresent()) {
                return toEffective(key, userOverride.get());
            }
        }

        if (ctx.terminalId() != null) {
            var terminalOverride = settingRepository.findActiveOverride(
                    key, SystemSettingScope.TERMINAL, null, null, null, ctx.terminalId(), null);
            if (terminalOverride.isPresent()) {
                return toEffective(key, terminalOverride.get());
            }
        }

        if (ctx.storeId() != null) {
            var storeOverride = settingRepository.findActiveOverride(
                    key, SystemSettingScope.STORE, null, null, ctx.storeId(), null, null);
            if (storeOverride.isPresent()) {
                return toEffective(key, storeOverride.get());
            }
        }

        if (ctx.storeGroupId() != null) {
            var groupOverride = settingRepository.findActiveOverride(
                    key, SystemSettingScope.STORE_GROUP, null, ctx.storeGroupId(), null, null, null);
            if (groupOverride.isPresent()) {
                return toEffective(key, groupOverride.get());
            }
        }

        var orgOverride = settingRepository.findActiveOverride(
                key, SystemSettingScope.ORGANIZATION, ctx.organizationId(), null, null, null, null);
        if (orgOverride.isPresent()) {
            return toEffective(key, orgOverride.get());
        }

        return new SystemEffectiveSettingResponse(
                key, defaultValue(key), SystemSettingScope.ORGANIZATION, null, defaultValue(key));
    }

    @Transactional(readOnly = true)
    public boolean getEffectiveBoolean(
            String settingKey,
            UUID organizationId,
            UUID storeGroupId,
            UUID storeId,
            UUID terminalId,
            boolean fallback) {
        try {
            return Boolean.parseBoolean(resolveEffective(
                            settingKey, organizationId, storeGroupId, storeId, terminalId, null)
                    .value());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    @Transactional
    public SystemSettingResponse upsert(SystemSettingUpsertRequest request) {
        assertManage();
        String key = normalizeKey(request.settingKey());
        assertKnownKey(key);
        assertScopeAllowed(key, request.scope());
        assertScopeRefs(request);

        var existing = settingRepository.findActiveOverride(
                key,
                request.scope(),
                request.organizationId(),
                request.storeGroupId(),
                request.storeId(),
                request.terminalId(),
                request.userId());

        if (existing.isEmpty()) {
            SystemSetting created = new SystemSetting();
            created.setSettingKey(key);
            created.setScope(request.scope());
            bindScopeRefs(created, request);
            created.setValueText(request.value().trim());
            created.setActive(true);
            SystemSetting saved = settingRepository.saveAndFlush(created);
            audit(saved, AuditLog.AuditAction.CREATE, null, "Configuração criada");
            return toResponse(saved);
        }

        SystemSetting current = existing.get();
        if (request.expectedVersion() != null && !request.expectedVersion().equals(current.getVersion())) {
            throw new ConflictException("Versão da configuração desatualizada; recarregue e tente novamente");
        }
        Map<String, Object> before = snapshot(current);
        current.setValueText(request.value().trim());
        SystemSetting saved = settingRepository.saveAndFlush(current);
        audit(saved, AuditLog.AuditAction.UPDATE, before, "Configuração atualizada");
        return toResponse(saved);
    }

    @Transactional
    public void restoreInheritance(UUID storeId, String settingKey) {
        assertManage();
        String key = normalizeKey(settingKey);
        var existing = settingRepository.findActiveOverride(
                key, SystemSettingScope.STORE, null, null, storeId, null, null);
        SystemSetting setting = existing.orElseThrow(() -> new ResourceNotFoundException(
                "Override de configuração da loja", storeId + "/" + key));
        Map<String, Object> before = snapshot(setting);
        setting.setActive(false);
        settingRepository.saveAndFlush(setting);
        audit(setting, AuditLog.AuditAction.DELETE, before, "Herança restaurada (override de loja removido)");
    }

    @Transactional
    public List<SystemSettingResponse> copyBetweenStores(SystemSettingCopyRequest request) {
        assertManage();
        Store source = storeService.requireUsable(request.sourceStoreId());
        Store target = storeService.requireUsable(request.targetStoreId());
        if (source.getId().equals(target.getId())) {
            throw new BusinessRuleException("Loja origem e destino devem ser diferentes");
        }
        if (source.getOrganization() == null
                || target.getOrganization() == null
                || !source.getOrganization().getId().equals(target.getOrganization().getId())) {
            throw new BusinessRuleException("Lojas devem pertencer à mesma organização");
        }

        List<SystemSettingResponse> copied = new ArrayList<>();
        for (SystemSetting sourceSetting : settingRepository.findActiveByStore(source.getId())) {
            if (sourceSetting.getScope() != SystemSettingScope.STORE) {
                continue;
            }
            SystemSettingResponse saved = upsert(new SystemSettingUpsertRequest(
                    sourceSetting.getSettingKey(),
                    SystemSettingScope.STORE,
                    null,
                    null,
                    target.getId(),
                    null,
                    null,
                    sourceSetting.getValueText(),
                    null));
            copied.add(saved);
        }
        return copied;
    }

    private ResolvedContext resolveContext(
            UUID organizationId, UUID storeGroupId, UUID storeId, UUID terminalId) {
        UUID resolvedStoreId = storeId;
        UUID resolvedOrgId = organizationId;
        UUID resolvedGroupId = storeGroupId;

        if (terminalId != null) {
            PosTerminal terminal = terminalRepository
                    .findById(terminalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Terminal", terminalId));
            if (terminal.getStore() != null) {
                resolvedStoreId = terminal.getStore().getId();
                if (terminal.getStore().getOrganization() != null) {
                    resolvedOrgId = terminal.getStore().getOrganization().getId();
                }
            }
        } else if (storeId != null) {
            Store store = storeRepository
                    .findById(storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Loja", storeId));
            if (store.getOrganization() != null) {
                resolvedOrgId = store.getOrganization().getId();
            }
        }

        if (resolvedOrgId == null) {
            resolvedOrgId = organizationService.requireDefault().getId();
        }

        if (resolvedGroupId == null && resolvedStoreId != null) {
            List<UUID> groups = storeGroupMemberRepository.findActiveGroupIdsByStoreId(resolvedStoreId);
            if (!groups.isEmpty()) {
                resolvedGroupId = groups.getFirst();
            }
        }

        return new ResolvedContext(resolvedOrgId, resolvedGroupId, resolvedStoreId, terminalId);
    }

    private void assertKnownKey(String key) {
        if (!SystemSettingKeys.DEFAULTS.containsKey(key)) {
            throw new BusinessRuleException("Chave de configuração desconhecida: " + key);
        }
    }

    private void assertScopeAllowed(String key, SystemSettingScope scope) {
        if (scope == SystemSettingScope.USER && !SystemSettingKeys.USER_SCOPE_WHITELIST.contains(key)) {
            throw new BusinessRuleException("Chave " + key + " não permite escopo USER");
        }
    }

    private void assertScopeRefs(SystemSettingUpsertRequest request) {
        switch (request.scope()) {
            case ORGANIZATION -> {
                UUID orgId = request.organizationId() != null
                        ? request.organizationId()
                        : organizationService.requireDefault().getId();
                organizationService.getById(orgId);
                if (request.storeGroupId() != null
                        || request.storeId() != null
                        || request.terminalId() != null
                        || request.userId() != null) {
                    throw new BusinessRuleException("Escopo ORGANIZATION não aceita refs de loja/terminal/usuário");
                }
            }
            case STORE_GROUP -> {
                if (request.storeGroupId() == null) {
                    throw new BusinessRuleException("Escopo STORE_GROUP exige storeGroupId");
                }
                storeGroupRepository
                        .findById(request.storeGroupId())
                        .orElseThrow(() -> new ResourceNotFoundException("Grupo de lojas", request.storeGroupId()));
            }
            case STORE -> {
                if (request.storeId() == null) {
                    throw new BusinessRuleException("Escopo STORE exige storeId");
                }
                storeService.requireUsable(request.storeId());
            }
            case TERMINAL -> {
                if (request.terminalId() == null) {
                    throw new BusinessRuleException("Escopo TERMINAL exige terminalId");
                }
                terminalRepository
                        .findById(request.terminalId())
                        .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.terminalId()));
            }
            case USER -> {
                if (request.userId() == null) {
                    throw new BusinessRuleException("Escopo USER exige userId");
                }
                userRepository
                        .findById(request.userId())
                        .orElseThrow(() -> new ResourceNotFoundException("Usuário", request.userId()));
            }
        }
    }

    private void bindScopeRefs(SystemSetting setting, SystemSettingUpsertRequest request) {
        switch (request.scope()) {
            case ORGANIZATION -> {
                UUID orgId = request.organizationId() != null
                        ? request.organizationId()
                        : organizationService.requireDefault().getId();
                setting.setOrganization(organizationRepository
                        .findById(orgId)
                        .orElseThrow(() -> new ResourceNotFoundException("Organização", orgId)));
            }
            case STORE_GROUP -> setting.setStoreGroup(storeGroupRepository
                    .findById(request.storeGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Grupo de lojas", request.storeGroupId())));
            case STORE -> setting.setStore(storeRepository
                    .findById(request.storeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loja", request.storeId())));
            case TERMINAL -> setting.setTerminal(terminalRepository
                    .findById(request.terminalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Terminal", request.terminalId())));
            case USER -> setting.setUser(userRepository
                    .findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário", request.userId())));
        }
    }

    private SystemEffectiveSettingResponse toEffective(String key, SystemSetting setting) {
        return new SystemEffectiveSettingResponse(
                key,
                setting.getValueText(),
                setting.getScope(),
                setting.getId(),
                defaultValue(key));
    }

    private String defaultValue(String key) {
        return SystemSettingKeys.DEFAULTS.getOrDefault(key, "");
    }

    private String normalizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new BusinessRuleException("settingKey é obrigatório");
        }
        return key.trim().toUpperCase();
    }

    private SystemSettingResponse toResponse(SystemSetting setting) {
        return new SystemSettingResponse(
                setting.getId(),
                setting.getSettingKey(),
                setting.getScope(),
                setting.getOrganization() != null ? setting.getOrganization().getId() : null,
                setting.getStoreGroup() != null ? setting.getStoreGroup().getId() : null,
                setting.getStore() != null ? setting.getStore().getId() : null,
                setting.getTerminal() != null ? setting.getTerminal().getId() : null,
                setting.getUser() != null ? setting.getUser().getId() : null,
                setting.getValueText(),
                setting.getActive(),
                setting.getVersion(),
                setting.getCreatedAt(),
                setting.getUpdatedAt());
    }

    private void audit(SystemSetting setting, AuditLog.AuditAction action, Map<String, Object> before, String message) {
        domainAuditService.record(
                "SETTINGS",
                "SystemSetting",
                setting.getId(),
                action,
                before,
                snapshot(setting),
                message);
    }

    private Map<String, Object> snapshot(SystemSetting setting) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("settingKey", setting.getSettingKey());
        map.put("scope", setting.getScope());
        map.put("value", setting.getValueText());
        return map;
    }

    private void assertRead() {
        // enforced by @PreAuthorize on controller
    }

    private void assertManage() {
        // enforced by @PreAuthorize on controller
    }

    private record ResolvedContext(UUID organizationId, UUID storeGroupId, UUID storeId, UUID terminalId) {}
}
