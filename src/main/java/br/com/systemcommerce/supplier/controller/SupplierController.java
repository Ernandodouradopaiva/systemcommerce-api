package br.com.systemcommerce.supplier.controller;

import br.com.systemcommerce.purchase.dto.PurchaseOrderResponse;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.service.PurchaseOrderService;
import br.com.systemcommerce.shared.exception.ApiErrorResponse;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import br.com.systemcommerce.supplier.dto.SupplierBlockRequest;
import br.com.systemcommerce.supplier.dto.SupplierCreateRequest;
import br.com.systemcommerce.supplier.dto.SupplierResponse;
import br.com.systemcommerce.supplier.dto.SupplierStatusHistoryResponse;
import br.com.systemcommerce.supplier.dto.SupplierUpdateRequest;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Suppliers", description = "Cadastro profissional de fornecedores (PF/PJ) — Prompts 56–57")
public class SupplierController {

    private final SupplierService supplierService;
    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(
            summary = "Lista fornecedores paginados",
            description = "Filtros: code, name, document, status (ACTIVE|INACTIVE|BLOCKED) e search")
    public ResponseEntity<PageResponse<SupplierResponse>> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String document,
            @RequestParam(required = false) Supplier.SupplierStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(supplierService.list(code, name, document, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(summary = "Consulta fornecedor por ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                headers = @Header(name = CorrelationIdConstants.HEADER)),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<SupplierResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(summary = "Histórico de status do fornecedor (ativação/inativação/bloqueio) — nunca apagado")
    public ResponseEntity<ApiResponse<List<SupplierStatusHistoryResponse>>> statusHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierService.statusHistory(id)));
    }

    @GetMapping("/{id}/purchase-orders")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_READ')")
    @Operation(summary = "Lista pedidos de compra do fornecedor (respeita acesso por loja)")
    public ResponseEntity<PageResponse<PurchaseOrderResponse>> purchaseOrders(
            @PathVariable UUID id,
            @RequestParam(required = false) PurchaseOrder.PurchaseOrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(purchaseOrderService.list(status, null, id, null, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria fornecedor PF ou PJ")
    public ResponseEntity<ApiResponse<SupplierResponse>> create(@Valid @RequestBody SupplierCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(supplierService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @Operation(summary = "Atualiza fornecedor")
    public ResponseEntity<ApiResponse<SupplierResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody SupplierUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(supplierService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SUPPLIER_STATUS_MANAGE') or hasAuthority('SUPPLIER_UPDATE')")
    @Operation(summary = "Ativa fornecedor")
    public ResponseEntity<ApiResponse<SupplierResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierService.activate(id)));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('SUPPLIER_STATUS_MANAGE') or hasAuthority('SUPPLIER_DELETE') or hasAuthority('SUPPLIER_UPDATE')")
    @Operation(summary = "Inativa fornecedor")
    public ResponseEntity<ApiResponse<SupplierResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierService.deactivate(id)));
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasAuthority('SUPPLIER_STATUS_MANAGE')")
    @Operation(
            summary = "Bloqueia fornecedor",
            description = "Fornecedor bloqueado não participa de novas compras até ser desbloqueado")
    public ResponseEntity<ApiResponse<SupplierResponse>> block(
            @PathVariable UUID id, @Valid @RequestBody SupplierBlockRequest request) {
        return ResponseEntity.ok(ApiResponse.of(supplierService.block(id, request.reason())));
    }

    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasAuthority('SUPPLIER_STATUS_MANAGE')")
    @Operation(summary = "Desbloqueia fornecedor (retorna para ACTIVE)")
    public ResponseEntity<ApiResponse<SupplierResponse>> unblock(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(supplierService.unblock(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Exclui fornecedor",
            description =
                    "Exclusão lógica (inativação) se houver vínculo (estoque/documentos/pedidos); caso contrário remove o registro")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
