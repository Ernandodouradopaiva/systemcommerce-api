package br.com.systemcommerce.report.support;

import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.shared.exception.BusinessException;
import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.storecontext.CurrentStoreContext;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolve {@link ReportStoreFilter} para relatórios e dashboard ERP.
 * Usuários sem escopo GLOBAL consultam apenas lojas acessíveis via {@link StoreAuthorizationEvaluator}.
 */
@Component
@RequiredArgsConstructor
public class ReportStoreAccessSupport {

    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    public ReportStoreFilter resolveReportFilter(UUID storeId, ReportScope scope) {
        return resolve(storeId, scope, true);
    }

    public ReportStoreFilter resolveDashboardFilter(UUID storeId, ReportScope scope) {
        return resolve(storeId, scope, false);
    }

    private ReportStoreFilter resolve(UUID storeId, ReportScope scope, boolean report) {
        UUID userId = CurrentUser.requireId();
        ReportScope effective = scope != null ? scope : inferDefaultScope(storeId, report);

        return switch (effective) {
            case GLOBAL -> {
                assertGlobalPermission(report);
                yield ReportStoreFilter.unrestricted();
            }
            case MULTI -> {
                assertMultiPermission(report);
                if (storeId != null) {
                    storeAuthorizationEvaluator.assertCanAccess(userId, storeId);
                    yield ReportStoreFilter.single(storeId);
                }
                yield accessibleStoresFilter(userId, report);
            }
            case STORE -> {
                UUID effectiveStoreId = storeId != null ? storeId : CurrentStoreContext.get().storeId();
                if (effectiveStoreId == null) {
                    throw new BusinessException(
                            ErrorCode.STORE_CONTEXT_REQUIRED,
                            "Informe storeId ou selecione uma loja para escopo STORE");
                }
                storeAuthorizationEvaluator.assertCanAccess(userId, effectiveStoreId);
                yield ReportStoreFilter.single(effectiveStoreId);
            }
        };
    }

    private ReportScope inferDefaultScope(UUID storeId, boolean report) {
        if (storeId != null || CurrentStoreContext.get().hasStore()) {
            return ReportScope.STORE;
        }
        if (hasGlobalPermission(report) || storeAuthorizationEvaluator.hasGlobalAccess()) {
            return ReportScope.GLOBAL;
        }
        if (hasMultiPermission(report)) {
            return ReportScope.MULTI;
        }
        return ReportScope.STORE;
    }

    private ReportStoreFilter accessibleStoresFilter(UUID userId, boolean report) {
        if (hasGlobalPermission(report) || storeAuthorizationEvaluator.hasGlobalAccess()) {
            return ReportStoreFilter.unrestricted();
        }
        List<UUID> ids = storeAuthorizationEvaluator.listEffectiveAccess(userId).stream()
                .map(UserStoreAccess::getStore)
                .map(s -> s.getId())
                .toList();
        return ReportStoreFilter.multi(ids);
    }

    private void assertGlobalPermission(boolean report) {
        if (storeAuthorizationEvaluator.hasGlobalAccess()) {
            return;
        }
        if (report) {
            if (!SecurityAuthorities.hasAnyAuthority("REPORT_GLOBAL_READ", "GLOBAL_STORE_ACCESS")) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "Sem permissão para relatórios globais");
            }
        } else if (!SecurityAuthorities.hasAnyAuthority("DASHBOARD_GLOBAL_READ", "GLOBAL_STORE_ACCESS")) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Sem permissão para dashboard global");
        }
    }

    private void assertMultiPermission(boolean report) {
        if (hasMultiPermission(report) || hasGlobalPermission(report) || storeAuthorizationEvaluator.hasGlobalAccess()) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED, "Sem permissão para consulta multi-loja");
    }

    private boolean hasGlobalPermission(boolean report) {
        return report
                ? SecurityAuthorities.hasAnyAuthority("REPORT_GLOBAL_READ", "GLOBAL_STORE_ACCESS")
                : SecurityAuthorities.hasAnyAuthority("DASHBOARD_GLOBAL_READ", "GLOBAL_STORE_ACCESS");
    }

    private boolean hasMultiPermission(boolean report) {
        return report
                ? SecurityAuthorities.hasAnyAuthority(
                        "REPORT_MULTI_STORE_READ", "REPORT_GLOBAL_READ", "STORE_CONSOLIDATED_READ")
                : SecurityAuthorities.hasAnyAuthority("DASHBOARD_GLOBAL_READ", "STORE_CONSOLIDATED_READ");
    }
}
