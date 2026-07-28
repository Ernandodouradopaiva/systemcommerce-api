package br.com.systemcommerce.pos.resilience.controller;

import br.com.systemcommerce.pos.resilience.dto.PosOperationLookupResponse;
import br.com.systemcommerce.pos.resilience.service.PosOperationLookupService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de operações PDV por Idempotency-Key — recuperação após instabilidade de rede.
 * Não implementa venda offline definitiva.
 */
@RestController
@RequestMapping("/api/v1/pos/operations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "POS Operations (resilience)",
        description =
                """
                Consulta o resultado oficial de operações pela Idempotency-Key \
                (item, pagamento, finalização). Use após resposta perdida ou reenvio seguro.
                """)
public class PosOperationLookupController {

    private final PosOperationLookupService posOperationLookupService;

    @GetMapping("/by-idempotency-key")
    @PreAuthorize(
            "hasAuthority('POS_SALE_CREATE') or hasAuthority('POS_PAYMENT_MANAGE') "
                    + "or hasAuthority('POS_SALE_FINALIZE') or hasAuthority('SALE_READ')")
    @Operation(summary = "Consulta operação PDV pela Idempotency-Key (query)")
    public ResponseEntity<ApiResponse<PosOperationLookupResponse>> lookupByQuery(
            @RequestParam("key") String key) {
        return ResponseEntity.ok(ApiResponse.of(posOperationLookupService.lookup(key)));
    }

    @GetMapping("/by-idempotency-key/{key}")
    @PreAuthorize(
            "hasAuthority('POS_SALE_CREATE') or hasAuthority('POS_PAYMENT_MANAGE') "
                    + "or hasAuthority('POS_SALE_FINALIZE') or hasAuthority('SALE_READ')")
    @Operation(summary = "Consulta operação PDV pela Idempotency-Key (path)")
    public ResponseEntity<ApiResponse<PosOperationLookupResponse>> lookupByPath(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.of(posOperationLookupService.lookup(key)));
    }
}
