package br.com.systemcommerce.publicapi.controller;

import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.publicapi.dto.PublicApiCredentialCreateRequest;
import br.com.systemcommerce.publicapi.dto.PublicApiCredentialCreatedResponse;
import br.com.systemcommerce.publicapi.dto.PublicApiCredentialResponse;
import br.com.systemcommerce.publicapi.dto.PublicApiTokenRequest;
import br.com.systemcommerce.publicapi.dto.PublicApiTokenResponse;
import br.com.systemcommerce.publicapi.entity.PublicApiCredential;
import br.com.systemcommerce.publicapi.service.PublicApiCredentialService;
import br.com.systemcommerce.salesorder.service.SalesOrderService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.webhook.service.WebhookSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Public API", description = "API pública versionada (Prompt 81)")
public class PublicApiController {

    private final PublicApiCredentialService credentialService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final PriceResolutionService priceResolutionService;
    private final SalesOrderService salesOrderService;
    private final StoreService storeService;
    private final WebhookSubscriptionService webhookSubscriptionService;

    @PostMapping("/api/public/v1/oauth/token")
    @Operation(summary = "Client credentials — emite access token da API pública")
    public PublicApiTokenResponse token(@Valid @RequestBody PublicApiTokenRequest request) {
        return credentialService.issueToken(request);
    }

    @GetMapping("/api/public/v1/products")
    public ResponseEntity<?> products(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        requireScope(request, "products.read");
        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(productService.list(
                null, null, null, null, Product.ProductStatus.ACTIVE, null, pageable))));
    }

    @GetMapping("/api/public/v1/inventory")
    public ResponseEntity<?> inventory(
            HttpServletRequest request, @RequestParam UUID productId, @RequestParam UUID warehouseId) {
        requireScope(request, "inventory.read");
        BigDecimal available = inventoryService.availableQuantity(productId, warehouseId);
        return ResponseEntity.ok(ApiResponse.of(Map.of(
                "productId", productId,
                "warehouseId", warehouseId,
                "availableQuantity", available)));
    }

    @GetMapping("/api/public/v1/prices")
    public ResponseEntity<?> prices(
            HttpServletRequest request,
            @RequestParam UUID productId,
            @RequestParam UUID storeId,
            @RequestParam(required = false) BigDecimal quantity) {
        requireScope(request, "prices.read");
        var resolved = priceResolutionService.resolve(
                productId,
                storeId,
                quantity != null ? quantity : BigDecimal.ONE,
                Instant.now(),
                PriceChannel.MARKETPLACE,
                null,
                null);
        return ResponseEntity.ok(ApiResponse.of(resolved));
    }

    @GetMapping("/api/public/v1/orders")
    public ResponseEntity<?> orders(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        requireScope(request, "orders.read");
        return ResponseEntity.ok(
                ApiResponse.of(PageResponse.from(salesOrderService.listByOrganization(orgId(request), pageable))));
    }

    @GetMapping("/api/public/v1/orders/{id}/status")
    public ResponseEntity<?> orderStatus(HttpServletRequest request, @PathVariable UUID id) {
        requireScope(request, "orders.read");
        var order = salesOrderService.getByIdForOrganization(id, orgId(request));
        return ResponseEntity.ok(ApiResponse.of(Map.of(
                "id", order.id(),
                "orderNumber", order.orderNumber(),
                "status", order.status())));
    }

    @GetMapping("/api/public/v1/stores")
    public ResponseEntity<?> stores(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        requireScope(request, "stores.read");
        return ResponseEntity.ok(
                ApiResponse.of(PageResponse.from(storeService.listOperational(orgId(request), pageable))));
    }

    @GetMapping("/api/public/v1/webhooks")
    public ResponseEntity<?> webhooks(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        requireScope(request, "webhooks.read");
        return ResponseEntity.ok(
                ApiResponse.of(PageResponse.from(webhookSubscriptionService.list(orgId(request), pageable))));
    }

    @GetMapping("/api/v1/public-api-credentials")
    @PreAuthorize("hasAuthority('PUBLIC_API_READ')")
    public ResponseEntity<PageResponse<PublicApiCredentialResponse>> listCredentials(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(credentialService.list(organizationId, pageable)));
    }

    @PostMapping("/api/v1/public-api-credentials")
    @PreAuthorize("hasAuthority('PUBLIC_API_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PublicApiCredentialCreatedResponse> createCredential(
            @Valid @RequestBody PublicApiCredentialCreateRequest request) {
        return ApiResponse.of(credentialService.create(request));
    }

    @PostMapping("/api/v1/public-api-credentials/{id}/revoke")
    @PreAuthorize("hasAuthority('PUBLIC_API_MANAGE')")
    public ApiResponse<PublicApiCredentialResponse> revoke(@PathVariable UUID id) {
        return ApiResponse.of(credentialService.revoke(id));
    }

    private void requireScope(HttpServletRequest request, String scope) {
        if (request.getAttribute("publicApiCredential") == null) {
            throw new BusinessRuleException("Autenticação da API pública obrigatória");
        }
        String scopes = (String) request.getAttribute("publicApiScopes");
        if (!credentialService.hasScope(scopes, scope)) {
            throw new BusinessRuleException("Escopo insuficiente: " + scope);
        }
        PublicApiCredential cred = (PublicApiCredential) request.getAttribute("publicApiCredential");
        credentialService.logAccess(
                cred,
                request.getMethod(),
                request.getRequestURI(),
                200,
                scope,
                request.getHeader("X-Correlation-Id"),
                request.getHeader("Idempotency-Key"));
    }

    private UUID orgId(HttpServletRequest request) {
        Object v = request.getAttribute("publicApiOrganizationId");
        if (v instanceof UUID uuid) {
            return uuid;
        }
        throw new BusinessRuleException("Organização não resolvida no token público");
    }
}
