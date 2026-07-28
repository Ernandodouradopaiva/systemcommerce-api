package br.com.systemcommerce.fiscal.nfce;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/nfce")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "NFC-e Emission", description = "Emissão NFC-e PDV (Prompt 135)")
public class NfceEmissionController {

    private final NfceEmissionService nfceEmissionService;

    @PostMapping("/emit-from-sale/{saleId}")
    @PreAuthorize("hasAuthority('FISCAL_NFCE_EMIT')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> emitFromSale(@PathVariable UUID saleId) {
        return ResponseEntity.ok(ApiResponse.of(nfceEmissionService.emitFromPosSale(saleId)));
    }
}
