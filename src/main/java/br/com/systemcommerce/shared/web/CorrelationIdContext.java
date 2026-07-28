package br.com.systemcommerce.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class CorrelationIdContext {

    private CorrelationIdContext() {}

    public static String current() {
        String fromMdc = MDC.get(CorrelationIdConstants.MDC_KEY);
        if (StringUtils.hasText(fromMdc)) {
            return fromMdc;
        }

        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(CorrelationIdContext::fromRequest)
                .filter(StringUtils::hasText)
                .orElseGet(CorrelationIdContext::newId);
    }

    public static String fromRequest(HttpServletRequest request) {
        Object attribute = request.getAttribute(CorrelationIdConstants.REQUEST_ATTRIBUTE);
        if (attribute instanceof String value && StringUtils.hasText(value)) {
            return value;
        }
        String header = request.getHeader(CorrelationIdConstants.HEADER);
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        return null;
    }

    public static String resolveOrCreate(HttpServletRequest request) {
        String existing = fromRequest(request);
        if (StringUtils.hasText(existing)) {
            return existing;
        }
        String header = request.getHeader(CorrelationIdConstants.HEADER);
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        return newId();
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static void clear() {
        MDC.remove(CorrelationIdConstants.MDC_KEY);
    }

    public static void set(String correlationId) {
        MDC.put(CorrelationIdConstants.MDC_KEY, correlationId);
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(
                    CorrelationIdConstants.REQUEST_ATTRIBUTE,
                    correlationId,
                    RequestAttributes.SCOPE_REQUEST);
        }
    }
}
