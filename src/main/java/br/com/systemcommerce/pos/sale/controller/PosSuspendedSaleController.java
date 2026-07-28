package br.com.systemcommerce.pos.sale.controller;

import br.com.systemcommerce.pos.sale.dto.SuspendedSaleClaimRequest;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleDiscardRequest;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleExpirationResponse;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleResponse;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleResumeRequest;
import br.com.systemcommerce.pos.sale.service.PosSuspendedSaleService;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/suspended-sales")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "POS Suspended Sales",
        description =
                """
                Vendas suspensas do PDV: listagem, recuperação, bloqueio concorrente e descarte auditado. \
                Suspensão não conclui pagamento nem baixa estoque definitivamente.
                """)
public class PosSuspendedSaleController {

    private final PosSuspendedSaleService posSuspendedSaleService;

    @GetMapping
    @PreAuthorize("hasAuthority('POS_SUSPENDED_SALE_READ') or hasAuthority('POS_SALE_SUSPEND')")
    @Operation(summary = "Lista vendas suspensas")
    public ResponseEntity<PageResponse<SuspendedSaleResponse>> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String saleNumber,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String customerQuery,
            @RequestParam(required = false) Boolean includeExpired,
            @PageableDefault(size = 20, sort = "suspendedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(posSuspendedSaleService.list(
                storeId, saleNumber, customerId, customerQuery, includeExpired, pageable)));
    }

    @GetMapping("/by-number")
    @PreAuthorize("hasAuthority('POS_SUSPENDED_SALE_READ') or hasAuthority('POS_SALE_SUSPEND')")
    @Operation(summary = "Pesquisa suspensas por número da venda")
    public ResponseEntity<PageResponse<SuspendedSaleResponse>> byNumber(
            @RequestParam String saleNumber,
            @RequestParam(required = false) UUID storeId,
            @PageableDefault(size = 20, sort = "suspendedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                posSuspendedSaleService.list(storeId, saleNumber, null, null, false, pageable)));
    }

    @GetMapping("/by-customer")
    @PreAuthorize("hasAuthority('POS_SUSPENDED_SALE_READ') or hasAuthority('POS_SALE_SUSPEND')")
    @Operation(summary = "Pesquisa suspensas por cliente")
    public ResponseEntity<PageResponse<SuspendedSaleResponse>> byCustomer(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID storeId,
            @PageableDefault(size = 20, sort = "suspendedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                posSuspendedSaleService.list(storeId, null, customerId, q, false, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('POS_SUSPENDED_SALE_READ') or hasAuthority('POS_SALE_SUSPEND')")
    @Operation(summary = "Detalhe de venda suspensa")
    public ResponseEntity<ApiResponse<SuspendedSaleResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(posSuspendedSaleService.getById(id)));
    }

    @GetMapping("/{id}/expiration")
    @PreAuthorize("hasAuthority('POS_SUSPENDED_SALE_READ') or hasAuthority('POS_SALE_SUSPEND')")
    @Operation(summary = "Consulta expiração da venda suspensa")
    public ResponseEntity<ApiResponse<SuspendedSaleExpirationResponse>> expiration(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(posSuspendedSaleService.expiration(id)));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize(
            "hasAuthority('POS_SUSPENDED_SALE_RESUME') or hasAuthority('POS_SALE_SUSPEND') or hasAuthority('POS_SUSPENDED_SALE_RESUME_OTHER_OPERATOR')")
    @Operation(summary = "Recupera venda suspensa (com bloqueio de edição)")
    public ResponseEntity<ApiResponse<SaleResponse>> resume(
            @PathVariable UUID id,
            @Valid @RequestBody SuspendedSaleResumeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(posSuspendedSaleService.resume(id, request, idempotencyKey)));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('POS_SUSPENDED_SALE_RESUME_OTHER_OPERATOR')")
    @Operation(summary = "Assume venda (troca operador + bloqueio)")
    public ResponseEntity<ApiResponse<SaleResponse>> claim(
            @PathVariable UUID id,
            @Valid @RequestBody SuspendedSaleClaimRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(posSuspendedSaleService.claim(id, request, idempotencyKey)));
    }

    @PostMapping("/{id}/release-lock")
    @PreAuthorize("hasAuthority('POS_SUSPENDED_SALE_RESUME_OTHER_OPERATOR')")
    @Operation(summary = "Libera bloqueio de edição (administrativo)")
    public ResponseEntity<ApiResponse<SaleResponse>> releaseLock(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(posSuspendedSaleService.releaseLock(id, idempotencyKey)));
    }

    @PostMapping("/{id}/discard")
    @PreAuthorize("hasAuthority('POS_SUSPENDED_SALE_DISCARD') or hasAuthority('POS_SALE_CANCEL')")
    @Operation(summary = "Descarta venda suspensa (auditado)")
    public ResponseEntity<ApiResponse<SaleResponse>> discard(
            @PathVariable UUID id,
            @Valid @RequestBody SuspendedSaleDiscardRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.of(posSuspendedSaleService.discard(id, request, idempotencyKey)));
    }
}
