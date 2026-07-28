package br.com.systemcommerce.supplier.controller;

import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.supplier.dto.SupplierCommercialConditionRequest;
import br.com.systemcommerce.supplier.dto.SupplierCommercialConditionResponse;
import br.com.systemcommerce.supplier.service.SupplierCommercialConditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Condições comerciais padrão (nível organização) — apenas referência; totais reais no pedido de compra. */
@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/commercial-conditions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Suppliers", description = "Condições comerciais padrão do fornecedor (Prompt 57)")
public class SupplierCommercialConditionController {

    private final SupplierCommercialConditionService conditionService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(summary = "Consulta condições comerciais padrão do fornecedor")
    public ResponseEntity<ApiResponse<SupplierCommercialConditionResponse>> get(@PathVariable UUID supplierId) {
        return ResponseEntity.ok(ApiResponse.of(conditionService.get(supplierId).orElse(null)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @Operation(summary = "Cria/atualiza condições comerciais padrão do fornecedor")
    public ResponseEntity<ApiResponse<SupplierCommercialConditionResponse>> upsert(
            @PathVariable UUID supplierId, @Valid @RequestBody SupplierCommercialConditionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(conditionService.upsert(supplierId, request)));
    }
}
