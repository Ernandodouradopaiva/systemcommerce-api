package br.com.systemcommerce.security;

import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.shared.web.AuditRequestContextFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rate limit em memória para endpoints públicos de autenticação (por IP + rota). */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final RateLimitProperties.MinuteWindowCounter counter = new RateLimitProperties.MinuteWindowCounter();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return resolveLimit(path) <= 0;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = resolveLimit(path);
        String ip = AuditRequestContextFilter.resolveClientIp(request);
        String key = ip + "|" + path;

        if (!counter.tryConsume(key, limit)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("status", 429);
            body.put("error", "Too Many Requests");
            body.put("code", ErrorCode.RATE_LIMITED.name());
            body.put("message", "Muitas tentativas. Aguarde e tente novamente.");
            body.put("path", path);
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int resolveLimit(String path) {
        if (path.endsWith("/api/v1/auth/login")) {
            return properties.getLoginPerMinute();
        }
        if (path.endsWith("/api/v1/auth/refresh")) {
            return properties.getRefreshPerMinute();
        }
        if (path.endsWith("/api/v1/auth/password/forgot")) {
            return properties.getPasswordForgotPerMinute();
        }
        if (path.endsWith("/api/v1/auth/password/reset")) {
            return properties.getPasswordResetPerMinute();
        }
        return 0;
    }
}
