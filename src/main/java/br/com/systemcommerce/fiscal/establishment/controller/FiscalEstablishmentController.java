package br.com.systemcommerce.fiscal.establishment.controller;

import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentAvailabilityResponse;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentCreateRequest;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentEnvironmentRequest;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentHistoryResponse;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentResponse;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentSeriesRequest;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentUpdateRequest;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.service.FiscalEstablishmentService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/establishments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Establishments", description = "Estabelecimentos fiscais por loja (Prompt 122)")
public class FiscalEstablishmentController {

    private final FiscalEstablishmentService establishmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('FISCAL_ESTABLISHMENT_READ')")
    public ResponseEntity<PageResponse<FiscalEstablishmentResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) FiscalEstablishment.EstablishmentStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(establishmentService.list(organizationId, storeId, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_ESTABLISHMENT_READ')")
    public ResponseEntity<ApiResponse<FiscalEstablishmentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(establishmentService.getById(id)));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('FISCAL_ESTABLISHMENT_READ')")
    public ResponseEntity<ApiResponse<List<FiscalEstablishmentHistoryResponse>>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(establishmentService.history(id)));
    }

    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAuthority('FISCAL_ESTABLISHMENT_READ')")
    public ResponseEntity<ApiResponse<FiscalEstablishmentAvailabilityResponse>> availability(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(establishmentService.availability(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FISCAL_ESTABLISHMENT_CREATE')")
    public ResponseEntity<ApiResponse<FiscalEstablishmentResponse>> create(
            @Valid @RequestBody FiscalEstablishmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(establishmentService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_ESTABLISHMENT_UPDATE')")
    public ResponseEntity<ApiResponse<FiscalEstablishmentResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody FiscalEstablishmentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(establishmentService.update(id, request)));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('FISCAL_ESTABLISHMENT_UPDATE')")
    public ResponseEntity<ApiResponse<FiscalEstablishmentAvailabilityResponse>> validate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(establishmentService.validate(id)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FISCAL_ESTABLISHMENT_UPDATE')")
    public ResponseEntity<ApiResponse<FiscalEstablishmentResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(establishmentService.activate(id)));
    }

    @PostMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('FISCAL_ESTABLISHMENT_UPDATE')")
    public ResponseEntity<ApiResponse<FiscalEstablishmentResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(establishmentService.inactivate(id)));
    }

    @PostMapping("/{id}/environment")
    @PreAuthorize("hasAuthority('FISCAL_ENVIRONMENT_CHANGE')")
    public ResponseEntity<ApiResponse<FiscalEstablishmentResponse>> changeEnvironment(
            @PathVariable UUID id, @Valid @RequestBody FiscalEstablishmentEnvironmentRequest request) {
        return ResponseEntity.ok(ApiResponse.of(establishmentService.changeEnvironment(id, request)));
    }

    @PutMapping("/{id}/series")
    @PreAuthorize("hasAuthority('FISCAL_SERIES_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalEstablishmentResponse>> updateSeries(
            @PathVariable UUID id, @Valid @RequestBody FiscalEstablishmentSeriesRequest request) {
        return ResponseEntity.ok(ApiResponse.of(establishmentService.updateSeries(id, request)));
    }
}
