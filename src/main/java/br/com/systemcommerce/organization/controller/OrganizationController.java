package br.com.systemcommerce.organization.controller;

import br.com.systemcommerce.organization.dto.OrganizationCreateRequest;
import br.com.systemcommerce.organization.dto.OrganizationResponse;
import br.com.systemcommerce.organization.dto.OrganizationUpdateRequest;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Organizations", description = "Cadastro da organização / empresa")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/default")
    @PreAuthorize("hasAuthority('ORGANIZATION_READ') or hasAuthority('ORGANIZATION_MANAGE')")
    @Operation(summary = "Consulta a organização padrão (seed)")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getDefault() {
        return ResponseEntity.ok(ApiResponse.of(organizationService.getDefault()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_READ') or hasAuthority('ORGANIZATION_MANAGE')")
    @Operation(summary = "Consulta organização por ID")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(organizationService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra organização")
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(
            @Valid @RequestBody OrganizationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(organizationService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_MANAGE')")
    @Operation(summary = "Atualiza organização")
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody OrganizationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(organizationService.update(id, request)));
    }
}
