package br.com.systemcommerce.shared.response;

import br.com.systemcommerce.shared.exception.AccessDeniedBusinessException;
import br.com.systemcommerce.shared.exception.ApiErrorResponse;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.InvalidTokenException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.exception.TokenExpiredException;
import br.com.systemcommerce.shared.exception.UnauthorizedException;
import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints apenas para validar o padrão de erros no profile test.
 */
@Profile("test")
@RestController
@RequestMapping("/api/v1/_test/errors")
@Tag(name = "Error Probe (test)", description = "Probes de erro — disponíveis somente no profile test")
public class ErrorProbeController {

    @GetMapping("/not-found")
    @Operation(summary = "Simula recurso não encontrado")
    @ApiResponse(
            responseCode = "404",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)),
            headers = @Header(name = CorrelationIdConstants.HEADER, description = "Correlation ID"))
    public void notFound() {
        throw new ResourceNotFoundException("Produto", "123");
    }

    @GetMapping("/conflict")
    public void conflict() {
        throw new ConflictException("SKU já está em uso");
    }

    @GetMapping("/business-rule")
    public void businessRule() {
        throw new BusinessRuleException("Estoque insuficiente para a operação");
    }

    @GetMapping("/access-denied")
    public void accessDenied() {
        throw new AccessDeniedBusinessException("Sem permissão para a operação");
    }

    @GetMapping("/unauthorized")
    public void unauthorized() {
        throw new UnauthorizedException("Credenciais inválidas");
    }

    @GetMapping("/invalid-token")
    public void invalidToken() {
        throw new InvalidTokenException("Token inválido");
    }

    @GetMapping("/token-expired")
    public void tokenExpired() {
        throw new TokenExpiredException("Token expirado");
    }

    @GetMapping("/data-integrity")
    public void dataIntegrity() {
        throw new DataIntegrityViolationException("simulated integrity violation");
    }

    @GetMapping("/internal")
    public void internal() {
        throw new IllegalStateException("falha simulada com detalhes internos sensíveis");
    }

    @PostMapping(value = "/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void validation(@Valid @RequestBody ProbeRequest request) {
        // validação via Bean Validation
    }

    public record ProbeRequest(
            @NotBlank(message = "nome é obrigatório") String name,
            @Email(message = "e-mail inválido") String email) {}
}
