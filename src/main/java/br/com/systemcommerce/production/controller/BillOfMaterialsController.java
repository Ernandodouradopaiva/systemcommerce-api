package br.com.systemcommerce.production.controller;

import br.com.systemcommerce.production.dto.BillOfMaterialsCreateRequest;
import br.com.systemcommerce.production.dto.BillOfMaterialsResponse;
import br.com.systemcommerce.production.entity.BillOfMaterialsStatus;
import br.com.systemcommerce.production.service.BillOfMaterialsService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bills-of-materials")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Bills of Materials", description = "Fichas técnicas / BOM (Prompt 79)")
public class BillOfMaterialsController {

    private final BillOfMaterialsService billOfMaterialsService;

    @GetMapping
    @PreAuthorize("hasAuthority('BOM_READ')")
    @Operation(summary = "Lista fichas técnicas")
    public ResponseEntity<PageResponse<BillOfMaterialsResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID finishedProductId,
            @RequestParam(required = false) BillOfMaterialsStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                billOfMaterialsService.list(organizationId, finishedProductId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOM_READ')")
    @Operation(summary = "Consulta BOM por ID")
    public ResponseEntity<ApiResponse<BillOfMaterialsResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(billOfMaterialsService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BOM_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria ficha técnica")
    public ResponseEntity<ApiResponse<BillOfMaterialsResponse>> create(
            @Valid @RequestBody BillOfMaterialsCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(billOfMaterialsService.create(request)));
    }
}
