package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.pos.audit.PosAuditContext;
import br.com.systemcommerce.pos.audit.PosAuditContexts;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditOutcome;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pricing.dto.DiscountAuthorizationDecisionRequest;
import br.com.systemcommerce.pricing.dto.DiscountAuthorizationRequest;
import br.com.systemcommerce.pricing.dto.DiscountAuthorizationResponse;
import br.com.systemcommerce.pricing.entity.DiscountAuthorization;
import br.com.systemcommerce.pricing.mapper.DiscountAuthorizationMapper;
import br.com.systemcommerce.pricing.repository.DiscountAuthorizationRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.sale.repository.SaleItemRepository;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiscountAuthorizationService {

    private final DiscountAuthorizationRepository discountAuthorizationRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final UserRepository userRepository;
    private final DiscountAuthorizationMapper discountAuthorizationMapper;
    private final DomainAuditService domainAuditService;
    private final PosAuditService posAuditService;

    @Transactional
    public DiscountAuthorizationResponse request(DiscountAuthorizationRequest request) {
        Sale sale = saleRepository
                .findById(request.saleId())
                .orElseThrow(() -> new ResourceNotFoundException("Venda", request.saleId()));
        SaleItem saleItem = null;
        if (request.saleItemId() != null) {
            saleItem = saleItemRepository
                    .findByIdAndSaleId(request.saleItemId(), request.saleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item da venda", request.saleItemId()));
        }
        User requester = userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));

        BigDecimal amount = MoneyAndQuantityUtils.money(request.requestedAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Valor solicitado deve ser maior que zero");
        }

        BigDecimal base = saleItem != null
                ? (saleItem.getLineSubtotal() != null ? saleItem.getLineSubtotal() : BigDecimal.ZERO)
                : (sale.getSubtotal() != null ? sale.getSubtotal() : BigDecimal.ZERO);
        BigDecimal percent = base.compareTo(BigDecimal.ZERO) > 0
                ? amount.multiply(new BigDecimal("100")).divide(base, 4, RoundingMode.HALF_UP)
                : null;

        DiscountAuthorization auth = new DiscountAuthorization();
        auth.setSale(sale);
        auth.setSaleItem(saleItem);
        auth.setRequestedAmount(amount);
        auth.setRequestedPercent(percent);
        auth.setStatus(DiscountAuthorization.Status.PENDING);
        auth.setRequestReason(MoneyAndQuantityUtils.blankToNull(request.reason()));
        auth.setRequestedBy(requester);

        DiscountAuthorization saved = discountAuthorizationRepository.save(auth);
        domainAuditService.record(
                "PRICING",
                "DiscountAuthorization",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Solicitação de autorização de desconto criada");
        posAuditService.success(
                PosAuditEventCode.DISCOUNT_REQUESTED,
                PosAuditContexts.fromSale(sale)
                        .entity("DiscountAuthorization", saved.getId())
                        .action(AuditLog.AuditAction.CREATE)
                        .after(snapshot(saved))
                        .details("Solicitação de autorização de desconto criada")
                        .build());
        return discountAuthorizationMapper.toResponse(getDetailed(saved.getId()));
    }

    @Transactional
    public DiscountAuthorizationResponse approve(UUID id, DiscountAuthorizationDecisionRequest request) {
        return decide(id, DiscountAuthorization.Status.APPROVED, request);
    }

    @Transactional
    public DiscountAuthorizationResponse deny(UUID id, DiscountAuthorizationDecisionRequest request) {
        return decide(id, DiscountAuthorization.Status.DENIED, request);
    }

    @Transactional(readOnly = true)
    public DiscountAuthorizationResponse getById(UUID id) {
        return discountAuthorizationMapper.toResponse(getDetailed(id));
    }

    private DiscountAuthorizationResponse decide(
            UUID id, DiscountAuthorization.Status decision, DiscountAuthorizationDecisionRequest request) {
        DiscountAuthorization auth = getDetailed(id);
        if (auth.getStatus() != DiscountAuthorization.Status.PENDING) {
            throw new BusinessRuleException("Somente solicitações PENDING podem ser decididas");
        }
        User decider = userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));

        Map<String, Object> before = snapshot(auth);
        auth.setStatus(decision);
        auth.setDecisionNotes(
                request != null ? MoneyAndQuantityUtils.blankToNull(request.decisionNotes()) : null);
        auth.setDecidedBy(decider);
        auth.setDecidedAt(Instant.now());
        DiscountAuthorization saved = discountAuthorizationRepository.save(auth);
        domainAuditService.record(
                "PRICING",
                "DiscountAuthorization",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                decision == DiscountAuthorization.Status.APPROVED
                        ? "Autorização de desconto aprovada"
                        : "Autorização de desconto negada");
        Sale sale = saved.getSale();
        PosAuditEventCode code = decision == DiscountAuthorization.Status.APPROVED
                ? PosAuditEventCode.DISCOUNT_APPROVED
                : PosAuditEventCode.DISCOUNT_DENIED;
        PosAuditOutcome outcome = decision == DiscountAuthorization.Status.APPROVED
                ? PosAuditOutcome.SUCCESS
                : PosAuditOutcome.DENIED;
        posAuditService.record(
                code,
                outcome,
                PosAuditContexts.fromSale(sale)
                        .authorizedById(decider.getId())
                        .entity("DiscountAuthorization", id)
                        .action(AuditLog.AuditAction.UPDATE)
                        .before(before)
                        .after(snapshot(saved))
                        .details(
                                decision == DiscountAuthorization.Status.APPROVED
                                        ? "Autorização de desconto aprovada"
                                        : "Autorização de desconto negada")
                        .errorCode(
                                decision == DiscountAuthorization.Status.DENIED ? "DISCOUNT_DENIED" : null)
                        .build());
        return discountAuthorizationMapper.toResponse(getDetailed(id));
    }

    private DiscountAuthorization getDetailed(UUID id) {
        return discountAuthorizationRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autorização de desconto", id));
    }

    private Map<String, Object> snapshot(DiscountAuthorization auth) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", auth.getId());
        map.put("saleId", auth.getSale() != null ? auth.getSale().getId() : null);
        map.put("saleItemId", auth.getSaleItem() != null ? auth.getSaleItem().getId() : null);
        map.put("requestedAmount", auth.getRequestedAmount());
        map.put("requestedPercent", auth.getRequestedPercent());
        map.put("status", auth.getStatus());
        map.put("requestedById", auth.getRequestedBy() != null ? auth.getRequestedBy().getId() : null);
        map.put("decidedById", auth.getDecidedBy() != null ? auth.getDecidedBy().getId() : null);
        map.put("decidedAt", auth.getDecidedAt());
        return map;
    }
}
