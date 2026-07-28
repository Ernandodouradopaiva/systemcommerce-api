package br.com.systemcommerce.finance.billing.controller;

import br.com.systemcommerce.finance.billing.dto.BillingDtos.*;
import br.com.systemcommerce.finance.billing.service.BillingService;
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
@Tag(name = "Billing", description = "Boletos e cobranças PIX (Prompt 113)")
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/billing-documents")
    @PreAuthorize("hasAuthority('BILLING_READ')")
    public ResponseEntity<PageResponse<BillingResponse>> list(
            @RequestParam UUID organizationId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(billingService.list(organizationId, pageable)));
    }

    @GetMapping("/billing-documents/{id}")
    @PreAuthorize("hasAuthority('BILLING_READ')")
    public ResponseEntity<ApiResponse<BillingResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(billingService.get(id)));
    }

    @PostMapping("/billing-documents")
    @PreAuthorize("hasAuthority('BILLING_CREATE')")
    public ResponseEntity<ApiResponse<BillingResponse>> create(@Valid @RequestBody CreateBillingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(billingService.create(request)));
    }

    @PostMapping("/billing-documents/{id}/register")
    @PreAuthorize("hasAuthority('BILLING_CREATE')")
    public ResponseEntity<ApiResponse<BillingResponse>> register(
            @PathVariable UUID id, @RequestBody(required = false) RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.of(billingService.register(id, request, null)));
    }

    @PostMapping("/billing-documents/{id}/cancel")
    @PreAuthorize("hasAuthority('BILLING_CANCEL')")
    public ResponseEntity<ApiResponse<BillingResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) CancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(billingService.cancel(id, request)));
    }

    @PostMapping("/billing/webhooks")
    @PreAuthorize("hasAuthority('BILLING_WEBHOOK')")
    public ResponseEntity<ApiResponse<BillingResponse>> webhook(@Valid @RequestBody WebhookRequest request) {
        BillingResponse response = billingService.processWebhook(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
