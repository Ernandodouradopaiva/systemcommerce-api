package br.com.systemcommerce.finance.reversal.controller;

import br.com.systemcommerce.finance.reversal.dto.FinancialReversalDtos.CreateRequest;
import br.com.systemcommerce.finance.reversal.dto.FinancialReversalDtos.Response;
import br.com.systemcommerce.finance.reversal.service.FinancialReversalService;
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
@Tag(name = "Financial Reversals", description = "Estornos financeiros (Prompt 109)")
public class FinancialReversalController {

    private final FinancialReversalService reversalService;

    @GetMapping("/financial-reversals")
    @PreAuthorize("hasAuthority('FINANCIAL_REVERSAL_READ')")
    public ResponseEntity<PageResponse<Response>> list(
            @RequestParam(required = false) UUID organizationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(reversalService.list(organizationId, pageable)));
    }

    @GetMapping("/financial-reversals/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_REVERSAL_READ')")
    public ResponseEntity<ApiResponse<Response>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reversalService.get(id)));
    }

    @PostMapping("/financial-reversals")
    @PreAuthorize("hasAuthority('FINANCIAL_REVERSAL_CREATE')")
    public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(reversalService.createAndConfirm(request)));
    }
}
