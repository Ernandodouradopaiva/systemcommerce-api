package br.com.systemcommerce.config;

import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI systemCommerceOpenApi() {
        Components components = new Components()
                .addSecuritySchemes(
                        BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                .addHeaders(
                        CorrelationIdConstants.HEADER,
                        new Header()
                                .description("Identificador de correlação da requisição")
                                .schema(new StringSchema()))
                .addResponses("BadRequest", errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Erro de validação"))
                .addResponses(
                        "Unauthorized",
                        errorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Não autenticado"))
                .addResponses(
                        "TokenExpired", errorResponse(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Token expirado"))
                .addResponses("Forbidden", errorResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Acesso negado"))
                .addResponses(
                        "NotFound",
                        errorResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Recurso não encontrado"))
                .addResponses("Conflict", errorResponse(HttpStatus.CONFLICT, "CONFLICT", "Conflito de dados"))
                .addResponses(
                        "UnprocessableEntity",
                        errorResponse(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "BUSINESS_RULE_VIOLATION",
                                "Violação de regra de negócio"))
                .addResponses(
                        "MethodNotAllowed",
                        errorResponse(
                                HttpStatus.METHOD_NOT_ALLOWED,
                                "METHOD_NOT_ALLOWED",
                                "Método HTTP não permitido"))
                .addResponses(
                        "InternalError",
                        errorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erro interno do servidor"));

        return new OpenAPI()
                .info(new Info()
                        .title("SystemCommerce API")
                        .description(
                                """
                                API REST do ERP de gestão comercial SystemCommerce.

                                ## Erros
                                Todos os erros seguem o envelope `ApiErrorResponse` com:
                                `timestamp`, `status`, `error`, `code`, `message`, `path`, `correlationId` e `details` (quando aplicável).

                                ## Correlação
                                Envie ou receba o header `X-Correlation-Id` em todas as requisições.
                                """)
                        .version("v1")
                        .contact(new Contact().name("SystemCommerce").email("contato@systemcommerce.local")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(components);
    }

    private io.swagger.v3.oas.models.responses.ApiResponse errorResponse(
            HttpStatus status, String code, String message) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("timestamp", "2026-07-17T23:00:00Z");
        example.put("status", status.value());
        example.put("error", status.getReasonPhrase());
        example.put("code", code);
        example.put("message", message);
        example.put("path", "/api/v1/example");
        example.put("correlationId", "a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        Schema<?> errorSchema = new Schema<>()
                .type("object")
                .addProperty("timestamp", new StringSchema().example("2026-07-17T23:00:00Z"))
                .addProperty("status", new Schema<>().type("integer").example(status.value()))
                .addProperty("error", new StringSchema().example(status.getReasonPhrase()))
                .addProperty("code", new StringSchema().example(code))
                .addProperty("message", new StringSchema().example(message))
                .addProperty("path", new StringSchema().example("/api/v1/example"))
                .addProperty("correlationId", new StringSchema().example("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
                .addProperty(
                        "details",
                        new Schema<>()
                                .type("array")
                                .items(new Schema<>()
                                        .type("object")
                                        .addProperty("field", new StringSchema())
                                        .addProperty("message", new StringSchema())
                                        .addProperty("rejectedValue", new StringSchema())));

        return new io.swagger.v3.oas.models.responses.ApiResponse()
                .description(message)
                .addHeaderObject(
                        CorrelationIdConstants.HEADER,
                        new Header().description("Correlation ID").schema(new StringSchema()))
                .content(new Content()
                        .addMediaType(
                                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                new MediaType()
                                        .schema(errorSchema)
                                        .addExamples("default", new Example().value(example))));
    }
}
