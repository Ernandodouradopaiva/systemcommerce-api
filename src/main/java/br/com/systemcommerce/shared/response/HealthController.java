package br.com.systemcommerce.shared.response;

import br.com.systemcommerce.shared.exception.ApiErrorResponse;
import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Health", description = "Health check da aplicação")
public class HealthController {

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Verifica se a API está disponível")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "API disponível",
            headers =
                    @io.swagger.v3.oas.annotations.headers.Header(
                            name = CorrelationIdConstants.HEADER,
                            description = "Identificador de correlação"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Erro interno",
            content =
                    @io.swagger.v3.oas.annotations.media.Content(
                            schema =
                                    @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation = ApiErrorResponse.class)))
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.of(Map.of("status", "UP", "service", "systemcommerce-api")));
    }
}
