package br.com.systemcommerce.purchase.controller;

import br.com.systemcommerce.purchase.dto.SupplierReturnCreateRequest;
import br.com.systemcommerce.purchase.dto.SupplierReturnResponse;
import br.com.systemcommerce.purchase.dto.SupplierReturnStatusHistoryResponse;
import br.com.systemcommerce.purchase.dto.SupplierReturnUpdateRequest;
import br.com.systemcommerce.purchase.entity.SupplierReturn;
import br.com.systemcommerce.purchase.service.SupplierReturnService;
import br.com.systemcommerce.shared.exception.ApiErrorResponse;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/supplier-returns")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Supplier Returns", description = "Devolução ao fornecedor (Prompt 63) — só baixa estoque ao concluir")
public class SupplierReturnController {

    private final SupplierReturnService supplierReturnService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_READ')")
    @Operation(summary = "Lista devoluções ao fornecedor")
    public ResponseEntity<PageResponse<SupplierReturnResponse>> list(
            @RequestParam(required = false) SupplierReturn.SupplierReturnStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(supplierReturnService.list(status, storeId, supplierId, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_READ')")
    @Operation(summary = "Consulta devolução por ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                headers = @Header(name = CorrelationIdConstants.HEADER)),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<SupplierReturnResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierReturnService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_READ')")
    @Operation(summary = "Histórico de status da devolução")
    public ResponseEntity<ApiResponse<List<SupplierReturnStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierReturnService.statusHistory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria devolução ao fornecedor em DRAFT")
    public ResponseEntity<ApiResponse<SupplierReturnResponse>> create(
            @Valid @RequestBody SupplierReturnCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(supplierReturnService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_UPDATE')")
    @Operation(summary = "Atualiza devolução em DRAFT")
    public ResponseEntity<ApiResponse<SupplierReturnResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody SupplierReturnUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(supplierReturnService.update(id, request)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_UPDATE')")
    @Operation(summary = "Envia devolução para aprovação")
    public ResponseEntity<ApiResponse<SupplierReturnResponse>> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierReturnService.submit(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_UPDATE')")
    @Operation(summary = "Aprova a devolução")
    public ResponseEntity<ApiResponse<SupplierReturnResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierReturnService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_UPDATE')")
    @Operation(summary = "Rejeita a devolução (motivo obrigatório)")
    public ResponseEntity<ApiResponse<SupplierReturnResponse>> reject(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Motivo da rejeição é obrigatório");
        }
        return ResponseEntity.ok(ApiResponse.of(supplierReturnService.reject(id, reason)));
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_UPDATE')")
    @Operation(summary = "Despacha a devolução ao fornecedor")
    public ResponseEntity<ApiResponse<SupplierReturnResponse>> dispatch(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierReturnService.dispatch(id)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_UPDATE')")
    @Operation(
            summary = "Conclui a devolução (baixa oficial de estoque via InventoryService)",
            description = "Único ponto que movimenta estoque; valida saldo disponível antes da baixa")
    public ResponseEntity<ApiResponse<SupplierReturnResponse>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierReturnService.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SUPPLIER_RETURN_CANCEL')")
    @Operation(summary = "Cancela a devolução (antes do despacho/conclusão)")
    public ResponseEntity<ApiResponse<SupplierReturnResponse>> cancel(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Motivo do cancelamento é obrigatório");
        }
        return ResponseEntity.ok(ApiResponse.of(supplierReturnService.cancel(id, reason)));
    }
}
