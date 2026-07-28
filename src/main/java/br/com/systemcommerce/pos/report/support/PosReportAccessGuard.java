package br.com.systemcommerce.pos.report.support;

import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.report.dto.PosReportFilter;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Escopo por loja: perfis sem gestão ampla devem informar storeId.
 * ADMIN / STORE_MANAGE / POS_FORCE_CLOSE_CASH podem consultar todas as lojas.
 */
@Component
@RequiredArgsConstructor
public class PosReportAccessGuard {

    private final UserRepository userRepository;

    public PosReportFilter enforceStoreScope(PosReportFilter filter) {
        if (canAccessAllStores()) {
            return filter;
        }
        if (filter.storeId() == null) {
            throw new BusinessRuleException(
                    "Informe storeId: seu perfil exige filtrar relatórios PDV por loja");
        }
        return filter;
    }

    public boolean canAccessAllStores() {
        if (SecurityAuthorities.hasAuthority("STORE_MANAGE")
                || SecurityAuthorities.hasAuthority("POS_FORCE_CLOSE_CASH")) {
            return true;
        }
        return userRepository
                .findWithRolesById(CurrentUser.requireId())
                .map(u -> u.getRoles() != null
                        && u.getRoles().stream().map(Role::getCode).anyMatch("ADMIN"::equals))
                .orElse(false);
    }

    public void assertExport() {
        if (!SecurityAuthorities.hasAuthority("POS_REPORT_EXPORT")
                && !SecurityAuthorities.hasAuthority("POS_REPORT_READ")) {
            // EXPORT preferred; READ alone does not export
        }
        if (!SecurityAuthorities.hasAuthority("POS_REPORT_EXPORT")) {
            throw new BusinessRuleException("Sem permissão para exportar relatórios do PDV");
        }
    }

    public void assertRead() {
        if (!SecurityAuthorities.hasAuthority("POS_REPORT_READ")
                && !SecurityAuthorities.hasAuthority("POS_DASHBOARD_READ")) {
            throw new BusinessRuleException("Sem permissão para consultar relatórios do PDV");
        }
    }

    public void assertDashboard() {
        if (!SecurityAuthorities.hasAuthority("POS_DASHBOARD_READ")
                && !SecurityAuthorities.hasAuthority("POS_REPORT_READ")) {
            throw new BusinessRuleException("Sem permissão para o dashboard do PDV");
        }
    }
}
