package br.com.systemcommerce.finance.approval.controller;

import br.com.systemcommerce.finance.approval.dto.ApprovalDtos.*;
import br.com.systemcommerce.finance.approval.service.FinancialApprovalService;
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
@Tag(name = "Financial Approval", description = "Aprovação em duas etapas (Prompt 119)")
public class FinancialApprovalController {

    private final FinancialApprovalService service;

    @GetMapping("/financial-approval-policies/{organizationId}")
    @PreAuthorize("hasAuthority('FINANCE_APPROVAL_DECIDE') or hasAuthority('FINANCE_APPROVAL_REQUEST')")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicy(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(service.getOrCreatePolicy(organizationId)));
    }

    @PutMapping("/financial-approval-policies/{organizationId}")
    @PreAuthorize("hasAuthority('FINANCE_APPROVAL_DECIDE')")
    public ResponseEntity<ApiResponse<PolicyResponse>> updatePolicy(
            @PathVariable UUID organizationId, @Valid @RequestBody PolicyUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.updatePolicy(organizationId, request)));
    }

    @GetMapping("/financial-approval-requests")
    @PreAuthorize("hasAuthority('FINANCE_APPROVAL_REQUEST') or hasAuthority('FINANCE_APPROVAL_DECIDE')")
    public ResponseEntity<ApiResponse<List<ApprovalResponse>>> listPending(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(service.listPending(organizationId)));
    }

    @GetMapping("/financial-approval-requests/{id}")
    @PreAuthorize("hasAuthority('FINANCE_APPROVAL_REQUEST') or hasAuthority('FINANCE_APPROVAL_DECIDE')")
    public ResponseEntity<ApiResponse<ApprovalResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(service.get(id)));
    }

    @PostMapping("/financial-approval-requests")
    @PreAuthorize("hasAuthority('FINANCE_APPROVAL_REQUEST')")
    public ResponseEntity<ApiResponse<ApprovalResponse>> create(@Valid @RequestBody CreateApprovalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.create(request)));
    }

    @PostMapping("/financial-approval-requests/{id}/decide")
    @PreAuthorize("hasAuthority('FINANCE_APPROVAL_DECIDE') or hasAuthority('FINANCE_PAYMENT_APPROVE')")
    public ResponseEntity<ApiResponse<ApprovalResponse>> decide(
            @PathVariable UUID id, @Valid @RequestBody DecideRequest request) {
        return ResponseEntity.ok(ApiResponse.of(service.decide(id, request)));
    }
}
