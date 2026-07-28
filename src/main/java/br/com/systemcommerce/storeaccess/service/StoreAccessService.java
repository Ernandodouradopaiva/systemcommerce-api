package br.com.systemcommerce.storeaccess.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessException;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.dto.AccessibleStoreResponse;
import br.com.systemcommerce.storeaccess.dto.StoreContextSwitchRequest;
import br.com.systemcommerce.storeaccess.dto.UserStoreAccessGrantRequest;
import br.com.systemcommerce.storeaccess.dto.UserStoreAccessResponse;
import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import br.com.systemcommerce.storeaccess.repository.UserStoreAccessRepository;
import br.com.systemcommerce.storecontext.CurrentStoreContext;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreAccessService {

    private final UserStoreAccessRepository accessRepository;
    private final StoreAuthorizationEvaluator authorizationEvaluator;
    private final StoreService storeService;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<AccessibleStoreResponse> listAccessibleStores(UUID userId) {
        UUID targetUser = userId != null ? userId : CurrentUser.requireId();
        if (authorizationEvaluator.hasGlobalAccess() && (userId == null || userId.equals(CurrentUser.requireId()))) {
            return storeService
                    .list(
                            null,
                            null,
                            Store.StoreStatus.ACTIVE,
                            null,
                            null,
                            null,
                            null,
                            null,
                            org.springframework.data.domain.Pageable.unpaged())
                    .stream()
                    .map(s -> new AccessibleStoreResponse(
                            s.id(),
                            s.code(),
                            s.name(),
                            false,
                            s.organizationId(),
                            true,
                            Boolean.TRUE.equals(s.allowsSales()),
                            Boolean.TRUE.equals(s.allowsPos())))
                    .toList();
        }
        return authorizationEvaluator.listEffectiveAccess(targetUser).stream()
                .map(a -> new AccessibleStoreResponse(
                        a.getStore().getId(),
                        a.getStore().getCode(),
                        a.getStore().getName(),
                        a.isDefaultStore(),
                        a.getStore().getOrganization().getId(),
                        a.getStore().isUsable(),
                        a.getStore().isAllowsSales(),
                        a.getStore().isAllowsPos()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserStoreAccessResponse> listHistory(UUID userId) {
        return accessRepository.findHistoryByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserStoreAccessResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public UserStoreAccessResponse grant(UserStoreAccessGrantRequest request) {
        User user = userRepository
                .findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", request.userId()));
        Store store = storeService.getEntity(request.storeId());
        UserStoreAccess.AccessType type =
                request.accessType() != null ? request.accessType() : UserStoreAccess.AccessType.PERMANENT;
        validatePeriod(type, request.startDate(), request.endDate());
        boolean makeDefault = Boolean.TRUE.equals(request.defaultStore());
        if (makeDefault) {
            clearDefault(user.getId());
        }
        UserStoreAccess access = new UserStoreAccess();
        access.setUser(user);
        access.setStore(store);
        access.setStartDate(request.startDate());
        access.setEndDate(request.endDate());
        access.setDefaultStore(makeDefault);
        access.setAccessType(type);
        access.setStatus(UserStoreAccess.AccessStatus.ACTIVE);
        access.setGrantedBy(userRepository.findById(CurrentUser.requireId()).orElse(null));
        access.setReason(MoneyAndQuantityUtils.blankToNull(request.reason()));
        access.setActive(true);
        UserStoreAccess saved = accessRepository.save(access);
        ensureSingleStoreHasDefault(user.getId());
        domainAuditService.record(
                "STORE_ACCESS",
                "UserStoreAccess",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Acesso à loja concedido");
        return toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public UserStoreAccessResponse grantTemporary(UserStoreAccessGrantRequest request) {
        UserStoreAccess.AccessType type = request.accessType() != null
                ? request.accessType()
                : UserStoreAccess.AccessType.TEMPORARY;
        if (type != UserStoreAccess.AccessType.TEMPORARY && type != UserStoreAccess.AccessType.SUPPORT) {
            type = UserStoreAccess.AccessType.TEMPORARY;
        }
        return grant(new UserStoreAccessGrantRequest(
                request.userId(),
                request.storeId(),
                request.startDate(),
                request.endDate(),
                request.defaultStore(),
                type,
                request.reason()));
    }

    @Transactional
    public UserStoreAccessResponse revoke(UUID id) {
        UserStoreAccess access = getEntity(id);
        Map<String, Object> before = snapshot(access);
        access.revoke();
        UserStoreAccess saved = accessRepository.save(access);
        domainAuditService.record(
                "STORE_ACCESS",
                "UserStoreAccess",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Acesso à loja revogado");
        return toResponse(getEntity(id));
    }

    @Transactional
    public UserStoreAccessResponse setDefaultStore(UUID userId, UUID storeId) {
        authorizationEvaluator.assertCanAccess(userId, storeId);
        clearDefault(userId);
        List<UserStoreAccess> effective = accessRepository.findEffectiveByUserId(userId, LocalDate.now());
        UserStoreAccess target = effective.stream()
                .filter(a -> a.getStore().getId().equals(storeId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.STORE_ACCESS_DENIED, "Usuário sem acesso efetivo à loja para torná-la padrão"));
        Map<String, Object> before = snapshot(target);
        target.setDefaultStore(true);
        accessRepository.save(target);
        domainAuditService.record(
                "STORE_ACCESS",
                "UserStoreAccess",
                target.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(target),
                "Loja padrão definida");
        return toResponse(getEntity(target.getId()));
    }

    @Transactional(readOnly = true)
    public CurrentStoreContext switchContext(StoreContextSwitchRequest request) {
        UUID userId = CurrentUser.requireId();
        Store store = authorizationEvaluator.assertCanAccess(userId, request.storeId());
        CurrentStoreContext ctx = CurrentStoreContext.of(
                        store.getId(), store.getOrganization().getId(), CurrentStoreContext.Source.EXPLICIT)
                .validated();
        CurrentStoreContext.set(ctx);
        domainAuditService.record(
                "STORE_ACCESS",
                "StoreContext",
                store.getId(),
                AuditLog.AuditAction.OTHER,
                null,
                Map.of("storeId", store.getId(), "userId", userId),
                "Contexto de loja trocado");
        return ctx;
    }

    public UserStoreAccess getEntity(UUID id) {
        return accessRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Acesso à loja", id));
    }

    private void validatePeriod(UserStoreAccess.AccessType type, LocalDate start, LocalDate end) {
        if (start == null) {
            throw new BusinessRuleException("Data de início do acesso é obrigatória");
        }
        if (end != null && end.isBefore(start)) {
            throw new BusinessRuleException("Período de acesso inconsistente");
        }
        if ((type == UserStoreAccess.AccessType.TEMPORARY || type == UserStoreAccess.AccessType.SUPPORT)
                && end == null) {
            throw new BusinessRuleException("Acesso temporário/apoio deve possuir data de término");
        }
    }

    private void clearDefault(UUID userId) {
        accessRepository.findHistoryByUserId(userId).stream()
                .filter(UserStoreAccess::isDefaultStore)
                .forEach(a -> {
                    a.setDefaultStore(false);
                    accessRepository.save(a);
                });
    }

    private void ensureSingleStoreHasDefault(UUID userId) {
        List<UserStoreAccess> effective = accessRepository.findEffectiveByUserId(userId, LocalDate.now());
        if (effective.size() == 1 && !effective.getFirst().isDefaultStore()) {
            effective.getFirst().setDefaultStore(true);
            accessRepository.save(effective.getFirst());
        }
    }

    private UserStoreAccessResponse toResponse(UserStoreAccess access) {
        return new UserStoreAccessResponse(
                access.getId(),
                access.getUser().getId(),
                access.getUser().getLogin(),
                access.getStore().getId(),
                access.getStore().getCode(),
                access.getStore().getName(),
                access.getStartDate(),
                access.getEndDate(),
                access.isDefaultStore(),
                access.getAccessType(),
                access.getStatus(),
                access.getGrantedBy() != null ? access.getGrantedBy().getId() : null,
                access.getReason(),
                access.getCreatedAt(),
                access.getUpdatedAt());
    }

    private Map<String, Object> snapshot(UserStoreAccess access) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", access.getId());
        map.put("userId", access.getUser() != null ? access.getUser().getId() : null);
        map.put("storeId", access.getStore() != null ? access.getStore().getId() : null);
        map.put("defaultStore", access.isDefaultStore());
        map.put("accessType", access.getAccessType());
        map.put("status", access.getStatus());
        map.put("startDate", access.getStartDate());
        map.put("endDate", access.getEndDate());
        return map;
    }
}
