package br.com.systemcommerce.fiscal.integration.returns.controller;

import br.com.systemcommerce.fiscal.integration.returns.dto.FiscalReturnLinkResponse;
import br.com.systemcommerce.fiscal.integration.returns.dto.PurchaseReturnEmitRequest;
import br.com.systemcommerce.fiscal.integration.returns.dto.SaleReturnEmitRequest;
import br.com.systemcommerce.fiscal.integration.returns.entity.FiscalReturnLink.ReturnType;
import br.com.systemcommerce.fiscal.integration.returns.service.FiscalReturnService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/returns")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Returns", description = "Devoluções fiscais (Prompt 141)")
public class FiscalReturnController {

    private final FiscalReturnService fiscalReturnService;

    @PostMapping("/sale")
    @PreAuthorize("hasAuthority('FISCAL_RETURN_EMIT')")
    public ResponseEntity<ApiResponse<FiscalReturnLinkResponse>> emitSaleReturn(
            @Valid @RequestBody SaleReturnEmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(fiscalReturnService.emitSaleReturn(request)));
    }

    @PostMapping("/purchase")
    @PreAuthorize("hasAuthority('FISCAL_RETURN_EMIT')")
    public ResponseEntity<ApiResponse<FiscalReturnLinkResponse>> emitPurchaseReturn(
            @Valid @RequestBody PurchaseReturnEmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(fiscalReturnService.emitPurchaseReturn(request)));
    }

    @GetMapping("/{returnType}/{returnId}")
    @PreAuthorize("hasAuthority('FISCAL_RETURN_READ')")
    public ResponseEntity<ApiResponse<FiscalReturnLinkResponse>> getByReturn(
            @PathVariable ReturnType returnType, @PathVariable UUID returnId) {
        return ResponseEntity.ok(ApiResponse.of(fiscalReturnService.getByReturn(returnType, returnId)));
    }
}
