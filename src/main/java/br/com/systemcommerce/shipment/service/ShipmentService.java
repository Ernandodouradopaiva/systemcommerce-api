package br.com.systemcommerce.shipment.service;

import br.com.systemcommerce.carrier.entity.Carrier;
import br.com.systemcommerce.carrier.entity.FreightMode;
import br.com.systemcommerce.carrier.repository.FreightModeRepository;
import br.com.systemcommerce.carrier.service.CarrierService;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.picking.entity.PickingOrder;
import br.com.systemcommerce.picking.repository.PickingOrderRepository;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.salesorder.repository.SalesOrderRepository;
import br.com.systemcommerce.salesorder.service.SalesOrderService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.shipment.dto.DeliveryProofRequest;
import br.com.systemcommerce.shipment.dto.ShipmentCreateRequest;
import br.com.systemcommerce.shipment.dto.ShipmentItemRequest;
import br.com.systemcommerce.shipment.dto.ShipmentPackageRequest;
import br.com.systemcommerce.shipment.dto.ShipmentResponse;
import br.com.systemcommerce.shipment.dto.ShipmentTrackingRequest;
import br.com.systemcommerce.shipment.entity.DeliveryEvent;
import br.com.systemcommerce.shipment.entity.DeliveryProof;
import br.com.systemcommerce.shipment.entity.Shipment;
import br.com.systemcommerce.shipment.entity.ShipmentItem;
import br.com.systemcommerce.shipment.entity.ShipmentPackage;
import br.com.systemcommerce.shipment.entity.ShipmentTracking;
import br.com.systemcommerce.shipment.mapper.ShipmentMapper;
import br.com.systemcommerce.shipment.repository.ShipmentRepository;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Expedição/entrega (Prompt 72). A entrega não altera estoque — a baixa física ocorre no
 * faturamento ({@code SalesOrderService.invoice}). Expedições podem ser parciais em relação ao
 * pedido de venda.
 */
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private static final Set<Shipment.ShipmentStatus> TRACKABLE_TRANSITIONS = EnumSet.of(
            Shipment.ShipmentStatus.DISPATCHED,
            Shipment.ShipmentStatus.IN_TRANSIT,
            Shipment.ShipmentStatus.OUT_FOR_DELIVERY,
            Shipment.ShipmentStatus.DELIVERY_FAILED,
            Shipment.ShipmentStatus.RETURNING,
            Shipment.ShipmentStatus.RETURNED);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;
    private final StoreShipmentSequenceService storeShipmentSequenceService;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderService salesOrderService;
    private final PickingOrderRepository pickingOrderRepository;
    private final WarehouseService warehouseService;
    private final ProductService productService;
    private final CarrierService carrierService;
    private final FreightModeRepository freightModeRepository;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final DomainAuditService domainAuditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<ShipmentResponse> list(
            Shipment.ShipmentStatus status, UUID storeId, UUID salesOrderId, Pageable pageable) {
        return shipmentRepository
                .findAll(
                        (root, query, cb) -> {
                            var predicates = cb.conjunction();
                            if (status != null) {
                                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
                            }
                            if (storeId != null) {
                                predicates =
                                        cb.and(predicates, cb.equal(root.get("store").get("id"), storeId));
                            }
                            if (salesOrderId != null) {
                                predicates = cb.and(
                                        predicates, cb.equal(root.get("salesOrder").get("id"), salesOrderId));
                            }
                            query.distinct(true);
                            return predicates;
                        },
                        pageable)
                .map(shipmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getById(UUID id) {
        return shipmentMapper.toResponse(getAccessibleEntity(id));
    }

    @Transactional
    public ShipmentResponse createFromSalesOrder(ShipmentCreateRequest request) {
        SalesOrder order = salesOrderRepository
                .findDetailedById(request.salesOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de venda", request.salesOrderId()));
        Store store = storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), order.getStore().getId());

        Warehouse warehouse = request.warehouseId() != null
                ? warehouseService.requireUsable(request.warehouseId())
                : order.getWarehouse();
        if (warehouse == null) {
            throw new BusinessRuleException("Informe o depósito de origem da expedição");
        }

        PickingOrder pickingOrder = null;
        if (request.pickingOrderId() != null) {
            pickingOrder = pickingOrderRepository
                    .findDetailedById(request.pickingOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Separação", request.pickingOrderId()));
            if (!pickingOrder.getSalesOrder().getId().equals(order.getId())) {
                throw new BusinessRuleException("Separação não pertence ao pedido de venda informado");
            }
        }

        Shipment shipment = new Shipment();
        shipment.setOrganization(order.getOrganization());
        shipment.setStore(store);
        shipment.setWarehouse(warehouse);
        shipment.setSalesOrder(order);
        shipment.setPickingOrder(pickingOrder);
        shipment.setCustomer(order.getCustomer());
        shipment.setShipmentNumber(storeShipmentSequenceService.allocateNextShipmentNumber(store));
        shipment.setStatus(Shipment.ShipmentStatus.PENDING);
        shipment.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        shipment.setExpectedDelivery(request.expectedDelivery());
        shipment.setFreightAmount(
                request.freightAmount() != null ? MoneyAndQuantityUtils.money(request.freightAmount()) : BigDecimal.ZERO);
        shipment.setCarrierName(MoneyAndQuantityUtils.blankToNull(request.carrierName()));
        shipment.setFreightModeLabel(MoneyAndQuantityUtils.blankToNull(request.freightModeLabel()));
        if (request.carrierId() != null) {
            shipment.setCarrier(carrierService.requireUsable(request.carrierId()));
        }
        if (request.freightModeId() != null) {
            FreightMode mode = freightModeRepository
                    .findById(request.freightModeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Modalidade de frete", request.freightModeId()));
            if (!mode.isUsable()) {
                throw new BusinessRuleException("Modalidade de frete inativa não pode ser selecionada");
            }
            shipment.setFreightMode(mode);
        }
        shipment.setAddressSnapshot(buildAddressSnapshot(order.getCustomer()));

        int line = 1;
        for (ShipmentItemRequest itemRequest : request.items()) {
            Product product = productService.requireUsableForSale(itemRequest.productId());
            ShipmentItem item = new ShipmentItem();
            item.setProduct(product);
            item.setLineNumber(line++);
            item.setQuantity(MoneyAndQuantityUtils.positiveQuantity(itemRequest.quantity()));
            if (itemRequest.salesOrderItemId() != null) {
                item.setSalesOrderItem(order.getItems().stream()
                        .filter(i -> i.getId().equals(itemRequest.salesOrderItemId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessRuleException(
                                "Item de pedido informado não pertence ao pedido de venda")));
            }
            shipment.addItem(item);
        }
        shipment.setPackageCount(1);

        Shipment saved = shipmentRepository.save(shipment);
        appendDeliveryEvent(saved, DeliveryEvent.EventType.STATUS_CHANGED, "Expedição criada (PENDING)");
        domainAuditService.record(
                "LOGISTICS",
                "Shipment",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Expedição criada a partir do pedido " + order.getOrderNumber());
        return shipmentMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public ShipmentResponse startPacking(UUID id) {
        Shipment shipment = getAccessibleEntity(id);
        assertTransition(shipment, Shipment.ShipmentStatus.PACKING, Shipment.ShipmentStatus.PENDING);
        return changeStatus(shipment, Shipment.ShipmentStatus.PACKING, "Iniciada a embalagem");
    }

    @Transactional
    public ShipmentResponse markReady(UUID id) {
        Shipment shipment = getAccessibleEntity(id);
        assertTransition(shipment, Shipment.ShipmentStatus.READY, Shipment.ShipmentStatus.PACKING);
        return changeStatus(shipment, Shipment.ShipmentStatus.READY, "Expedição pronta para despacho");
    }

    @Transactional
    public ShipmentResponse addPackage(UUID id, ShipmentPackageRequest request) {
        Shipment shipment = getAccessibleEntity(id);
        if (!shipment.isOpen()) {
            throw new BusinessRuleException("Expedição não está aberta para inclusão de volumes");
        }
        ShipmentPackage pkg = new ShipmentPackage();
        pkg.setShipment(shipment);
        pkg.setPackageNumber(shipment.getPackages().size() + 1);
        pkg.setWeight(request.weight());
        pkg.setLengthCm(request.lengthCm());
        pkg.setWidthCm(request.widthCm());
        pkg.setHeightCm(request.heightCm());
        pkg.setTrackingCode(MoneyAndQuantityUtils.blankToNull(request.trackingCode()));
        shipment.getPackages().add(pkg);
        shipment.setPackageCount(shipment.getPackages().size());
        shipment.setTotalWeight(shipment.getPackages().stream()
                .map(ShipmentPackage::getWeight)
                .filter(w -> w != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        Shipment saved = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public ShipmentResponse dispatch(UUID id, String trackingCode) {
        Shipment shipment = getAccessibleEntity(id);
        assertTransition(shipment, Shipment.ShipmentStatus.DISPATCHED, Shipment.ShipmentStatus.READY);
        if (StringUtils.hasText(trackingCode)) {
            shipment.setTrackingCode(trackingCode.trim());
        }
        return changeStatus(shipment, Shipment.ShipmentStatus.DISPATCHED, "Expedição despachada");
    }

    @Transactional
    public ShipmentResponse addTrackingEvent(UUID id, ShipmentTrackingRequest request) {
        Shipment shipment = getAccessibleEntity(id);
        if (shipment.getStatus() == Shipment.ShipmentStatus.DELIVERED
                || shipment.getStatus() == Shipment.ShipmentStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Expedição finalizada (" + shipment.getStatus() + ") não aceita novos eventos de rastreio");
        }
        ShipmentTracking tracking = new ShipmentTracking();
        tracking.setShipment(shipment);
        tracking.setStatus(request.status().trim().toUpperCase());
        tracking.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        tracking.setLocationText(MoneyAndQuantityUtils.blankToNull(request.locationText()));
        tracking.setOccurredAt(Instant.now());
        shipment.getTrackingEvents().add(tracking);

        try {
            Shipment.ShipmentStatus mapped = Shipment.ShipmentStatus.valueOf(tracking.getStatus());
            if (TRACKABLE_TRANSITIONS.contains(mapped)) {
                shipment.setStatus(mapped);
            }
        } catch (IllegalArgumentException ignored) {
            /* status livre da transportadora (texto), não corresponde a um status interno */
        }
        Shipment saved = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(getEntity(saved.getId()));
    }

    /**
     * Confirma a entrega. NÃO altera estoque — a baixa física ocorreu no faturamento. Se todas as
     * expedições em aberto do pedido estiverem entregues, marca o pedido como DELIVERED.
     */
    @Transactional
    public ShipmentResponse deliver(UUID id, DeliveryProofRequest request) {
        Shipment shipment = getAccessibleEntity(id);
        if (shipment.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
            return shipmentMapper.toResponse(shipment);
        }
        if (shipment.getStatus() == Shipment.ShipmentStatus.CANCELLED) {
            throw new BusinessRuleException("Expedição cancelada não pode ser entregue");
        }
        if (request != null) {
            DeliveryProof proof = new DeliveryProof();
            proof.setShipment(shipment);
            proof.setProofType(request.proofType());
            proof.setStorageRef(MoneyAndQuantityUtils.requireText(request.storageRef(), "Referência do comprovante"));
            proof.setRecipientName(MoneyAndQuantityUtils.blankToNull(request.recipientName()));
            proof.setCapturedAt(Instant.now());
            shipment.getDeliveryProofs().add(proof);
        }
        shipment.markDelivered();
        Shipment saved = shipmentRepository.save(shipment);
        appendDeliveryEvent(saved, DeliveryEvent.EventType.DELIVERED, "Expedição entregue (estoque já baixado no faturamento)");
        domainAuditService.record(
                "LOGISTICS",
                "Shipment",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                snapshot(saved),
                "Expedição entregue");

        maybeMarkSalesOrderDelivered(saved.getSalesOrder().getId());
        return shipmentMapper.toResponse(getEntity(id));
    }

    @Transactional
    public ShipmentResponse markDeliveryFailed(UUID id, String notes) {
        Shipment shipment = getAccessibleEntity(id);
        if (!shipment.isOpen()) {
            throw new BusinessRuleException("Expedição não está em andamento");
        }
        shipment.setStatus(Shipment.ShipmentStatus.DELIVERY_FAILED);
        Shipment saved = shipmentRepository.save(shipment);
        appendDeliveryEvent(
                saved,
                DeliveryEvent.EventType.DELIVERY_FAILED,
                notes != null ? notes : "Tentativa de entrega sem sucesso");
        return shipmentMapper.toResponse(getEntity(id));
    }

    @Transactional
    public ShipmentResponse cancel(UUID id, String notes) {
        Shipment shipment = getAccessibleEntity(id);
        if (shipment.getStatus() == Shipment.ShipmentStatus.CANCELLED) {
            return shipmentMapper.toResponse(shipment);
        }
        if (shipment.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
            throw new BusinessRuleException("Expedição já entregue não pode ser cancelada");
        }
        shipment.setStatus(Shipment.ShipmentStatus.CANCELLED);
        Shipment saved = shipmentRepository.save(shipment);
        appendDeliveryEvent(
                saved, DeliveryEvent.EventType.CANCELLED, notes != null ? notes : "Expedição cancelada");
        domainAuditService.record(
                "LOGISTICS", "Shipment", id, AuditLog.AuditAction.STATUS_CHANGE, null, snapshot(saved), "Expedição cancelada");
        return shipmentMapper.toResponse(getEntity(id));
    }

    private void maybeMarkSalesOrderDelivered(UUID salesOrderId) {
        var shipments = shipmentRepository.findBySalesOrderId(salesOrderId);
        if (shipments.isEmpty()) {
            return;
        }
        boolean allSettled = shipments.stream()
                .allMatch(s -> s.getStatus() == Shipment.ShipmentStatus.DELIVERED
                        || s.getStatus() == Shipment.ShipmentStatus.CANCELLED);
        boolean anyDelivered =
                shipments.stream().anyMatch(s -> s.getStatus() == Shipment.ShipmentStatus.DELIVERED);
        if (allSettled && anyDelivered) {
            try {
                salesOrderService.deliver(salesOrderId);
            } catch (BusinessRuleException ex) {
                /* pedido pode já estar em outro status terminal (ex.: cancelado) — não interrompe a entrega */
            }
        }
    }

    private String buildAddressSnapshot(Customer customer) {
        if (customer == null) {
            return null;
        }
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("customerId", customer.getId());
        address.put("name", customer.getName());
        address.put("document", customer.getDocument());
        address.put("zipCode", customer.getZipCode());
        address.put("street", customer.getStreet());
        address.put("number", customer.getNumber());
        address.put("complement", customer.getComplement());
        address.put("district", customer.getDistrict());
        address.put("city", customer.getCity());
        address.put("state", customer.getState());
        try {
            return objectMapper.writeValueAsString(address);
        } catch (Exception ex) {
            return null;
        }
    }

    private void appendDeliveryEvent(Shipment shipment, DeliveryEvent.EventType eventType, String notes) {
        DeliveryEvent event = new DeliveryEvent();
        event.setShipment(shipment);
        event.setEventType(eventType.name());
        event.setNotes(notes);
        event.setOccurredAt(Instant.now());
        CurrentUser.id().ifPresent(event::setPerformedBy);
        shipment.getDeliveryEvents().add(event);
        shipmentRepository.save(shipment);
    }

    private ShipmentResponse changeStatus(Shipment shipment, Shipment.ShipmentStatus to, String notes) {
        shipment.setStatus(to);
        Shipment saved = shipmentRepository.save(shipment);
        appendDeliveryEvent(saved, DeliveryEvent.EventType.STATUS_CHANGED, notes);
        return shipmentMapper.toResponse(getEntity(saved.getId()));
    }

    private void assertTransition(
            Shipment shipment, Shipment.ShipmentStatus target, Shipment.ShipmentStatus... allowedFrom) {
        for (Shipment.ShipmentStatus allowed : allowedFrom) {
            if (shipment.getStatus() == allowed) {
                return;
            }
        }
        throw new BusinessRuleException(
                "Não é possível alterar para " + target + " a partir de " + shipment.getStatus());
    }

    private Shipment getAccessibleEntity(UUID id) {
        Shipment shipment = getEntity(id);
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), shipment.getStore().getId());
        return shipment;
    }

    private Shipment getEntity(UUID id) {
        return shipmentRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expedição", id));
    }

    private Map<String, Object> snapshot(Shipment shipment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("shipmentNumber", shipment.getShipmentNumber());
        map.put("status", shipment.getStatus());
        map.put("salesOrderId", shipment.getSalesOrder() != null ? shipment.getSalesOrder().getId() : null);
        map.put("storeId", shipment.getStore() != null ? shipment.getStore().getId() : null);
        return map;
    }
}
