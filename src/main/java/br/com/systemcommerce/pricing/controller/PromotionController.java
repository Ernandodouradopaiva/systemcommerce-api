package br.com.systemcommerce.pricing.controller;

import br.com.systemcommerce.pricing.dto.ApplicablePriceResponse;
import br.com.systemcommerce.pricing.dto.PromotionBenefitRequest;
import br.com.systemcommerce.pricing.dto.PromotionBenefitResponse;
import br.com.systemcommerce.pricing.dto.PromotionCartContextRequest;
import br.com.systemcommerce.pricing.dto.PromotionConditionRequest;
import br.com.systemcommerce.pricing.dto.PromotionConditionResponse;
import br.com.systemcommerce.pricing.dto.PromotionCreateRequest;
import br.com.systemcommerce.pricing.dto.PromotionEngineResultResponse;
import br.com.systemcommerce.pricing.dto.PromotionProductLinkRequest;
import br.com.systemcommerce.pricing.dto.PromotionProductResponse;
import br.com.systemcommerce.pricing.dto.PromotionResponse;
import br.com.systemcommerce.pricing.dto.PromotionRuleRequest;
import br.com.systemcommerce.pricing.dto.PromotionRuleResponse;
import br.com.systemcommerce.pricing.dto.PromotionStoreLinkRequest;
import br.com.systemcommerce.pricing.dto.PromotionUpdateRequest;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.service.PromotionEngineService;
import br.com.systemcommerce.pricing.service.PromotionService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Promotions", description = "Promoções por loja e canal")
public class PromotionController {

    private final PromotionService promotionService;
    private final PromotionEngineService promotionEngineService;

    @GetMapping
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE') or hasAuthority('PRICE_TABLE_READ')")
    @Operation(summary = "Lista promoções")
    public ResponseEntity<PageResponse<PromotionResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(promotionService.list(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE') or hasAuthority('PRICE_TABLE_READ')")
    @Operation(summary = "Consulta promoção por ID")
    public ResponseEntity<ApiResponse<PromotionResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(promotionService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria promoção")
    public ResponseEntity<ApiResponse<PromotionResponse>> create(@Valid @RequestBody PromotionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(promotionService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @Operation(summary = "Atualiza promoção")
    public ResponseEntity<ApiResponse<PromotionResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody PromotionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(promotionService.update(id, request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @Operation(summary = "Ativa promoção")
    public ResponseEntity<ApiResponse<PromotionResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(promotionService.activate(id)));
    }

    @PostMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @Operation(summary = "Inativa promoção")
    public ResponseEntity<ApiResponse<PromotionResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(promotionService.inactivate(id)));
    }

    @PostMapping("/{id}/stores")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @Operation(summary = "Vincula loja à promoção")
    public ResponseEntity<ApiResponse<PromotionResponse>> linkStore(
            @PathVariable UUID id, @Valid @RequestBody PromotionStoreLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.of(promotionService.linkStore(id, request)));
    }

    @GetMapping("/{id}/products")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE') or hasAuthority('PRICE_TABLE_READ')")
    @Operation(summary = "Lista produtos da promoção")
    public ResponseEntity<ApiResponse<List<PromotionProductResponse>>> listProducts(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(promotionService.listProducts(id)));
    }

    @PostMapping("/{id}/products")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona produto à promoção")
    public ResponseEntity<ApiResponse<PromotionProductResponse>> addProduct(
            @PathVariable UUID id, @Valid @RequestBody PromotionProductLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(promotionService.addProduct(id, request)));
    }

    @GetMapping("/applicable")
    @PreAuthorize(
            "hasAuthority('PROMOTION_MANAGE') or hasAuthority('PRICE_TABLE_READ') or hasAuthority('POS_SALE_CREATE')")
    @Operation(summary = "Resolve preço promocional aplicável (mesma resolução oficial)")
    public ResponseEntity<ApiResponse<ApplicablePriceResponse>> applicable(
            @RequestParam UUID productId,
            @RequestParam UUID storeId,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam(required = false) PriceChannel channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        return ResponseEntity.ok(ApiResponse.of(
                promotionService.getApplicable(productId, storeId, quantity, at, channel != null ? channel : PriceChannel.POS)));
    }

    @GetMapping("/{id}/rules")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE') or hasAuthority('PRICE_TABLE_READ')")
    @Operation(summary = "Lista regras do motor de promoção")
    public ResponseEntity<ApiResponse<List<PromotionRuleResponse>>> listRules(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(promotionService.listRules(id)));
    }

    @PostMapping("/{id}/rules")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria regra do motor de promoção")
    public ResponseEntity<ApiResponse<PromotionRuleResponse>> addRule(
            @PathVariable UUID id, @Valid @RequestBody PromotionRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(promotionService.addRule(id, request)));
    }

    @DeleteMapping("/{id}/rules/{ruleId}")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @Operation(summary = "Remove regra do motor de promoção")
    public ResponseEntity<Void> removeRule(@PathVariable UUID id, @PathVariable UUID ruleId) {
        promotionService.removeRule(id, ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/conditions")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE') or hasAuthority('PRICE_TABLE_READ')")
    @Operation(summary = "Lista condições do motor de promoção")
    public ResponseEntity<ApiResponse<List<PromotionConditionResponse>>> listConditions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(promotionService.listConditions(id)));
    }

    @PostMapping("/{id}/conditions")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria condição do motor de promoção")
    public ResponseEntity<ApiResponse<PromotionConditionResponse>> addCondition(
            @PathVariable UUID id, @Valid @RequestBody PromotionConditionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(promotionService.addCondition(id, request)));
    }

    @DeleteMapping("/{id}/conditions/{conditionId}")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @Operation(summary = "Remove condição do motor de promoção")
    public ResponseEntity<Void> removeCondition(@PathVariable UUID id, @PathVariable UUID conditionId) {
        promotionService.removeCondition(id, conditionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/benefits")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE') or hasAuthority('PRICE_TABLE_READ')")
    @Operation(summary = "Lista benefícios do motor de promoção")
    public ResponseEntity<ApiResponse<List<PromotionBenefitResponse>>> listBenefits(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(promotionService.listBenefits(id)));
    }

    @PostMapping("/{id}/benefits")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria benefício do motor de promoção")
    public ResponseEntity<ApiResponse<PromotionBenefitResponse>> addBenefit(
            @PathVariable UUID id, @Valid @RequestBody PromotionBenefitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(promotionService.addBenefit(id, request)));
    }

    @DeleteMapping("/{id}/benefits/{benefitId}")
    @PreAuthorize("hasAuthority('PROMOTION_MANAGE')")
    @Operation(summary = "Remove benefício do motor de promoção")
    public ResponseEntity<Void> removeBenefit(@PathVariable UUID id, @PathVariable UUID benefitId) {
        promotionService.removeBenefit(id, benefitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/engine/apply")
    @PreAuthorize(
            "hasAuthority('PROMOTION_MANAGE') or hasAuthority('PRICE_TABLE_READ') or hasAuthority('POS_SALE_CREATE')")
    @Operation(
            summary = "Simula a aplicação do motor de promoções sobre um carrinho",
            description = "Calcula o desconto oficial (percentual/fixo/preço promo/compre-X-pague-Y), respeitando "
                    + "prioridade, empilhamento (stackable) e teto de benefício. Não persiste nada; é a fonte "
                    + "oficial de cálculo para o front exibir o total com desconto.")
    public ResponseEntity<ApiResponse<PromotionEngineResultResponse>> applyEngine(
            @Valid @RequestBody PromotionCartContextRequest request) {
        return ResponseEntity.ok(ApiResponse.of(promotionEngineService.apply(request)));
    }
}
