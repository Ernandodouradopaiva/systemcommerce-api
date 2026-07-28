package br.com.systemcommerce.batch.controller;

import br.com.systemcommerce.batch.dto.FefoPickLineResponse;
import br.com.systemcommerce.batch.dto.ProductBatchCreateRequest;
import br.com.systemcommerce.batch.dto.ProductBatchResponse;
import br.com.systemcommerce.batch.entity.ProductBatchStatus;
import br.com.systemcommerce.batch.service.ProductBatchService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/product-batches")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Product Batches", description = "Controle de lotes e validade (Prompt 76)")
public class ProductBatchController {

    private final ProductBatchService productBatchService;

    @GetMapping
    @PreAuthorize("hasAuthority('BATCH_READ')")
    @Operation(summary = "Lista lotes")
    public ResponseEntity<PageResponse<ProductBatchResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) ProductBatchStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(productBatchService.list(organizationId, productId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BATCH_READ')")
    @Operation(summary = "Consulta lote por ID")
    public ResponseEntity<ApiResponse<ProductBatchResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productBatchService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BATCH_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria lote")
    public ResponseEntity<ApiResponse<ProductBatchResponse>> create(
            @Valid @RequestBody ProductBatchCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(productBatchService.create(request)));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAuthority('BATCH_MANAGE')")
    @Operation(summary = "Bloqueia lote")
    public ResponseEntity<ApiResponse<ProductBatchResponse>> block(
            @PathVariable UUID id, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.of(productBatchService.block(id, reason)));
    }

    @GetMapping("/fefo-pick")
    @PreAuthorize("hasAuthority('BATCH_READ')")
    @Operation(summary = "Separação FEFO por produto/depósito")
    public ResponseEntity<ApiResponse<List<FefoPickLineResponse>>> fefoPick(
            @RequestParam UUID productId,
            @RequestParam UUID warehouseId,
            @RequestParam BigDecimal quantity) {
        return ResponseEntity.ok(ApiResponse.of(productBatchService.fefoPick(productId, warehouseId, quantity)));
    }
}
