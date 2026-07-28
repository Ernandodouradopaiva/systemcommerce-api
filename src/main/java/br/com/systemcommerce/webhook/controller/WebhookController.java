package br.com.systemcommerce.webhook.controller;

import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.webhook.dto.WebhookSubscriptionCreateRequest;
import br.com.systemcommerce.webhook.dto.WebhookSubscriptionResponse;
import br.com.systemcommerce.webhook.service.WebhookDispatcher;
import br.com.systemcommerce.webhook.service.WebhookSubscriptionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Webhooks", description = "Subscriptions e entregas (Prompt 82)")
public class WebhookController {

    private final WebhookSubscriptionService subscriptionService;
    private final WebhookDispatcher webhookDispatcher;

    @GetMapping
    @PreAuthorize("hasAuthority('WEBHOOK_READ')")
    public ResponseEntity<PageResponse<WebhookSubscriptionResponse>> list(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(subscriptionService.list(organizationId, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WebhookSubscriptionResponse> create(
            @Valid @RequestBody WebhookSubscriptionCreateRequest request) {
        return ApiResponse.of(subscriptionService.create(request));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ApiResponse<WebhookSubscriptionResponse> disable(@PathVariable UUID id) {
        return ApiResponse.of(subscriptionService.disable(id));
    }

    @PostMapping("/deliveries/{deliveryId}/replay")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ApiResponse<Map<String, Object>> replay(@PathVariable UUID deliveryId) {
        var d = webhookDispatcher.replay(deliveryId);
        return ApiResponse.of(Map.of(
                "id", d.getId(),
                "status", d.getStatus().name(),
                "attemptCount", d.getAttemptCount()));
    }
}
