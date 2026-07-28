package br.com.systemcommerce.finance.closing.controller;

import br.com.systemcommerce.finance.closing.dto.ClosingDtos.*;
import br.com.systemcommerce.finance.closing.service.FinancialClosingService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Financial Closing", description = "Fechamento financeiro (Prompt 115)")
public class FinancialClosingController {

    private final FinancialClosingService service;

    @GetMapping("/financial-periods")
    @PreAuthorize("hasAuthority('FINANCIAL_PERIOD_READ')")
    public ResponseEntity<ApiResponse<List<PeriodResponse>>> list(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(service.list(organizationId)));
    }

    @GetMapping("/financial-periods/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_PERIOD_READ')")
    public ResponseEntity<ApiResponse<PeriodResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(service.get(id)));
    }

    @PostMapping("/financial-periods")
    @PreAuthorize("hasAuthority('FINANCIAL_PERIOD_CLOSE')")
    public ResponseEntity<ApiResponse<PeriodResponse>> create(@Valid @RequestBody PeriodCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.createPeriod(request)));
    }

    @PostMapping("/financial-periods/{id}/close")
    @PreAuthorize("hasAuthority('FINANCIAL_PERIOD_CLOSE')")
    public ResponseEntity<ApiResponse<ClosingResponse>> close(
            @PathVariable UUID id, @RequestBody(required = false) CloseRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.close(id, request != null ? request : new CloseRequest(null, false))));
    }

    @PostMapping("/financial-periods/{id}/reopen")
    @PreAuthorize("hasAuthority('FINANCIAL_PERIOD_REOPEN')")
    public ResponseEntity<ApiResponse<PeriodResponse>> reopen(
            @PathVariable UUID id, @Valid @RequestBody ReopenRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.reopen(id, request)));
    }

    @GetMapping("/financial-closings/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_PERIOD_READ')")
    public ResponseEntity<ApiResponse<ClosingResponse>> getClosing(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(service.getClosing(id)));
    }
}
