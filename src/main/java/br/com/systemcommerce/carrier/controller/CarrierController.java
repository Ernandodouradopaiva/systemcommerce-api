package br.com.systemcommerce.carrier.controller;

import br.com.systemcommerce.carrier.dto.CarrierCreateRequest;
import br.com.systemcommerce.carrier.dto.CarrierResponse;
import br.com.systemcommerce.carrier.dto.CarrierUpdateRequest;
import br.com.systemcommerce.carrier.entity.Carrier;
import br.com.systemcommerce.carrier.service.CarrierService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carriers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Carriers", description = "Transportadoras e contatos (Prompt 73)")
public class CarrierController {

    private final CarrierService carrierService;

    @GetMapping
    @PreAuthorize("hasAuthority('CARRIER_READ')")
    @Operation(summary = "Lista transportadoras")
    public ResponseEntity<PageResponse<CarrierResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) Carrier.CarrierStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(carrierService.list(organizationId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CARRIER_READ')")
    @Operation(summary = "Consulta transportadora por ID")
    public ResponseEntity<ApiResponse<CarrierResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(carrierService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CARRIER_MANAGE')")
    @Operation(summary = "Cria transportadora")
    public ResponseEntity<ApiResponse<CarrierResponse>> create(@Valid @RequestBody CarrierCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(carrierService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CARRIER_MANAGE')")
    @Operation(summary = "Atualiza transportadora")
    public ResponseEntity<ApiResponse<CarrierResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CarrierUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(carrierService.update(id, request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('CARRIER_MANAGE')")
    @Operation(summary = "Ativa transportadora")
    public ResponseEntity<ApiResponse<CarrierResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(carrierService.activate(id)));
    }

    @PostMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('CARRIER_MANAGE')")
    @Operation(summary = "Inativa transportadora (não poderá mais ser selecionada)")
    public ResponseEntity<ApiResponse<CarrierResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(carrierService.inactivate(id)));
    }
}
