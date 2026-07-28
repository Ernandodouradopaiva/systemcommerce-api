package br.com.systemcommerce.customerstore.controller;

import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipCreateRequest;
import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipNotesRequest;
import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipResponse;
import br.com.systemcommerce.customerstore.entity.CustomerStoreRelationshipStatus;
import br.com.systemcommerce.customerstore.service.CustomerStoreRelationshipService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer-store-relationships")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Customer Store Relationships", description = "Vínculo local cliente-loja (multiloja)")
public class CustomerStoreRelationshipController {

    private final CustomerStoreRelationshipService relationshipService;

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Lista vínculos por loja")
    public ResponseEntity<PageResponse<CustomerStoreRelationshipResponse>> listByStore(
            @RequestParam UUID storeId,
            @RequestParam(required = false) CustomerStoreRelationshipStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(relationshipService.listByStore(storeId, status, pageable)));
    }

    @GetMapping("/{customerId}/{storeId}")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Consulta vínculo cliente-loja")
    public ResponseEntity<ApiResponse<CustomerStoreRelationshipResponse>> get(
            @PathVariable UUID customerId, @PathVariable UUID storeId) {
        return ResponseEntity.ok(ApiResponse.of(relationshipService.getRelationship(customerId, storeId)));
    }

    @PostMapping("/{customerId}")
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE') or hasAuthority('CUSTOMER_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria vínculo cliente-loja")
    public ResponseEntity<ApiResponse<CustomerStoreRelationshipResponse>> create(
            @PathVariable UUID customerId, @Valid @RequestBody CustomerStoreRelationshipCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(relationshipService.create(customerId, request)));
    }

    @PatchMapping("/{customerId}/{storeId}/notes")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @Operation(summary = "Atualiza notas locais do vínculo")
    public ResponseEntity<ApiResponse<CustomerStoreRelationshipResponse>> updateNotes(
            @PathVariable UUID customerId,
            @PathVariable UUID storeId,
            @Valid @RequestBody CustomerStoreRelationshipNotesRequest request) {
        return ResponseEntity.ok(ApiResponse.of(relationshipService.updateLocalNotes(customerId, storeId, request)));
    }
}
