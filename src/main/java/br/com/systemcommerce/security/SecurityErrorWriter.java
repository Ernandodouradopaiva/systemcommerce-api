package br.com.systemcommerce.security;

import br.com.systemcommerce.shared.exception.ApiErrorResponse;
import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.shared.web.CorrelationIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    public static final String JWT_ERROR_ATTR = "JWT_ERROR";
    public static final String JWT_EXPIRED = "EXPIRED";
    public static final String JWT_INVALID = "INVALID";

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        write(request, response, errorCode, errorCode.getDefaultMessage());
    }

    public void write(
            HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode, String message)
            throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(errorCode.getHttpStatus().value())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .code(errorCode.name())
                .message(message)
                .path(request.getRequestURI())
                .correlationId(CorrelationIdContext.current())
                .build();

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    public ErrorCode resolveUnauthorizedCode(HttpServletRequest request) {
        Object jwtError = request.getAttribute(JWT_ERROR_ATTR);
        if (JWT_EXPIRED.equals(jwtError)) {
            return ErrorCode.TOKEN_EXPIRED;
        }
        if (JWT_INVALID.equals(jwtError)) {
            return ErrorCode.INVALID_TOKEN;
        }
        return ErrorCode.UNAUTHENTICATED;
    }
}
