package br.com.systemcommerce.uom.controller;

import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.uom.dto.UnitOfMeasureCreateRequest;
import br.com.systemcommerce.uom.dto.UnitOfMeasureResponse;
import br.com.systemcommerce.uom.dto.UnitOfMeasureUpdateRequest;
import br.com.systemcommerce.uom.entity.UnitOfMeasure;
import br.com.systemcommerce.uom.service.UnitOfMeasureService;
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
@RequestMapping("/api/v1/units-of-measure")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Units of Measure", description = "Unidades de medida e conversões (Prompt 66)")
public class UnitOfMeasureController {

    private final UnitOfMeasureService unitOfMeasureService;

    @GetMapping
    @PreAuthorize("hasAuthority('UOM_READ') or hasAuthority('UOM_MANAGE')")
    @Operation(summary = "Lista unidades de medida")
    public ResponseEntity<PageResponse<UnitOfMeasureResponse>> list(
            @RequestParam(required = false) UnitOfMeasure.UomStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(unitOfMeasureService.list(status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('UOM_READ') or hasAuthority('UOM_MANAGE')")
    @Operation(summary = "Consulta unidade de medida por ID")
    public ResponseEntity<ApiResponse<UnitOfMeasureResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(unitOfMeasureService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('UOM_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria unidade de medida")
    public ResponseEntity<ApiResponse<UnitOfMeasureResponse>> create(
            @Valid @RequestBody UnitOfMeasureCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(unitOfMeasureService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UOM_MANAGE')")
    @Operation(summary = "Atualiza unidade de medida")
    public ResponseEntity<ApiResponse<UnitOfMeasureResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UnitOfMeasureUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(unitOfMeasureService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('UOM_MANAGE')")
    @Operation(summary = "Ativa unidade de medida")
    public ResponseEntity<ApiResponse<UnitOfMeasureResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(unitOfMeasureService.activate(id)));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('UOM_MANAGE')")
    @Operation(summary = "Inativa unidade de medida (bloqueado para unidade de sistema)")
    public ResponseEntity<ApiResponse<UnitOfMeasureResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(unitOfMeasureService.inactivate(id)));
    }
}
