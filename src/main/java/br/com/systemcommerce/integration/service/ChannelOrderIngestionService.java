package br.com.systemcommerce.integration.service;

import br.com.systemcommerce.integration.adapter.MarketplaceAdapter;
import br.com.systemcommerce.integration.dto.ChannelOrderItemResponse;
import br.com.systemcommerce.integration.dto.ChannelOrderResponse;
import br.com.systemcommerce.integration.entity.ChannelOrder;
import br.com.systemcommerce.integration.entity.ChannelOrderItem;
import br.com.systemcommerce.integration.entity.ChannelOrderStatus;
import br.com.systemcommerce.integration.entity.ChannelProduct;
import br.com.systemcommerce.integration.entity.MarketplaceAccount;
import br.com.systemcommerce.integration.repository.ChannelOrderRepository;
import br.com.systemcommerce.integration.repository.ChannelProductRepository;
import br.com.systemcommerce.salesorder.dto.SalesOrderCreateRequest;
import br.com.systemcommerce.salesorder.dto.SalesOrderItemRequest;
import br.com.systemcommerce.salesorder.dto.SalesOrderResponse;
import br.com.systemcommerce.salesorder.repository.SalesOrderRepository;
import br.com.systemcommerce.salesorder.service.SalesOrderService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Ingestão idempotente de pedidos externos → ChannelOrder → SalesOrder (Prompt 80).
 */
@Service
@RequiredArgsConstructor
public class ChannelOrderIngestionService {

    private final ChannelOrderRepository channelOrderRepository;
    private final ChannelProductRepository channelProductRepository;
    private final SalesOrderService salesOrderService;
    private final SalesOrderRepository salesOrderRepository;
    private final IntegrationHubService integrationHubService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<ChannelOrderResponse> list(UUID accountId, Pageable pageable) {
        return channelOrderRepository
                .findAll(
                        (root, q, cb) ->
                                accountId == null
                                        ? cb.conjunction()
                                        : cb.equal(root.get("marketplaceAccount").get("id"), accountId),
                        pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ChannelOrderResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public ChannelOrderResponse ingestExternalOrder(
            UUID marketplaceAccountId, MarketplaceAdapter.ExternalOrder external, String idempotencyKey) {
        MarketplaceAccount account = integrationHubService.getAccountEntity(marketplaceAccountId);

        if (StringUtils.hasText(idempotencyKey)) {
            var byKey = channelOrderRepository.findByMarketplaceAccountIdAndIdempotencyKey(
                    account.getId(), idempotencyKey.trim());
            if (byKey.isPresent()) {
                return toResponse(byKey.get());
            }
        }
        var existing = channelOrderRepository.findByMarketplaceAccountIdAndExternalOrderId(
                account.getId(), external.externalOrderId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        ChannelOrder order = new ChannelOrder();
        order.setOrganization(account.getOrganization());
        order.setMarketplaceAccount(account);
        order.setExternalOrderId(external.externalOrderId());
        order.setExternalStatus(external.externalStatus());
        order.setStatus(ChannelOrderStatus.RECEIVED);
        order.setBuyerExternalId(external.buyerExternalId());
        order.setBuyerName(external.buyerName());
        order.setCurrency(StringUtils.hasText(external.currency()) ? external.currency() : "BRL");
        order.setTotalAmount(external.totalAmount() != null ? external.totalAmount() : BigDecimal.ZERO);
        order.setRawPayloadJson(external.rawPayloadJson());
        order.setIdempotencyKey(StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : null);
        order.setReceivedAt(Instant.now());

        for (MarketplaceAdapter.ExternalOrderItem extItem : external.items()) {
            ChannelOrderItem item = new ChannelOrderItem();
            item.setChannelOrder(order);
            item.setExternalItemId(extItem.externalItemId());
            item.setExternalSku(extItem.externalSku());
            item.setTitle(extItem.title());
            item.setQuantity(extItem.quantity());
            item.setUnitPrice(extItem.unitPrice() != null ? extItem.unitPrice() : BigDecimal.ZERO);
            item.setLineTotal(item.getQuantity().multiply(item.getUnitPrice()));
            resolveProduct(account.getId(), extItem).ifPresent(cp -> {
                item.setChannelProduct(cp);
                item.setProduct(cp.getProduct());
            });
            order.getItems().add(item);
        }
        order.setStatus(ChannelOrderStatus.MAPPED);
        ChannelOrder saved = channelOrderRepository.save(order);
        domainAuditService.record(
                "ChannelOrder",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("externalOrderId", saved.getExternalOrderId()),
                "Pedido de canal recebido");
        return toResponse(saved);
    }

    @Transactional
    public ChannelOrderResponse convertToSalesOrder(UUID channelOrderId) {
        ChannelOrder order = getEntity(channelOrderId);
        if (order.getSalesOrder() != null) {
            return toResponse(order);
        }
        if (order.getStatus() == ChannelOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Pedido de canal cancelado não pode ser convertido");
        }
        MarketplaceAccount account = order.getMarketplaceAccount();
        List<SalesOrderItemRequest> items = new ArrayList<>();
        for (ChannelOrderItem line : order.getItems()) {
            if (line.getProduct() == null) {
                throw new BusinessRuleException(
                        "Item sem produto interno vinculado: " + line.getExternalSku());
            }
            items.add(new SalesOrderItemRequest(
                    line.getProduct().getId(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    BigDecimal.ZERO,
                    line.getTitle()));
        }
        if (items.isEmpty()) {
            throw new BusinessRuleException("Pedido de canal sem itens mapeados");
        }
        SalesOrderResponse so = salesOrderService.createFromIntegration(new SalesOrderCreateRequest(
                account.getStore().getId(),
                account.getWarehouse().getId(),
                null,
                null,
                null,
                null,
                "Canal " + account.getAdapterCode() + " / " + order.getExternalOrderId(),
                Boolean.TRUE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                items));
        order.setSalesOrder(salesOrderRepository
                .findById(so.id())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de venda não encontrado")));
        order.setStatus(ChannelOrderStatus.CONVERTED);
        order.setConvertedAt(Instant.now());
        channelOrderRepository.save(order);
        domainAuditService.record(
                "ChannelOrder",
                order.getId(),
                AuditLog.AuditAction.UPDATE,
                Map.of("status", ChannelOrderStatus.MAPPED.name()),
                Map.of("status", ChannelOrderStatus.CONVERTED.name(), "salesOrderId", so.id().toString()),
                "Pedido de canal convertido em pedido de venda");
        return toResponse(order);
    }

    private java.util.Optional<ChannelProduct> resolveProduct(
            UUID accountId, MarketplaceAdapter.ExternalOrderItem extItem) {
        if (StringUtils.hasText(extItem.externalProductId())) {
            var byId = channelProductRepository.findByMarketplaceAccountIdAndExternalProductId(
                    accountId, extItem.externalProductId());
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (StringUtils.hasText(extItem.externalSku())) {
            return channelProductRepository.findByMarketplaceAccountIdAndExternalSku(
                    accountId, extItem.externalSku());
        }
        return java.util.Optional.empty();
    }

    private ChannelOrder getEntity(UUID id) {
        return channelOrderRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de canal não encontrado"));
    }

    private ChannelOrderResponse toResponse(ChannelOrder o) {
        return new ChannelOrderResponse(
                o.getId(),
                o.getMarketplaceAccount().getId(),
                o.getSalesOrder() != null ? o.getSalesOrder().getId() : null,
                o.getExternalOrderId(),
                o.getExternalStatus(),
                o.getStatus(),
                o.getBuyerName(),
                o.getTotalAmount(),
                o.getCurrency(),
                o.getReceivedAt(),
                o.getConvertedAt(),
                o.getItems().stream()
                        .map(i -> new ChannelOrderItemResponse(
                                i.getId(),
                                i.getProduct() != null ? i.getProduct().getId() : null,
                                i.getExternalSku(),
                                i.getTitle(),
                                i.getQuantity(),
                                i.getUnitPrice(),
                                i.getLineTotal()))
                        .toList());
    }
}
