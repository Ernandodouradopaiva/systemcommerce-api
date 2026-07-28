package br.com.systemcommerce.finance.transfer.controller;

import br.com.systemcommerce.finance.transfer.dto.FinancialTransferDtos.CreateRequest;
import br.com.systemcommerce.finance.transfer.dto.FinancialTransferDtos.Response;
import br.com.systemcommerce.finance.transfer.entity.FinancialTransfer;
import br.com.systemcommerce.finance.transfer.service.FinancialTransferService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
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
@Tag(name = "Financial Transfers", description = "Transferências entre contas/caixas (Prompt 107)")
public class FinancialTransferController {

    private final FinancialTransferService transferService;

    @GetMapping("/financial-transfers")
    @PreAuthorize("hasAuthority('FINANCIAL_TRANSFER_READ')")
    public ResponseEntity<PageResponse<Response>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) FinancialTransfer.Status status,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(transferService.list(organizationId, status, pageable)));
    }

    @GetMapping("/financial-transfers/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_TRANSFER_READ')")
    public ResponseEntity<ApiResponse<Response>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(transferService.get(id)));
    }

    @PostMapping("/financial-transfers")
    @PreAuthorize("hasAuthority('FINANCIAL_TRANSFER_CREATE')")
    public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(transferService.createDraft(request)));
    }

    @PostMapping("/financial-transfers/{id}/confirm")
    @PreAuthorize("hasAuthority('FINANCIAL_TRANSFER_CONFIRM')")
    public ResponseEntity<ApiResponse<Response>> confirm(
            @PathVariable UUID id, @RequestParam(required = false) UUID approvalRequestId) {
        return ResponseEntity.ok(ApiResponse.of(transferService.confirm(id, approvalRequestId)));
    }

    @PostMapping("/financial-transfers/{id}/reverse")
    @PreAuthorize("hasAuthority('FINANCIAL_TRANSFER_CONFIRM')")
    public ResponseEntity<ApiResponse<Response>> reverse(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(transferService.reverse(id, notes)));
    }

    @PostMapping("/financial-transfers/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCIAL_TRANSFER_CREATE')")
    public ResponseEntity<ApiResponse<Response>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, @NotBlank String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(transferService.cancelDraft(id, notes)));
    }
}
