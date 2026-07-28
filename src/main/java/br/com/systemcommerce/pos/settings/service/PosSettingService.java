package br.com.systemcommerce.pos.settings.service;

import br.com.systemcommerce.pos.audit.PosAuditContext;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.settings.dto.PosEffectiveSettingItem;
import br.com.systemcommerce.pos.settings.dto.PosEffectiveSettingsResponse;
import br.com.systemcommerce.pos.settings.dto.PosSettingDefinitionResponse;
import br.com.systemcommerce.pos.settings.dto.PosSettingHistoryResponse;
import br.com.systemcommerce.pos.settings.dto.PosSettingResponse;
import br.com.systemcommerce.pos.settings.dto.PosSettingUpsertRequest;
import br.com.systemcommerce.pos.settings.dto.PosSettingValidateRequest;
import br.com.systemcommerce.pos.settings.dto.PosSettingValidateResponse;
import br.com.systemcommerce.pos.settings.entity.PosSetting;
import br.com.systemcommerce.pos.settings.entity.PosSettingDefinition;
import br.com.systemcommerce.pos.settings.entity.PosSettingHistory;
import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import br.com.systemcommerce.pos.settings.repository.PosSettingDefinitionRepository;
import br.com.systemcommerce.pos.settings.repository.PosSettingHistoryRepository;
import br.com.systemcommerce.pos.settings.repository.PosSettingRepository;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pos.terminal.repository.PosTerminalRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.shared.web.CorrelationIdContext;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Administração e resolução de configurações do PDV (terminal &gt; loja &gt; global &gt; default).
 */
@Service
@RequiredArgsConstructor
public class PosSettingService {

    private final PosSettingDefinitionRepository definitionRepository;
    private final PosSettingRepository settingRepository;
    private final PosSettingHistoryRepository historyRepository;
    private final StoreRepository storeRepository;
    private final PosTerminalRepository terminalRepository;
    private final UserRepository userRepository;
    private final PosSettingValidator validator;
    private final DomainAuditService domainAuditService;
    private final PosAuditService posAuditService;

    @Transactional(readOnly = true)
    public List<PosSettingDefinitionResponse> listDefinitions() {
        assertRead();
        return definitionRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toDefinitionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PosSettingResponse> list(
            PosSettingScope scope, UUID storeId, UUID terminalId, String settingKey, Pageable pageable) {
        assertRead();
        Specification<PosSetting> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.isTrue(root.get("active")));
            if (scope != null) {
                preds.add(cb.equal(root.get("scope"), scope));
            }
            if (storeId != null) {
                preds.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (terminalId != null) {
                preds.add(cb.equal(root.get("terminal").get("id"), terminalId));
            }
            if (StringUtils.hasText(settingKey)) {
                preds.add(cb.equal(root.get("settingKey"), settingKey.trim()));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        return settingRepository.findAll(spec, pageable).map(this::toSettingResponse);
    }

    @Transactional(readOnly = true)
    public PosSettingResponse getById(UUID id) {
        assertRead();
        PosSetting setting = settingRepository
                .findActiveDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração PDV", id));
        return toSettingResponse(setting);
    }

    @Transactional(readOnly = true)
    public PosEffectiveSettingsResponse effective(UUID storeId, UUID terminalId) {
        assertRead();
        return resolveEffective(storeId, terminalId);
    }

    /** Resolução sem checagem de permissão — uso interno por serviços do PDV. */
    @Transactional(readOnly = true)
    public PosEffectiveSettingsResponse resolveEffective(UUID storeId, UUID terminalId) {
        UUID resolvedStoreId = storeId;
        if (terminalId != null) {
            PosTerminal terminal = terminalRepository
                    .findById(terminalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Terminal", terminalId));
            if (terminal.getStore() != null) {
                resolvedStoreId = terminal.getStore().getId();
            }
        } else if (storeId != null) {
            storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("Loja", storeId));
        }

        Map<String, PosSetting> globals = indexByKey(settingRepository.findActiveGlobals());
        Map<String, PosSetting> storeMap =
                resolvedStoreId == null ? Map.of() : indexByKey(settingRepository.findActiveByStore(resolvedStoreId));
        Map<String, PosSetting> terminalMap =
                terminalId == null ? Map.of() : indexByKey(settingRepository.findActiveByTerminal(terminalId));

        List<PosEffectiveSettingItem> items = new ArrayList<>();
        for (PosSettingDefinition def : definitionRepository.findByActiveTrueOrderBySortOrderAsc()) {
            PosSetting override = null;
            PosSettingScope from = null;
            if (terminalMap.containsKey(def.getSettingKey())) {
                override = terminalMap.get(def.getSettingKey());
                from = PosSettingScope.TERMINAL;
            } else if (storeMap.containsKey(def.getSettingKey())) {
                override = storeMap.get(def.getSettingKey());
                from = PosSettingScope.STORE;
            } else if (globals.containsKey(def.getSettingKey())) {
                override = globals.get(def.getSettingKey());
                from = PosSettingScope.GLOBAL;
            }
            String value = override != null ? override.getValueText() : def.getDefaultValue();
            if (from == null) {
                from = PosSettingScope.GLOBAL;
            }
            items.add(new PosEffectiveSettingItem(
                    def.getSettingKey(),
                    def.getValueType(),
                    def.getCategory(),
                    def.getLabel(),
                    def.isCritical(),
                    value,
                    from,
                    override != null ? override.getId() : null,
                    def.getDefaultValue()));
        }
        return new PosEffectiveSettingsResponse(resolvedStoreId, terminalId, items);
    }

    /** Valor efetivo tipado — uso interno por outros serviços PDV. */
    @Transactional(readOnly = true)
    public String getEffectiveValue(String key, UUID storeId, UUID terminalId) {
        return resolveEffective(storeId, terminalId).settings().stream()
                .filter(s -> key.equals(s.settingKey()))
                .map(PosEffectiveSettingItem::value)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Configuração desconhecida: " + key));
    }

    @Transactional(readOnly = true)
    public BigDecimal getEffectiveDecimal(String key, UUID storeId, UUID terminalId, BigDecimal fallback) {
        try {
            return new BigDecimal(getEffectiveValue(key, storeId, terminalId));
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    @Transactional(readOnly = true)
    public boolean getEffectiveBoolean(String key, UUID storeId, UUID terminalId, boolean fallback) {
        try {
            return Boolean.parseBoolean(getEffectiveValue(key, storeId, terminalId));
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    @Transactional(readOnly = true)
    public Duration getEffectiveHoursAsDuration(String key, UUID storeId, UUID terminalId, Duration fallback) {
        try {
            int hours = Integer.parseInt(getEffectiveValue(key, storeId, terminalId));
            return Duration.ofHours(hours);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    @Transactional(readOnly = true)
    public PosSettingValidateResponse validate(PosSettingValidateRequest request) {
        assertRead();
        PosSettingDefinition def = requireDefinition(request.settingKey());
        assertScopeRefs(request.scope(), request.storeId(), request.terminalId());
        PosSettingValidateResponse result = validator.validate(def, request.value());
        // histórico leve de tentativa inválida (somente se inválido) — opcional; não grava VALIDATE em sucesso
        return result;
    }

    @Transactional
    public PosSettingResponse upsert(PosSettingUpsertRequest request) {
        assertManage();
        PosSettingDefinition def = requireDefinition(request.settingKey());
        if (def.isCritical()) {
            assertAdminForCritical();
        }
        assertScopeRefs(request.scope(), request.storeId(), request.terminalId());
        String normalized = validator.requireValid(def, request.value());

        PosSetting existing = settingRepository
                .findActiveOverride(request.settingKey(), request.scope(), request.storeId(), request.terminalId())
                .orElse(null);

        if (existing == null) {
            PosSetting created = new PosSetting();
            created.setId(UUID.randomUUID());
            created.setSettingKey(def.getSettingKey());
            created.setScope(request.scope());
            created.setStore(resolveStore(request.scope(), request.storeId(), request.terminalId()));
            created.setTerminal(resolveTerminal(request.scope(), request.terminalId()));
            created.setValueText(normalized);
            created.setActive(true);
            PosSetting saved = settingRepository.saveAndFlush(created);
            appendHistory(saved, null, normalized, PosSettingHistory.ChangeType.CREATE, request.reason());
            domainAuditService.record(
                    "POS",
                    "PosSetting",
                    saved.getId(),
                    AuditLog.AuditAction.CREATE,
                    null,
                    snapshot(saved),
                    "Configuração PDV criada (" + saved.getScope() + "/" + saved.getSettingKey() + ")");
            auditSetting(PosAuditEventCode.SETTINGS_CHANGE, saved, null, snapshot(saved), "Configuração PDV criada");
            return toSettingResponse(saved);
        }

        if (request.expectedVersion() != null && !request.expectedVersion().equals(existing.getVersion())) {
            throw new ConflictException("Versão da configuração desatualizada; recarregue e tente novamente");
        }
        Map<String, Object> before = snapshot(existing);
        String oldValue = existing.getValueText();
        existing.setValueText(normalized);
        PosSetting saved = settingRepository.saveAndFlush(existing);
        appendHistory(saved, oldValue, normalized, PosSettingHistory.ChangeType.UPDATE, request.reason());
        domainAuditService.record(
                "POS",
                "PosSetting",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Configuração PDV atualizada (" + saved.getScope() + "/" + saved.getSettingKey() + ")");
        auditSetting(PosAuditEventCode.SETTINGS_CHANGE, saved, before, snapshot(saved), "Configuração PDV atualizada");
        return toSettingResponse(saved);
    }

    @Transactional
    public void delete(UUID id, String reason) {
        assertManage();
        PosSetting setting = settingRepository
                .findActiveDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração PDV", id));
        PosSettingDefinition def = requireDefinition(setting.getSettingKey());
        if (def.isCritical()) {
            assertAdminForCritical();
        }
        if (setting.getScope() == PosSettingScope.GLOBAL) {
            throw new BusinessRuleException(
                    "Não é permitido remover configuração GLOBAL; altere o valor ou sobrescreva em loja/terminal");
        }
        Map<String, Object> before = snapshot(setting);
        String oldValue = setting.getValueText();
        setting.setActive(false);
        settingRepository.saveAndFlush(setting);
        appendHistory(setting, oldValue, null, PosSettingHistory.ChangeType.DELETE, reason);
        domainAuditService.record(
                "POS",
                "PosSetting",
                setting.getId(),
                AuditLog.AuditAction.DELETE,
                before,
                null,
                "Override de configuração PDV removido (" + setting.getSettingKey() + ")");
        auditSetting(
                PosAuditEventCode.SETTINGS_CHANGE,
                setting,
                before,
                null,
                "Override de configuração PDV removido");
    }

    @Transactional(readOnly = true)
    public Page<PosSettingHistoryResponse> history(
            String settingKey, UUID storeId, UUID terminalId, Pageable pageable) {
        assertRead();
        return historyRepository
                .search(
                        StringUtils.hasText(settingKey) ? settingKey.trim() : null,
                        storeId,
                        terminalId,
                        pageable)
                .map(this::toHistoryResponse);
    }

    private Map<String, PosSetting> indexByKey(List<PosSetting> list) {
        Map<String, PosSetting> map = new HashMap<>();
        for (PosSetting s : list) {
            map.put(s.getSettingKey(), s);
        }
        return map;
    }

    private PosSettingDefinition requireDefinition(String key) {
        return definitionRepository
                .findBySettingKeyAndActiveTrue(key.trim())
                .orElseThrow(() -> new BusinessRuleException("Chave de configuração desconhecida: " + key));
    }

    private void assertScopeRefs(PosSettingScope scope, UUID storeId, UUID terminalId) {
        switch (scope) {
            case GLOBAL -> {
                if (storeId != null || terminalId != null) {
                    throw new BusinessRuleException("Escopo GLOBAL não aceita storeId/terminalId");
                }
            }
            case STORE -> {
                if (storeId == null || terminalId != null) {
                    throw new BusinessRuleException("Escopo STORE exige storeId e não aceita terminalId");
                }
                storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("Loja", storeId));
            }
            case TERMINAL -> {
                if (terminalId == null) {
                    throw new BusinessRuleException("Escopo TERMINAL exige terminalId");
                }
                PosTerminal terminal = terminalRepository
                        .findById(terminalId)
                        .orElseThrow(() -> new ResourceNotFoundException("Terminal", terminalId));
                if (storeId != null
                        && terminal.getStore() != null
                        && !terminal.getStore().getId().equals(storeId)) {
                    throw new BusinessRuleException("terminalId não pertence à loja informada");
                }
            }
        }
    }

    private Store resolveStore(PosSettingScope scope, UUID storeId, UUID terminalId) {
        if (scope == PosSettingScope.STORE) {
            return storeRepository.findById(storeId).orElseThrow();
        }
        if (scope == PosSettingScope.TERMINAL) {
            PosTerminal terminal = terminalRepository.findById(terminalId).orElseThrow();
            return terminal.getStore();
        }
        return null;
    }

    private PosTerminal resolveTerminal(PosSettingScope scope, UUID terminalId) {
        if (scope == PosSettingScope.TERMINAL) {
            return terminalRepository.findById(terminalId).orElseThrow();
        }
        return null;
    }

    private void appendHistory(
            PosSetting setting,
            String oldValue,
            String newValue,
            PosSettingHistory.ChangeType type,
            String reason) {
        PosSettingHistory history = new PosSettingHistory();
        history.setSettingId(setting.getId());
        history.setSettingKey(setting.getSettingKey());
        history.setScope(setting.getScope());
        history.setStoreId(setting.getStore() != null ? setting.getStore().getId() : null);
        history.setTerminalId(setting.getTerminal() != null ? setting.getTerminal().getId() : null);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangeType(type);
        history.setReason(StringUtils.hasText(reason) ? reason.trim() : null);
        history.setChangedBy(requireCurrentUser());
        history.setChangedAt(Instant.now());
        history.setCorrelationId(CorrelationIdContext.current());
        historyRepository.save(history);
    }

    private void assertRead() {
        if (!SecurityAuthorities.hasAuthority("POS_SETTINGS_READ")
                && !SecurityAuthorities.hasAuthority("POS_SETTINGS_MANAGE")) {
            throw new BusinessRuleException("Sem permissão para consultar configurações do PDV");
        }
    }

    private void assertManage() {
        if (!SecurityAuthorities.hasAuthority("POS_SETTINGS_MANAGE")) {
            throw new BusinessRuleException("Sem permissão para administrar configurações do PDV");
        }
    }

    private void assertAdminForCritical() {
        User user = userRepository
                .findWithRolesById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));
        boolean admin = user.getRoles() != null
                && user.getRoles().stream().map(Role::getCode).anyMatch("ADMIN"::equals);
        if (!admin) {
            throw new BusinessRuleException(
                    "Configuração crítica exige perfil administrativo (ADMIN)");
        }
        posAuditService.success(
                PosAuditEventCode.ADMIN_ACCESS,
                PosAuditContext.builder()
                        .entity("PosSetting", null)
                        .action(AuditLog.AuditAction.OTHER)
                        .details("Acesso administrativo a configuração crítica do PDV")
                        .build());
    }

    private void auditSetting(
            PosAuditEventCode event, PosSetting setting, Object before, Object after, String details) {
        PosAuditContext.Builder base = PosAuditContext.builder()
                .storeId(setting.getStore() != null ? setting.getStore().getId() : null)
                .terminalId(setting.getTerminal() != null ? setting.getTerminal().getId() : null)
                .entity("PosSetting", setting.getId())
                .action(AuditLog.AuditAction.UPDATE)
                .before(before)
                .after(after)
                .details(details);
        posAuditService.success(event, base.build());
        posAuditService.success(
                PosAuditEventCode.ADMIN_ACCESS,
                PosAuditContext.builder()
                        .storeId(setting.getStore() != null ? setting.getStore().getId() : null)
                        .terminalId(setting.getTerminal() != null ? setting.getTerminal().getId() : null)
                        .entity("PosSetting", setting.getId())
                        .action(AuditLog.AuditAction.OTHER)
                        .details("Acesso administrativo — alteração de configuração PDV")
                        .build());
    }

    private User requireCurrentUser() {
        return userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));
    }

    private PosSettingDefinitionResponse toDefinitionResponse(PosSettingDefinition def) {
        return new PosSettingDefinitionResponse(
                def.getId(),
                def.getSettingKey(),
                def.getValueType(),
                def.getCategory(),
                def.getLabel(),
                def.getDescription(),
                def.getDefaultValue(),
                def.getMinValue(),
                def.getMaxValue(),
                def.getAllowedValues(),
                def.isCritical(),
                def.getSortOrder());
    }

    private PosSettingResponse toSettingResponse(PosSetting setting) {
        PosSettingDefinition def = definitionRepository
                .findBySettingKeyAndActiveTrue(setting.getSettingKey())
                .orElse(null);
        return new PosSettingResponse(
                setting.getId(),
                setting.getSettingKey(),
                def != null ? def.getValueType() : null,
                def != null ? def.getCategory() : null,
                def != null ? def.getLabel() : setting.getSettingKey(),
                def != null && def.isCritical(),
                setting.getScope(),
                setting.getStore() != null ? setting.getStore().getId() : null,
                setting.getStore() != null ? setting.getStore().getCode() : null,
                setting.getTerminal() != null ? setting.getTerminal().getId() : null,
                setting.getTerminal() != null ? setting.getTerminal().getCode() : null,
                setting.getValueText(),
                setting.getUpdatedAt(),
                setting.getVersion());
    }

    private PosSettingHistoryResponse toHistoryResponse(PosSettingHistory h) {
        return new PosSettingHistoryResponse(
                h.getId(),
                h.getSettingId(),
                h.getSettingKey(),
                h.getScope(),
                h.getStoreId(),
                h.getTerminalId(),
                h.getOldValue(),
                h.getNewValue(),
                h.getChangeType(),
                h.getReason(),
                h.getChangedBy() != null ? h.getChangedBy().getId() : null,
                h.getChangedBy() != null ? h.getChangedBy().getName() : null,
                h.getChangedAt(),
                h.getCorrelationId());
    }

    private Map<String, Object> snapshot(PosSetting setting) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", setting.getId());
        map.put("settingKey", setting.getSettingKey());
        map.put("scope", setting.getScope());
        map.put("storeId", setting.getStore() != null ? setting.getStore().getId() : null);
        map.put("terminalId", setting.getTerminal() != null ? setting.getTerminal().getId() : null);
        map.put("value", setting.getValueText());
        map.put("active", setting.getActive());
        return map;
    }
}
