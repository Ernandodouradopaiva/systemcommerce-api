package br.com.systemcommerce.finance.advance.controller;

import br.com.systemcommerce.finance.advance.dto.AdvanceDtos.*;
import br.com.systemcommerce.finance.advance.entity.AdvanceApplication;
import br.com.systemcommerce.finance.advance.entity.AdvanceRefund;
import br.com.systemcommerce.finance.advance.service.AdvanceService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Advances", description = "Adiantamentos de clientes e fornecedores (Prompt 105)")
public class AdvanceController {

    private final AdvanceService advanceService;

    @GetMapping("/customer-advances")
    @PreAuthorize("hasAuthority('ADVANCE_READ')")
    public ResponseEntity<PageResponse<CustomerAdvanceResponse>> listCustomer(
            @RequestParam(required = false) UUID organizationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(advanceService.listCustomer(organizationId, pageable)));
    }

    @PostMapping("/customer-advances")
    @PreAuthorize("hasAuthority('ADVANCE_CREATE')")
    public ResponseEntity<ApiResponse<CustomerAdvanceResponse>> createCustomer(
            @Valid @RequestBody CustomerAdvanceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(advanceService.createCustomer(request)));
    }

    @GetMapping("/customer-advances/{id}")
    @PreAuthorize("hasAuthority('ADVANCE_READ')")
    public ResponseEntity<ApiResponse<CustomerAdvanceResponse>> getCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(advanceService.getCustomer(id)));
    }

    @GetMapping("/customer-advances/{id}/balance")
    @PreAuthorize("hasAuthority('ADVANCE_READ')")
    public ResponseEntity<ApiResponse<AdvanceBalanceResponse>> balanceCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(advanceService.balanceCustomer(id)));
    }

    @PostMapping("/customer-advances/{id}/cancel")
    @PreAuthorize("hasAuthority('ADVANCE_CREATE')")
    public ResponseEntity<ApiResponse<CustomerAdvanceResponse>> cancelCustomer(
            @PathVariable UUID id, @Valid @RequestBody AdvanceCancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(advanceService.cancelCustomer(id, request)));
    }

    @GetMapping("/supplier-advances")
    @PreAuthorize("hasAuthority('ADVANCE_READ')")
    public ResponseEntity<PageResponse<SupplierAdvanceResponse>> listSupplier(
            @RequestParam(required = false) UUID organizationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(advanceService.listSupplier(organizationId, pageable)));
    }

    @PostMapping("/supplier-advances")
    @PreAuthorize("hasAuthority('ADVANCE_CREATE')")
    public ResponseEntity<ApiResponse<SupplierAdvanceResponse>> createSupplier(
            @Valid @RequestBody SupplierAdvanceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(advanceService.createSupplier(request)));
    }

    @GetMapping("/supplier-advances/{id}")
    @PreAuthorize("hasAuthority('ADVANCE_READ')")
    public ResponseEntity<ApiResponse<SupplierAdvanceResponse>> getSupplier(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(advanceService.getSupplier(id)));
    }

    @GetMapping("/supplier-advances/{id}/balance")
    @PreAuthorize("hasAuthority('ADVANCE_READ')")
    public ResponseEntity<ApiResponse<AdvanceBalanceResponse>> balanceSupplier(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(advanceService.balanceSupplier(id)));
    }

    @PostMapping("/supplier-advances/{id}/cancel")
    @PreAuthorize("hasAuthority('ADVANCE_CREATE')")
    public ResponseEntity<ApiResponse<SupplierAdvanceResponse>> cancelSupplier(
            @PathVariable UUID id, @Valid @RequestBody AdvanceCancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(advanceService.cancelSupplier(id, request)));
    }

    @PostMapping("/advance-applications")
    @PreAuthorize("hasAuthority('ADVANCE_APPLY')")
    public ResponseEntity<ApiResponse<AdvanceApplication>> apply(@Valid @RequestBody AdvanceApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(advanceService.apply(request)));
    }

    @PostMapping("/advance-refunds")
    @PreAuthorize("hasAuthority('ADVANCE_REFUND')")
    public ResponseEntity<ApiResponse<AdvanceRefund>> refund(@Valid @RequestBody AdvanceRefundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(advanceService.refund(request)));
    }
}
