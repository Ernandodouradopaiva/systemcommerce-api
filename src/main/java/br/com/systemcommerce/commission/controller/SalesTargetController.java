package br.com.systemcommerce.commission.controller;

import br.com.systemcommerce.commission.dto.SalesTargetCreateRequest;
import br.com.systemcommerce.commission.dto.SalesTargetResponse;
import br.com.systemcommerce.commission.service.SalesTargetService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales-targets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Sales Targets", description = "Metas de vendas por loja/vendedor")
public class SalesTargetController {

    private final SalesTargetService salesTargetService;

    @GetMapping
    @PreAuthorize("hasAuthority('SALES_TARGET_READ') or hasAuthority('SALES_TARGET_MANAGE')")
    @Operation(summary = "Lista metas de vendas")
    public ResponseEntity<PageResponse<SalesTargetResponse>> list(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(salesTargetService.list(organizationId, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALES_TARGET_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria meta de vendas")
    public ResponseEntity<ApiResponse<SalesTargetResponse>> create(@Valid @RequestBody SalesTargetCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(salesTargetService.create(request)));
    }
}
