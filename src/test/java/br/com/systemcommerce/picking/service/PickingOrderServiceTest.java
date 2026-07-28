package br.com.systemcommerce.picking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.picking.dto.PickingItemPickRequest;
import br.com.systemcommerce.picking.dto.PickingOrderCreateRequest;
import br.com.systemcommerce.picking.dto.PickingOrderResponse;
import br.com.systemcommerce.picking.entity.PickingOrder;
import br.com.systemcommerce.picking.entity.PickingOrderItem;
import br.com.systemcommerce.picking.mapper.PickingOrderMapper;
import br.com.systemcommerce.picking.repository.PickingAssignmentRepository;
import br.com.systemcommerce.picking.repository.PickingDivergenceRepository;
import br.com.systemcommerce.picking.repository.PickingEventRepository;
import br.com.systemcommerce.picking.repository.PickingOrderItemRepository;
import br.com.systemcommerce.picking.repository.PickingOrderRepository;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.reservation.dto.StockReservationLineRequest;
import br.com.systemcommerce.reservation.repository.StockReservationRepository;
import br.com.systemcommerce.reservation.service.StockReservationService;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.salesorder.entity.SalesOrderItem;
import br.com.systemcommerce.salesorder.repository.SalesOrderRepository;
import br.com.systemcommerce.salesorder.service.SalesOrderService;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Testes unitários de {@link PickingOrderService}. A separação nunca baixa estoque físico —
 * apenas organiza a coleta; por isso o service não depende de {@code InventoryService}.
 */
@ExtendWith(MockitoExtension.class)
class PickingOrderServiceTest {

    @Mock
    private PickingOrderRepository pickingOrderRepository;

    @Mock
    private PickingOrderItemRepository pickingOrderItemRepository;

    @Mock
    private PickingAssignmentRepository pickingAssignmentRepository;

    @Mock
    private PickingEventRepository pickingEventRepository;

    @Mock
    private PickingDivergenceRepository pickingDivergenceRepository;

    @Mock
    private StorePickingOrderSequenceService sequenceService;

    @Mock
    private PickingOrderMapper mapper;

    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private StockReservationService stockReservationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private PickingOrderService pickingOrderService;

    private UUID userId;
    private UUID storeId;
    private Store store;
    private Warehouse warehouse;
    private Product product;
    private SalesOrder salesOrder;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());

        store = new Store();
        store.setId(storeId);
        store.setCode("LJ01");
        store.setOrganization(organization);

        warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setStore(store);

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Produto Separável");
        product.setBarcode("789000111");

        salesOrder = new SalesOrder();
        salesOrder.setId(UUID.randomUUID());
        salesOrder.setStore(store);
        salesOrder.setOrganization(organization);
        salesOrder.setWarehouse(warehouse);
        salesOrder.setOrderNumber("P-LJ01-000001");
        salesOrder.setStatus(SalesOrder.SalesOrderStatus.APPROVED);

        SalesOrderItem soItem = new SalesOrderItem();
        soItem.setId(UUID.randomUUID());
        soItem.setSalesOrder(salesOrder);
        soItem.setProduct(product);
        soItem.setLineNumber(1);
        soItem.setQuantity(new BigDecimal("10"));
        salesOrder.addItem(soItem);

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, List.of(new SimpleGrantedAuthority("PICKING_MANAGE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreatePickingOrderFromApprovedSalesOrderAndNeverTouchInventory() {
        when(salesOrderRepository.findDetailedById(salesOrder.getId())).thenReturn(Optional.of(salesOrder));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(pickingOrderRepository.findBySalesOrderId(salesOrder.getId())).thenReturn(List.of());
        when(sequenceService.allocateNextPickingNumber(store)).thenReturn("SP-LJ01-000001");
        when(pickingOrderItemRepository.findPreferredStorageLocationId(any(), any())).thenReturn(null);
        when(pickingOrderRepository.save(any(PickingOrder.class))).thenAnswer(inv -> {
            PickingOrder p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });
        when(pickingOrderRepository.findDetailedById(any())).thenAnswer(inv -> Optional.of(buildOpenPickingOrder()));
        when(mapper.toResponse(any(PickingOrder.class))).thenReturn(mockResponse());

        pickingOrderService.createFromSalesOrder(new PickingOrderCreateRequest(salesOrder.getId(), "Urgente"));

        verify(salesOrderService).startPicking(salesOrder.getId());

        ArgumentCaptor<PickingOrder> captor = ArgumentCaptor.forClass(PickingOrder.class);
        verify(pickingOrderRepository).save(captor.capture());
        PickingOrder saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PickingOrder.PickingOrderStatus.PENDING);
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getQuantityRequested()).isEqualByComparingTo("10");
    }

    @Test
    void shouldRejectPickWhenBarcodeDoesNotMatchAnyPendingItem() {
        PickingOrder order = buildOpenPickingOrder();
        when(pickingOrderRepository.findDetailedById(order.getId())).thenReturn(Optional.of(order));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> pickingOrderService.pickItem(
                        order.getId(), new PickingItemPickRequest("000-invalido", new BigDecimal("1"), null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldSkipDuplicatePickWhenIdempotencyKeyAlreadyUsed() {
        PickingOrder order = buildOpenPickingOrder();
        String idempotencyKey = "SCAN-0001";
        when(pickingOrderRepository.findDetailedById(order.getId())).thenReturn(Optional.of(order));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(pickingEventRepository.findByPickingOrderIdAndIdempotencyKey(order.getId(), idempotencyKey))
                .thenReturn(Optional.of(new br.com.systemcommerce.picking.entity.PickingEvent()));
        when(mapper.toResponse(order)).thenReturn(mockResponse());

        pickingOrderService.pickItem(
                order.getId(), new PickingItemPickRequest(product.getBarcode(), new BigDecimal("3"), idempotencyKey));

        verify(pickingOrderItemRepository, never()).save(any());
        assertThat(order.getItems().get(0).getQuantityPicked()).isEqualByComparingTo("0");
    }

    @Test
    void shouldPickItemAndUpdateQuantityPicked() {
        PickingOrder order = buildOpenPickingOrder();
        when(pickingOrderRepository.findDetailedById(order.getId())).thenReturn(Optional.of(order));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(pickingOrderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pickingOrderRepository.save(any(PickingOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(order)).thenReturn(mockResponse());

        pickingOrderService.pickItem(order.getId(), new PickingItemPickRequest(product.getBarcode(), new BigDecimal("4"), null));

        assertThat(order.getItems().get(0).getQuantityPicked()).isEqualByComparingTo("4");
        assertThat(order.getStatus()).isEqualTo(PickingOrder.PickingOrderStatus.PARTIALLY_PICKED);
    }

    @Test
    void shouldCompleteConsumeAndReleaseReservationThenMarkSalesOrderPicked() {
        PickingOrder order = buildOpenPickingOrder();
        order.getItems().get(0).setQuantityPicked(new BigDecimal("7"));
        br.com.systemcommerce.reservation.entity.StockReservation reservation =
                new br.com.systemcommerce.reservation.entity.StockReservation();
        reservation.setId(UUID.randomUUID());
        order.setStockReservation(reservation);

        when(pickingOrderRepository.findDetailedById(order.getId())).thenReturn(Optional.of(order));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(pickingOrderRepository.save(any(PickingOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(order)).thenReturn(mockResponse());

        pickingOrderService.complete(order.getId());

        verify(stockReservationService)
                .consumeForOrigin(
                        eq(br.com.systemcommerce.reservation.entity.StockReservation.OriginType.SALES_ORDER),
                        eq(salesOrder.getId()),
                        anyList());
        verify(stockReservationService)
                .releaseForOrigin(
                        eq(br.com.systemcommerce.reservation.entity.StockReservation.OriginType.SALES_ORDER),
                        eq(salesOrder.getId()),
                        anyList());
        verify(salesOrderService).markPicked(salesOrder.getId());
        assertThat(order.getStatus()).isEqualTo(PickingOrder.PickingOrderStatus.PICKED);
    }

    @Test
    void shouldRejectCompleteWhenNothingWasPicked() {
        PickingOrder order = buildOpenPickingOrder();
        when(pickingOrderRepository.findDetailedById(order.getId())).thenReturn(Optional.of(order));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> pickingOrderService.complete(order.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Nenhum item");

        verify(salesOrderService, never()).markPicked(any());
    }

    private PickingOrder buildOpenPickingOrder() {
        PickingOrder order = new PickingOrder();
        order.setId(UUID.randomUUID());
        order.setOrganization(salesOrder.getOrganization());
        order.setStore(store);
        order.setWarehouse(warehouse);
        order.setSalesOrder(salesOrder);
        order.setPickingNumber("SP-LJ01-000001");
        order.setStatus(PickingOrder.PickingOrderStatus.IN_PROGRESS);

        PickingOrderItem item = new PickingOrderItem();
        item.setProduct(product);
        item.setLineNumber(1);
        item.setQuantityRequested(new BigDecimal("10"));
        item.setQuantityPicked(BigDecimal.ZERO);
        order.addItem(item);
        return order;
    }

    private PickingOrderResponse mockResponse() {
        return new PickingOrderResponse(
                UUID.randomUUID(),
                "SP-LJ01-000001",
                salesOrder.getOrganization().getId(),
                storeId,
                "LJ01",
                warehouse.getId(),
                null,
                salesOrder.getId(),
                salesOrder.getOrderNumber(),
                null,
                PickingOrder.PickingOrderStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                true,
                false,
                0L,
                null,
                null);
    }
}
