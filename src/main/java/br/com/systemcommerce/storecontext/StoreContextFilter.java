package br.com.systemcommerce.storecontext;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.security.SecurityErrorWriter;
import br.com.systemcommerce.shared.exception.BusinessException;
import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolve o contexto de loja ativa:
 * 1) Header X-Store-Id (se presente) — sempre revalidado
 * 2) Loja padrão / única do usuário
 * 3) Nenhum (operações globais)
 *
 * PDV deve preferir loja do terminal/sessão nos services (Source.TERMINAL/RESOURCE).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
@RequiredArgsConstructor
public class StoreContextFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Store-Id";
    public static final String MDC_STORE_ID = "storeId";

    private static final Logger log = LoggerFactory.getLogger(StoreContextFilter.class);

    private final StoreAuthorizationEvaluator authorizationEvaluator;
    private final SecurityErrorWriter securityErrorWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CurrentStoreContext.clear();
        MDC.remove(MDC_STORE_ID);
        try {
            if (isAuthenticated()) {
                resolveContext(request);
            }
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            securityErrorWriter.write(request, response, ex.getErrorCode(), ex.getMessage());
        } finally {
            CurrentStoreContext.clear();
            MDC.remove(MDC_STORE_ID);
        }
    }

    private void resolveContext(HttpServletRequest request) {
        UUID userId = CurrentUser.id().orElse(null);
        if (userId == null) {
            return;
        }
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header)) {
            UUID storeId = parseUuid(header.trim());
            if (storeId == null) {
                throw new BusinessException(ErrorCode.STORE_CONTEXT_INVALID, "X-Store-Id inválido");
            }
            Store store = authorizationEvaluator.assertCanAccess(userId, storeId);
            CurrentStoreContext ctx = CurrentStoreContext.of(
                            store.getId(), store.getOrganization().getId(), CurrentStoreContext.Source.HEADER)
                    .validated();
            CurrentStoreContext.set(ctx);
            MDC.put(MDC_STORE_ID, store.getId().toString());
            log.debug("Store context from header storeId={} userId={}", storeId, userId);
            return;
        }
        CurrentStoreContext def = authorizationEvaluator.resolveDefaultContext(userId);
        if (def.hasStore()) {
            CurrentStoreContext.set(def.validated());
            MDC.put(MDC_STORE_ID, def.storeId().toString());
        } else {
            CurrentStoreContext.set(CurrentStoreContext.empty());
        }
    }

    private static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
