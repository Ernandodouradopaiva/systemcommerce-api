package br.com.systemcommerce.storeaccess.service;

import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.exception.BusinessException;
import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import br.com.systemcommerce.storeaccess.repository.UserStoreAccessRepository;
import br.com.systemcommerce.storecontext.CurrentStoreContext;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avaliador central de autorização por loja. Preferir este serviço a validações manuais espalhadas.
 */
@Service
@RequiredArgsConstructor
public class StoreAuthorizationEvaluator {

    private static final Logger log = LoggerFactory.getLogger(StoreAuthorizationEvaluator.class);

    private final UserStoreAccessRepository userStoreAccessRepository;
    private final StoreService storeService;

    public boolean hasGlobalAccess() {
        return SecurityAuthorities.hasAuthority("GLOBAL_STORE_ACCESS");
    }

    public boolean canAccessStore(UUID userId, UUID storeId) {
        if (userId == null || storeId == null) {
            return false;
        }
        // GLOBAL_STORE_ACCESS aplica-se apenas ao usuário autenticado em avaliação (não a terceiros)
        if (CurrentUser.id().filter(userId::equals).isPresent() && hasGlobalAccess()) {
            return true;
        }
        return userStoreAccessRepository.hasEffectiveAccess(userId, storeId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Store assertCanAccess(UUID userId, UUID storeId) {
        Store store = storeService.getEntity(storeId);
        if (!store.isUsable()) {
            throw new BusinessException(ErrorCode.STORE_INACTIVE, "Loja inativa");
        }
        if (!canAccessStore(userId, storeId)) {
            log.warn(
                    "STORE_ACCESS_DENIED userId={} storeId={} correlationAttempt=cross-store",
                    userId,
                    storeId);
            throw new BusinessException(ErrorCode.STORE_ACCESS_DENIED, "Usuário sem acesso à loja informada");
        }
        return store;
    }

    @Transactional(readOnly = true)
    public void assertResourceBelongsToStore(UUID resourceStoreId, UUID expectedStoreId) {
        if (resourceStoreId == null || expectedStoreId == null) {
            return;
        }
        if (!resourceStoreId.equals(expectedStoreId)) {
            log.warn(
                    "RESOURCE_BELONGS_TO_ANOTHER_STORE resourceStoreId={} expectedStoreId={}",
                    resourceStoreId,
                    expectedStoreId);
            throw new BusinessException(
                    ErrorCode.RESOURCE_BELONGS_TO_ANOTHER_STORE,
                    "Recurso pertence a outra loja");
        }
    }

    @Transactional(readOnly = true)
    public List<UserStoreAccess> listEffectiveAccess(UUID userId) {
        if (hasGlobalAccess()) {
            return List.of();
        }
        return userStoreAccessRepository.findEffectiveByUserId(userId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public CurrentStoreContext resolveDefaultContext(UUID userId) {
        if (hasGlobalAccess()) {
            return CurrentStoreContext.empty();
        }
        return userStoreAccessRepository
                .findFirstByUserIdAndDefaultStoreTrueAndStatus(userId, UserStoreAccess.AccessStatus.ACTIVE)
                .filter(a -> a.isEffectiveOn(LocalDate.now()))
                .map(a -> CurrentStoreContext.of(
                        a.getStore().getId(),
                        a.getStore().getOrganization().getId(),
                        CurrentStoreContext.Source.DEFAULT))
                .orElseGet(() -> {
                    List<UserStoreAccess> effective =
                            userStoreAccessRepository.findEffectiveByUserId(userId, LocalDate.now());
                    if (effective.size() == 1) {
                        UserStoreAccess only = effective.getFirst();
                        return CurrentStoreContext.of(
                                only.getStore().getId(),
                                only.getStore().getOrganization().getId(),
                                CurrentStoreContext.Source.DEFAULT);
                    }
                    return CurrentStoreContext.empty();
                });
    }
}
