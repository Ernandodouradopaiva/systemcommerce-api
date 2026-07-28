package br.com.systemcommerce.fiscal.nfe;

import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/nfe")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "NF-e Emission", description = "Emissão NF-e (Prompt 134)")
public class NfeEmissionController {

    private final NfeEmissionService nfeEmissionService;

    @PostMapping("/emit-from-sale/{saleId}")
    @PreAuthorize("hasAuthority('FISCAL_NFE_EMIT')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> emitFromSale(
            @PathVariable UUID saleId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(nfeEmissionService.emitFromSale(saleId, idempotencyKey)));
    }
}
