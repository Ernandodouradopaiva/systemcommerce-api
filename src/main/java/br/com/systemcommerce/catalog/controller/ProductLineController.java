package br.com.systemcommerce.catalog.controller;

import br.com.systemcommerce.catalog.dto.ProductLineCreateRequest;
import br.com.systemcommerce.catalog.dto.ProductLineResponse;
import br.com.systemcommerce.catalog.dto.ProductLineUpdateRequest;
import br.com.systemcommerce.catalog.entity.ProductLine;
import br.com.systemcommerce.catalog.service.ProductLineService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/product-lines")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Product Lines", description = "Cadastro de linhas de produto (Prompt 65)")
public class ProductLineController {

    private final ProductLineService productLineService;

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_LINE_READ') or hasAuthority('PRODUCT_LINE_MANAGE')")
    @Operation(summary = "Lista linhas de produto paginadas")
    public ResponseEntity<PageResponse<ProductLineResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) ProductLine.ProductLineStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(productLineService.list(organizationId, brandId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_LINE_READ') or hasAuthority('PRODUCT_LINE_MANAGE')")
    @Operation(summary = "Consulta linha de produto por ID")
    public ResponseEntity<ApiResponse<ProductLineResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productLineService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_LINE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria linha de produto")
    public ResponseEntity<ApiResponse<ProductLineResponse>> create(
            @Valid @RequestBody ProductLineCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(productLineService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_LINE_MANAGE')")
    @Operation(summary = "Atualiza linha de produto")
    public ResponseEntity<ApiResponse<ProductLineResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ProductLineUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(productLineService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('PRODUCT_LINE_MANAGE')")
    @Operation(summary = "Ativa linha de produto")
    public ResponseEntity<ApiResponse<ProductLineResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productLineService.activate(id)));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('PRODUCT_LINE_MANAGE')")
    @Operation(summary = "Inativa linha de produto")
    public ResponseEntity<ApiResponse<ProductLineResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productLineService.inactivate(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_LINE_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui linha de produto (lógica se houver produtos vinculados)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productLineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
