package br.com.systemcommerce.uom.controller;

import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.uom.dto.UnitConversionCreateRequest;
import br.com.systemcommerce.uom.dto.UnitConversionResponse;
import br.com.systemcommerce.uom.dto.UnitConversionSimulateRequest;
import br.com.systemcommerce.uom.dto.UnitConversionSimulateResponse;
import br.com.systemcommerce.uom.service.UnitConversionService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/unit-conversions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Unit Conversions", description = "Conversões entre unidades de medida (Prompt 66)")
public class UnitConversionController {

    private final UnitConversionService unitConversionService;

    @GetMapping
    @PreAuthorize("hasAuthority('UOM_READ') or hasAuthority('UOM_MANAGE')")
    @Operation(summary = "Lista conversões cadastradas")
    public ResponseEntity<PageResponse<UnitConversionResponse>> list(@PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(unitConversionService.list(pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('UOM_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria conversão entre duas unidades")
    public ResponseEntity<ApiResponse<UnitConversionResponse>> create(
            @Valid @RequestBody UnitConversionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(unitConversionService.create(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('UOM_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove conversão")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        unitConversionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/simulate")
    @PreAuthorize("hasAuthority('UOM_READ') or hasAuthority('UOM_MANAGE')")
    @Operation(summary = "Simula conversão de quantidade entre unidades")
    public ResponseEntity<ApiResponse<UnitConversionSimulateResponse>> simulate(
            @Valid @RequestBody UnitConversionSimulateRequest request) {
        var converted = unitConversionService.convert(request.fromUnitId(), request.toUnitId(), request.quantity());
        return ResponseEntity.ok(ApiResponse.of(new UnitConversionSimulateResponse(request.quantity(), converted)));
    }
}
