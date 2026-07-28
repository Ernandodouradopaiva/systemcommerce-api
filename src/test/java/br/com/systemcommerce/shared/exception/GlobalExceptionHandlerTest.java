package br.com.systemcommerce.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/example");
        MDC.put(CorrelationIdConstants.MDC_KEY, "corr-unit-test");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldMapResourceNotFound() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleBusiness(new ResourceNotFoundException("Produto", "1"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("corr-unit-test");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/example");
    }

    @Test
    void shouldMapConflictAndBusinessRule() {
        assertThat(handler.handleBusiness(new ConflictException("duplicado"), request).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler
                        .handleBusiness(new BusinessRuleException("estoque"), request)
                        .getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void shouldMapConstraintViolationWithFieldDetails() {
        // cobertura de validação detalhada também em ApiErrorHandlingTest (MockMvc)
        ResponseEntity<ApiErrorResponse> response = handler.handleBusiness(
                new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Erro de validação",
                        List.of(ApiErrorResponse.FieldErrorDetail.builder()
                                .field("email")
                                .message("e-mail inválido")
                                .build())),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getDetails()).hasSize(1);
        assertThat(response.getBody().getDetails().getFirst().getField()).isEqualTo("email");
    }

    @Test
    void shouldMapDataIntegrityWithoutExposingSql() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("insert into secret_table failed"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo("DATA_INTEGRITY_VIOLATION");
        assertThat(response.getBody().getMessage()).doesNotContain("secret_table");
        assertThat(response.getBody().getMessage()).doesNotContain("insert into");
    }

    @Test
    void shouldMapMethodNotAllowed() {
        ResponseEntity<ApiErrorResponse> response = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("DELETE", List.of("GET")), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().getCode()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void shouldHideInternalDetailsOnUnexpectedError() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleGeneric(new IllegalStateException("stack com senha=123"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Erro interno do servidor");
        assertThat(response.getBody().getMessage()).doesNotContain("senha");
    }

    @Test
    void shouldMapTokenExceptions() {
        assertThat(handler
                        .handleBusiness(new TokenExpiredException("expirado"), request)
                        .getBody()
                        .getCode())
                .isEqualTo("TOKEN_EXPIRED");
        assertThat(handler
                        .handleBusiness(new InvalidTokenException("invalido"), request)
                        .getBody()
                        .getCode())
                .isEqualTo("INVALID_TOKEN");
    }
}
