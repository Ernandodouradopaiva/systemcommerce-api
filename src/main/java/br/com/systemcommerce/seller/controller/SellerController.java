package br.com.systemcommerce.seller.controller;

import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.mapper.SaleMapper;
import br.com.systemcommerce.seller.dto.SellerDiscountLimitResponse;
import br.com.systemcommerce.seller.dto.SellerEnableRequest;
import br.com.systemcommerce.seller.dto.SellerResponse;
import br.com.systemcommerce.seller.dto.SellerStoreAssignmentResponse;
import br.com.systemcommerce.seller.dto.SellerStoreAuthorizeRequest;
import br.com.systemcommerce.seller.dto.SellerUpdateRequest;
import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.seller.service.SellerService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.storecontext.GlobalStoreOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Sellers", description = "Cadastro e autorização comercial de vendedores")
@GlobalStoreOperation
public class SellerController {

    private final SellerService sellerService;
    private final SaleMapper saleMapper;

    @PostMapping("/enable")
    @PreAuthorize("hasAnyAuthority('SELLER_CREATE','SALESPERSON_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Habilita profissional como vendedor")
    public ResponseEntity<ApiResponse<SellerResponse>> enable(@Valid @RequestBody SellerEnableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(sellerService.enable(request)));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyAuthority('SELLER_UPDATE','SALESPERSON_UPDATE','SALESPERSON_DELETE')")
    @Operation(summary = "Desabilita vendedor")
    public ResponseEntity<ApiResponse<SellerResponse>> disable(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(sellerService.disable(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SELLER_UPDATE','SALESPERSON_UPDATE')")
    @Operation(summary = "Atualiza perfil de vendedor")
    public ResponseEntity<ApiResponse<SellerResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody SellerUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(sellerService.update(id, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SELLER_READ','SALESPERSON_READ')")
    @Operation(summary = "Consulta vendedor")
    public ResponseEntity<ApiResponse<SellerResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(sellerService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SELLER_READ','SALESPERSON_READ')")
    @Operation(summary = "Lista vendedores")
    public ResponseEntity<PageResponse<SellerResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) SellerProfile.SellerStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(sellerService.list(organizationId, status, search, pageable)));
    }

    @GetMapping("/by-store/{storeId}")
    @PreAuthorize("hasAnyAuthority('SELLER_READ','SALESPERSON_READ')")
    @Operation(summary = "Lista vendedores autorizados na loja")
    public ResponseEntity<ApiResponse<List<SellerResponse>>> listByStore(@PathVariable UUID storeId) {
        return ResponseEntity.ok(ApiResponse.of(sellerService.listByStore(storeId)));
    }

    @PostMapping("/{id}/stores")
    @PreAuthorize("hasAuthority('SELLER_ASSIGN_STORE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Autoriza vendedor em loja")
    public ResponseEntity<ApiResponse<SellerStoreAssignmentResponse>> authorize(
            @PathVariable UUID id, @Valid @RequestBody SellerStoreAuthorizeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(sellerService.authorizeStore(id, request)));
    }

    @PostMapping("/{id}/stores/{assignmentId}/revoke")
    @PreAuthorize("hasAuthority('SELLER_ASSIGN_STORE')")
    @Operation(summary = "Remove autorização comercial (histórico preservado)")
    public ResponseEntity<ApiResponse<SellerStoreAssignmentResponse>> revoke(
            @PathVariable UUID id, @PathVariable UUID assignmentId) {
        return ResponseEntity.ok(ApiResponse.of(sellerService.revokeStoreAuthorization(id, assignmentId)));
    }

    @GetMapping("/{id}/stores")
    @PreAuthorize("hasAnyAuthority('SELLER_READ','SALESPERSON_READ')")
    @Operation(summary = "Consulta lojas permitidas (vigentes)")
    public ResponseEntity<ApiResponse<List<SellerStoreAssignmentResponse>>> allowedStores(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(sellerService.listAllowedStores(id)));
    }

    @GetMapping("/{id}/stores/history")
    @PreAuthorize("hasAnyAuthority('SELLER_READ','SALESPERSON_READ')")
    @Operation(summary = "Histórico de autorizações comerciais")
    public ResponseEntity<ApiResponse<List<SellerStoreAssignmentResponse>>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(sellerService.listHistory(id)));
    }

    @GetMapping("/{id}/discount-limit")
    @PreAuthorize("hasAnyAuthority('SELLER_READ','SALESPERSON_READ')")
    @Operation(summary = "Consulta limite de desconto efetivo (API)")
    public ResponseEntity<ApiResponse<SellerDiscountLimitResponse>> discountLimit(
            @PathVariable UUID id, @RequestParam(required = false) UUID storeId) {
        return ResponseEntity.ok(ApiResponse.of(sellerService.getDiscountLimit(id, storeId)));
    }

    @GetMapping("/{id}/sales")
    @PreAuthorize("hasAuthority('SELLER_VIEW_PERFORMANCE') or hasAnyAuthority('SELLER_READ','SALESPERSON_READ')")
    @Operation(summary = "Consulta vendas do vendedor (seller_profile preservado na venda)")
    public ResponseEntity<PageResponse<SaleResponse>> sales(
            @PathVariable UUID id, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                sellerService.listSales(id, pageable).map(s -> saleMapper.toResponse(s, List.of()))));
    }
}
