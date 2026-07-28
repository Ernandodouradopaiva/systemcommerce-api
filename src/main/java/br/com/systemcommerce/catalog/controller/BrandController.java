package br.com.systemcommerce.catalog.controller;

import br.com.systemcommerce.catalog.dto.BrandCreateRequest;
import br.com.systemcommerce.catalog.dto.BrandResponse;
import br.com.systemcommerce.catalog.dto.BrandUpdateRequest;
import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.catalog.service.BrandService;
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
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Brands", description = "Cadastro de marcas (Prompt 65)")
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    @PreAuthorize("hasAuthority('BRAND_READ') or hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Lista marcas paginadas")
    public ResponseEntity<PageResponse<BrandResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) Brand.BrandStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(brandService.list(organizationId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BRAND_READ') or hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Consulta marca por ID")
    public ResponseEntity<ApiResponse<BrandResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(brandService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BRAND_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria marca")
    public ResponseEntity<ApiResponse<BrandResponse>> create(@Valid @RequestBody BrandCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(brandService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Atualiza marca")
    public ResponseEntity<ApiResponse<BrandResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody BrandUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(brandService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Ativa marca")
    public ResponseEntity<ApiResponse<BrandResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(brandService.activate(id)));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Inativa marca")
    public ResponseEntity<ApiResponse<BrandResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(brandService.inactivate(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BRAND_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui marca (lógica se houver produtos vinculados)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
