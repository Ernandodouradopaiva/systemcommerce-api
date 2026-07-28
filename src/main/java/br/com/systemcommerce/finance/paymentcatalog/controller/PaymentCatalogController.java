package br.com.systemcommerce.finance.paymentcatalog.controller;

import br.com.systemcommerce.finance.paymentcatalog.dto.CalculateDueDatesRequest;
import br.com.systemcommerce.finance.paymentcatalog.dto.CalculateDueDatesResponse;
import br.com.systemcommerce.finance.paymentcatalog.dto.PaymentConditionCreateRequest;
import br.com.systemcommerce.finance.paymentcatalog.dto.PaymentConditionResponse;
import br.com.systemcommerce.finance.paymentcatalog.dto.PaymentMethodCreateRequest;
import br.com.systemcommerce.finance.paymentcatalog.dto.PaymentMethodResponse;
import br.com.systemcommerce.finance.paymentcatalog.dto.PaymentMethodStoreConfigRequest;
import br.com.systemcommerce.finance.paymentcatalog.service.PaymentCatalogService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Payment Catalog", description = "Formas e condições de pagamento (Prompt 95)")
public class PaymentCatalogController {

    private final PaymentCatalogService paymentCatalogService;

    @GetMapping("/payment-methods")
    @PreAuthorize("hasAuthority('PAYMENT_METHOD_READ')")
    public ResponseEntity<PageResponse<PaymentMethodResponse>> listMethods(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(paymentCatalogService.listMethods(organizationId, pageable)));
    }

    @PostMapping("/payment-methods")
    @PreAuthorize("hasAuthority('PAYMENT_METHOD_MANAGE')")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> createMethod(
            @Valid @RequestBody PaymentMethodCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(paymentCatalogService.createMethod(request)));
    }

    @PostMapping("/payment-methods/{id}/activate")
    @PreAuthorize("hasAuthority('PAYMENT_METHOD_MANAGE')")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> activateMethod(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(paymentCatalogService.activateMethod(id)));
    }

    @PostMapping("/payment-methods/{id}/inactivate")
    @PreAuthorize("hasAuthority('PAYMENT_METHOD_MANAGE')")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> inactivateMethod(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(paymentCatalogService.inactivateMethod(id)));
    }

    @PostMapping("/payment-methods/{id}/store-configurations")
    @PreAuthorize("hasAuthority('PAYMENT_METHOD_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> configureStore(
            @PathVariable UUID id, @Valid @RequestBody PaymentMethodStoreConfigRequest request) {
        paymentCatalogService.configureStore(id, request);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @GetMapping("/payment-conditions")
    @PreAuthorize("hasAuthority('PAYMENT_CONDITION_READ')")
    public ResponseEntity<PageResponse<PaymentConditionResponse>> listConditions(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(paymentCatalogService.listConditions(organizationId, pageable)));
    }

    @PostMapping("/payment-conditions")
    @PreAuthorize("hasAuthority('PAYMENT_CONDITION_MANAGE')")
    public ResponseEntity<ApiResponse<PaymentConditionResponse>> createCondition(
            @Valid @RequestBody PaymentConditionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(paymentCatalogService.createCondition(request)));
    }

    @PostMapping("/payment-conditions/{id}/activate")
    @PreAuthorize("hasAuthority('PAYMENT_CONDITION_MANAGE')")
    public ResponseEntity<ApiResponse<PaymentConditionResponse>> activateCondition(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(paymentCatalogService.activateCondition(id)));
    }

    @PostMapping("/payment-conditions/{id}/inactivate")
    @PreAuthorize("hasAuthority('PAYMENT_CONDITION_MANAGE')")
    public ResponseEntity<ApiResponse<PaymentConditionResponse>> inactivateCondition(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(paymentCatalogService.inactivateCondition(id)));
    }

    @PostMapping("/payment-conditions/{id}/calculate-due-dates")
    @PreAuthorize("hasAuthority('PAYMENT_CONDITION_READ')")
    public ResponseEntity<ApiResponse<CalculateDueDatesResponse>> calculateDueDates(
            @PathVariable UUID id, @Valid @RequestBody CalculateDueDatesRequest request) {
        return ResponseEntity.ok(ApiResponse.of(paymentCatalogService.calculateDueDates(id, request)));
    }
}
