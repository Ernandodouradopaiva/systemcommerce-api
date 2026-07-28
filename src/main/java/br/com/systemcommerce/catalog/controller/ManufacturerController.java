package br.com.systemcommerce.catalog.controller;

import br.com.systemcommerce.catalog.dto.ManufacturerCreateRequest;
import br.com.systemcommerce.catalog.dto.ManufacturerResponse;
import br.com.systemcommerce.catalog.dto.ManufacturerUpdateRequest;
import br.com.systemcommerce.catalog.entity.Manufacturer;
import br.com.systemcommerce.catalog.service.ManufacturerService;
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
@RequestMapping("/api/v1/manufacturers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Manufacturers", description = "Cadastro de fabricantes (Prompt 65)")
public class ManufacturerController {

    private final ManufacturerService manufacturerService;

    @GetMapping
    @PreAuthorize("hasAuthority('MANUFACTURER_READ') or hasAuthority('MANUFACTURER_MANAGE')")
    @Operation(summary = "Lista fabricantes paginados")
    public ResponseEntity<PageResponse<ManufacturerResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) Manufacturer.ManufacturerStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(manufacturerService.list(organizationId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANUFACTURER_READ') or hasAuthority('MANUFACTURER_MANAGE')")
    @Operation(summary = "Consulta fabricante por ID")
    public ResponseEntity<ApiResponse<ManufacturerResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(manufacturerService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANUFACTURER_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria fabricante")
    public ResponseEntity<ApiResponse<ManufacturerResponse>> create(
            @Valid @RequestBody ManufacturerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(manufacturerService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANUFACTURER_MANAGE')")
    @Operation(summary = "Atualiza fabricante")
    public ResponseEntity<ApiResponse<ManufacturerResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ManufacturerUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(manufacturerService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('MANUFACTURER_MANAGE')")
    @Operation(summary = "Ativa fabricante")
    public ResponseEntity<ApiResponse<ManufacturerResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(manufacturerService.activate(id)));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('MANUFACTURER_MANAGE')")
    @Operation(summary = "Inativa fabricante")
    public ResponseEntity<ApiResponse<ManufacturerResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(manufacturerService.inactivate(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANUFACTURER_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui fabricante (lógica se houver produtos vinculados)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        manufacturerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
