package br.com.systemcommerce.storecontext;

import br.com.systemcommerce.shared.exception.BusinessException;
import br.com.systemcommerce.shared.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RequireStoreContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        Method method = handlerMethod.getMethod();
        boolean global = handlerMethod.getBeanType().isAnnotationPresent(GlobalStoreOperation.class)
                || method.isAnnotationPresent(GlobalStoreOperation.class);
        if (global) {
            return true;
        }
        boolean required = handlerMethod.getBeanType().isAnnotationPresent(RequireStoreContext.class)
                || method.isAnnotationPresent(RequireStoreContext.class);
        if (!required) {
            return true;
        }
        CurrentStoreContext ctx = CurrentStoreContext.get();
        if (!ctx.hasStore()) {
            throw new BusinessException(
                    ErrorCode.STORE_CONTEXT_REQUIRED,
                    "Operação exige loja ativa (header X-Store-Id ou loja padrão)");
        }
        return true;
    }
}
