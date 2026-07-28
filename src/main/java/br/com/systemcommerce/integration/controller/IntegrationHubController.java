package br.com.systemcommerce.integration.controller;

import br.com.systemcommerce.integration.dto.ChannelOrderResponse;
import br.com.systemcommerce.integration.dto.ChannelProductLinkRequest;
import br.com.systemcommerce.integration.dto.ChannelProductResponse;
import br.com.systemcommerce.integration.dto.IntegrationJobCreateRequest;
import br.com.systemcommerce.integration.dto.IntegrationJobResponse;
import br.com.systemcommerce.integration.dto.MarketplaceAccountCreateRequest;
import br.com.systemcommerce.integration.dto.MarketplaceAccountResponse;
import br.com.systemcommerce.integration.dto.SalesChannelCreateRequest;
import br.com.systemcommerce.integration.dto.SalesChannelResponse;
import br.com.systemcommerce.integration.service.ChannelOrderIngestionService;
import br.com.systemcommerce.integration.service.IntegrationHubService;
import br.com.systemcommerce.integration.service.IntegrationJobService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Integration Hub", description = "Hub genérico de marketplaces (Prompt 80)")
public class IntegrationHubController {

    private final IntegrationHubService hubService;
    private final ChannelOrderIngestionService orderIngestionService;
    private final IntegrationJobService jobService;

    @GetMapping("/sales-channels")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    @Operation(summary = "Listar canais de venda")
    public ResponseEntity<PageResponse<SalesChannelResponse>> listChannels(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(hubService.listChannels(organizationId, pageable)));
    }

    @PostMapping("/sales-channels")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SalesChannelResponse> createChannel(@Valid @RequestBody SalesChannelCreateRequest request) {
        return ApiResponse.of(hubService.createChannel(request));
    }

    @GetMapping("/marketplace-accounts")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ResponseEntity<PageResponse<MarketplaceAccountResponse>> listAccounts(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(hubService.listAccounts(organizationId, pageable)));
    }

    @PostMapping("/marketplace-accounts")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MarketplaceAccountResponse> createAccount(
            @Valid @RequestBody MarketplaceAccountCreateRequest request) {
        return ApiResponse.of(hubService.createAccount(request));
    }

    @PostMapping("/channel-products/link")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChannelProductResponse> linkProduct(@Valid @RequestBody ChannelProductLinkRequest request) {
        return ApiResponse.of(hubService.linkProduct(request));
    }

    @GetMapping("/channel-orders")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ResponseEntity<PageResponse<ChannelOrderResponse>> listOrders(
            @RequestParam(required = false) UUID marketplaceAccountId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(orderIngestionService.list(marketplaceAccountId, pageable)));
    }

    @GetMapping("/channel-orders/{id}")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ApiResponse<ChannelOrderResponse> getOrder(@PathVariable UUID id) {
        return ApiResponse.of(orderIngestionService.getById(id));
    }

    @PostMapping("/channel-orders/{id}/convert")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    public ApiResponse<ChannelOrderResponse> convert(@PathVariable UUID id) {
        return ApiResponse.of(orderIngestionService.convertToSalesOrder(id));
    }

    @GetMapping("/integration-jobs")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ResponseEntity<PageResponse<IntegrationJobResponse>> listJobs(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(jobService.list(organizationId, pageable)));
    }

    @PostMapping("/integration-jobs")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IntegrationJobResponse> enqueue(@Valid @RequestBody IntegrationJobCreateRequest request) {
        return ApiResponse.of(jobService.enqueue(request));
    }

    @GetMapping("/integration-jobs/{id}")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ApiResponse<IntegrationJobResponse> getJob(@PathVariable UUID id) {
        return ApiResponse.of(jobService.getById(id));
    }
}
