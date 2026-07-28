package br.com.systemcommerce.pos.cancellation.service;

import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.cancellation.dto.SaleReturnCreateRequest;
import br.com.systemcommerce.pos.cancellation.dto.SaleReturnResponse;
import br.com.systemcommerce.pos.cancellation.entity.SaleReturn;
import br.com.systemcommerce.pos.cancellation.entity.SaleReturnItem;
import br.com.systemcommerce.pos.cancellation.mapper.PosCancellationMapper;
import br.com.systemcommerce.pos.cancellation.repository.SaleReturnItemRepository;
import br.com.systemcommerce.pos.cancellation.repository.SaleReturnRepository;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PosReturnService {

    private final SaleReturnRepository saleReturnRepository;
    private final SaleReturnItemRepository saleReturnItemRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CashSessionRepository cashSessionRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final UserRepository userRepository;
    private final PosCancellationMapper mapper;
    private final DomainAuditService domainAuditService;

    @Transactional
    public SaleReturnResponse register(SaleReturnCreateRequest request, String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = saleReturnRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return mapper.toReturnResponse(requireDetailed(existing.get().getId()));
            }
        }

        String reason = MoneyAndQuantityUtils.requireText(request.reason(), "Motivo da devolução");
        Sale original = saleRepository
                .findById(request.originalSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Venda", request.originalSaleId()));
        if (!original.isPos()) {
            throw new BusinessRuleException("Devolução PDV exige venda do canal POS");
        }
        if (!original.isConfirmedLike() && !original.isCancelled()) {
            throw new BusinessRuleException(
                    "Devolução futura exige venda concluída (ou já cancelada com documento próprio)");
        }

        CashSession session = null;
        if (request.cashSessionId() != null) {
            session = cashSessionRepository
                    .findById(request.cashSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sessão de caixa", request.cashSessionId()));
            if (!session.acceptsOperations()) {
                throw new BusinessRuleException("Sessão de caixa deve estar aberta para registrar devolução");
            }
        }

        User requester = userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));

        SaleReturn saleReturn = new SaleReturn();
        saleReturn.setReturnNumber(nextReturnNumber());
        saleReturn.setOriginalSale(original);
        saleReturn.setCashSession(session);
        saleReturn.setStatus(SaleReturn.Status.CONFIRMED);
        saleReturn.setReason(reason);
        saleReturn.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        saleReturn.setRequestedBy(requester);
        saleReturn.setConfirmedAt(Instant.now());
        if (StringUtils.hasText(idempotencyKey)) {
            saleReturn.setIdempotencyKey(idempotencyKey.trim());
        }
        SaleReturn saved = saleReturnRepository.saveAndFlush(saleReturn);

        UUID warehouseId = original.getWarehouse() != null ? original.getWarehouse().getId() : null;
        List<SaleReturnItem> items = new ArrayList<>();
        for (SaleReturnCreateRequest.SaleReturnItemRequest line : request.items()) {
            Product product = productRepository
                    .findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", line.productId()));
            BigDecimal qty = MoneyAndQuantityUtils.quantity(line.quantity());
            SaleItem originalItem = null;
            BigDecimal unitPrice = product.getSalePrice();
            if (line.originalSaleItemId() != null) {
                originalItem = saleItemRepository
                        .findByIdAndSaleId(line.originalSaleItemId(), original.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Item da venda", line.originalSaleItemId()));
                if (!originalItem.getProduct().getId().equals(product.getId())) {
                    throw new BusinessRuleException("Produto do item de devolução diverge do item original");
                }
                unitPrice = originalItem.getUnitPrice();
            } else {
                var match = saleItemRepository.findBySaleIdAndProductId(original.getId(), product.getId());
                if (match.isPresent()) {
                    originalItem = match.get();
                    unitPrice = originalItem.getUnitPrice();
                }
            }

            BigDecimal lineTotal = qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            SaleReturnItem item = new SaleReturnItem();
            item.setSaleReturn(saved);
            item.setProduct(product);
            item.setOriginalSaleItem(originalItem);
            item.setQuantity(qty);
            item.setUnitPrice(MoneyAndQuantityUtils.money(unitPrice));
            item.setLineTotal(lineTotal);
            items.add(saleReturnItemRepository.save(item));

            inventoryService.registerEntry(new InventoryEntryRequest(
                    product.getId(), warehouseId, qty, "Devolução " + saved.getReturnNumber(), true));
        }
        saved.setItems(items);

        domainAuditService.record(
                "POS",
                "SaleReturn",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Devolução registrada: " + reason);
        return mapper.toReturnResponse(requireDetailed(saved.getId()));
    }

    @Transactional(readOnly = true)
    public SaleReturnResponse getById(UUID id) {
        return mapper.toReturnResponse(requireDetailed(id));
    }

    private String nextReturnNumber() {
        String prefix = "DEV-" + DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now(ZoneOffset.UTC));
        long count = saleReturnRepository.countByReturnNumberStartingWith(prefix) + 1;
        return prefix + "-" + String.format("%04d", count);
    }

    private SaleReturn requireDetailed(UUID id) {
        return saleReturnRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolução", id));
    }

    private Map<String, Object> snapshot(SaleReturn r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("returnNumber", r.getReturnNumber());
        map.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        map.put("originalSaleId", r.getOriginalSale() != null ? r.getOriginalSale().getId() : null);
        map.put("reason", r.getReason());
        return map;
    }
}
