package br.com.systemcommerce.carrier.controller;

import br.com.systemcommerce.carrier.dto.FreightModeRequest;
import br.com.systemcommerce.carrier.dto.FreightModeResponse;
import br.com.systemcommerce.carrier.dto.FreightQuotationRequest;
import br.com.systemcommerce.carrier.dto.FreightQuotationResponse;
import br.com.systemcommerce.carrier.dto.FreightTableCreateRequest;
import br.com.systemcommerce.carrier.dto.FreightTableResponse;
import br.com.systemcommerce.carrier.service.FreightCatalogService;
import br.com.systemcommerce.carrier.service.FreightQuotationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Freight", description = "Modalidades, tabelas e cotação de frete (Prompt 73)")
public class FreightController {

    private final FreightCatalogService freightCatalogService;
    private final FreightQuotationService freightQuotationService;

    @GetMapping("/api/v1/freight-modes")
    @PreAuthorize("hasAuthority('CARRIER_READ')")
    @Operation(summary = "Lista modalidades de frete")
    public ResponseEntity<PageResponse<FreightModeResponse>> listModes(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(freightCatalogService.listModes(organizationId, pageable)));
    }

    @PostMapping("/api/v1/freight-modes")
    @PreAuthorize("hasAuthority('CARRIER_MANAGE')")
    @Operation(summary = "Cria modalidade de frete")
    public ResponseEntity<ApiResponse<FreightModeResponse>> createMode(
            @Valid @RequestBody FreightModeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(freightCatalogService.createMode(request)));
    }

    @PostMapping("/api/v1/freight-modes/{id}/inactivate")
    @PreAuthorize("hasAuthority('CARRIER_MANAGE')")
    @Operation(summary = "Inativa modalidade de frete")
    public ResponseEntity<ApiResponse<FreightModeResponse>> inactivateMode(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(freightCatalogService.inactivateMode(id)));
    }

    @GetMapping("/api/v1/freight-tables")
    @PreAuthorize("hasAuthority('CARRIER_READ')")
    @Operation(summary = "Lista tabelas de frete")
    public ResponseEntity<PageResponse<FreightTableResponse>> listTables(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(freightCatalogService.listTables(organizationId, pageable)));
    }

    @GetMapping("/api/v1/freight-tables/{id}")
    @PreAuthorize("hasAuthority('CARRIER_READ')")
    @Operation(summary = "Consulta tabela de frete por ID")
    public ResponseEntity<ApiResponse<FreightTableResponse>> getTable(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(freightCatalogService.getTableById(id)));
    }

    @PostMapping("/api/v1/freight-tables")
    @PreAuthorize("hasAuthority('CARRIER_MANAGE')")
    @Operation(summary = "Cria tabela de frete com regiões")
    public ResponseEntity<ApiResponse<FreightTableResponse>> createTable(
            @Valid @RequestBody FreightTableCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(freightCatalogService.createTable(request)));
    }

    @PostMapping("/api/v1/freight-tables/{id}/inactivate")
    @PreAuthorize("hasAuthority('CARRIER_MANAGE')")
    @Operation(summary = "Inativa tabela de frete")
    public ResponseEntity<ApiResponse<FreightTableResponse>> inactivateTable(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(freightCatalogService.inactivateTable(id)));
    }

    @PostMapping("/api/v1/freight-quotations/calculate")
    @PreAuthorize("hasAnyAuthority('CARRIER_READ', 'CARRIER_MANAGE')")
    @Operation(
            summary = "Calcula frete por tabela (CEP/peso/valor)",
            description = "Se manualOverrideAmount for informado, exige a permissão CARRIER_MANAGE.")
    public ResponseEntity<ApiResponse<FreightQuotationResponse>> calculate(
            @Valid @RequestBody FreightQuotationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(freightQuotationService.calculate(request)));
    }
}
