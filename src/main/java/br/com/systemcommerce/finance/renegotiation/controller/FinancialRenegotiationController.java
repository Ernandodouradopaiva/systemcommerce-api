package br.com.systemcommerce.finance.renegotiation.controller;

import br.com.systemcommerce.finance.renegotiation.dto.FinancialRenegotiationDtos.*;
import br.com.systemcommerce.finance.renegotiation.service.FinancialRenegotiationService;
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
@Tag(name = "Financial Renegotiations", description = "Renegociação de parcelas (Prompt 110)")
public class FinancialRenegotiationController {

    private final FinancialRenegotiationService renegotiationService;

    @GetMapping("/financial-renegotiations")
    @PreAuthorize("hasAuthority('FINANCIAL_RENEGOTIATION_READ')")
    public ResponseEntity<PageResponse<Response>> list(
            @RequestParam(required = false) UUID organizationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(renegotiationService.list(organizationId, pageable)));
    }

    @GetMapping("/financial-renegotiations/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_RENEGOTIATION_READ')")
    public ResponseEntity<ApiResponse<Response>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(renegotiationService.get(id)));
    }

    @PostMapping("/financial-renegotiations")
    @PreAuthorize("hasAuthority('FINANCIAL_RENEGOTIATION_CREATE')")
    public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(renegotiationService.create(request)));
    }

    @PostMapping("/financial-renegotiations/{id}/confirm")
    @PreAuthorize("hasAuthority('FINANCIAL_RENEGOTIATION_CREATE')")
    public ResponseEntity<ApiResponse<Response>> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(renegotiationService.confirm(id)));
    }

    @PostMapping("/financial-renegotiations/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCIAL_RENEGOTIATION_CANCEL')")
    public ResponseEntity<ApiResponse<Response>> cancel(
            @PathVariable UUID id, @Valid @RequestBody CancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(renegotiationService.cancel(id, request)));
    }
}
