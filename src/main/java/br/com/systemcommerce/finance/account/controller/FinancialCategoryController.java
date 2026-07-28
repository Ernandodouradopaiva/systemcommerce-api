package br.com.systemcommerce.finance.account.controller;

import br.com.systemcommerce.finance.account.dto.FinancialCategoryCreateRequest;
import br.com.systemcommerce.finance.account.dto.FinancialCategoryResponse;
import br.com.systemcommerce.finance.account.dto.FinancialCategoryUpdateRequest;
import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import br.com.systemcommerce.finance.account.service.FinancialCategoryService;
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
@RequestMapping("/api/v1/financial-categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Financial Categories", description = "Categorias financeiras (Prompt 92)")
public class FinancialCategoryController {

    private final FinancialCategoryService financialCategoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_READ')")
    @Operation(summary = "Lista categorias financeiras")
    public ResponseEntity<PageResponse<FinancialCategoryResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) FinancialCategory.UsageScope usageScope,
            @RequestParam(required = false) FinancialCategory.CategoryStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                financialCategoryService.list(organizationId, usageScope, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_READ')")
    public ResponseEntity<ApiResponse<FinancialCategoryResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(financialCategoryService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_CREATE')")
    public ResponseEntity<ApiResponse<FinancialCategoryResponse>> create(
            @Valid @RequestBody FinancialCategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(financialCategoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_UPDATE')")
    public ResponseEntity<ApiResponse<FinancialCategoryResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody FinancialCategoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(financialCategoryService.update(id, request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_STATUS_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialCategoryResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(financialCategoryService.activate(id)));
    }

    @PostMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_STATUS_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialCategoryResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(financialCategoryService.inactivate(id)));
    }
}
