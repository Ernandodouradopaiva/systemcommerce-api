package br.com.systemcommerce.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.reservation.dto.StockReservationCreateRequest;
import br.com.systemcommerce.reservation.dto.StockReservationItemRequest;
import br.com.systemcommerce.reservation.dto.StockReservationLineRequest;
import br.com.systemcommerce.reservation.dto.StockReservationResponse;
import br.com.systemcommerce.reservation.entity.StockReservation;
import br.com.systemcommerce.reservation.entity.StockReservationItem;
import br.com.systemcommerce.reservation.mapper.StockReservationMapper;
import br.com.systemcommerce.reservation.repository.StockReservationRepository;
import br.com.systemcommerce.reservation.repository.StockReservationStatusHistoryRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
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
 * Testes unitários de {@link StockReservationService}: a reserva nunca altera o saldo físico —
 * apenas {@code quantityReserved} do Inventory (via {@link InventoryService}, mockado aqui). Também
 * cobre o cenário de concorrência simples: disputa por disponibilidade insuficiente.
 */
@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private StockReservationStatusHistoryRepository historyRepository;

    @Mock
    private StockReservationMapper mapper;

    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private StockReservationService stockReservationService;

    private UUID userId;
    private UUID storeId;
    private UUID warehouseId;
    private Store store;
    private Warehouse warehouse;
    private Product product;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();

        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());

        store = new Store();
        store.setId(storeId);
        store.setCode("LJ01");
        store.setOrganization(organization);

        warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        warehouse.setStore(store);

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Produto Reservável");

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, List.of(new SimpleGrantedAuthority("STOCK_RESERVATION_MANAGE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateReservationAndIncreaseReservedQuantityViaInventoryService() {
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(warehouseService.requireUsable(warehouseId)).thenReturn(warehouse);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(reservationRepository.countByReservationNumberPrefix(any())).thenReturn(0L);
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> {
            StockReservation r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });
        when(reservationRepository.findDetailedById(any())).thenAnswer(inv -> Optional.of(captured()));
        when(mapper.toResponse(any(StockReservation.class))).thenReturn(mockResponse());

        StockReservationCreateRequest request = new StockReservationCreateRequest(
                storeId,
                warehouseId,
                StockReservation.OriginType.SALES_ORDER,
                UUID.randomUUID(),
                "SO-000001",
                null,
                null,
                null,
                List.of(new StockReservationItemRequest(product.getId(), new BigDecimal("5"))));

        stockReservationService.create(request);

        verify(inventoryService).reserveQuantity(product.getId(), warehouseId, new BigDecimal("5.000"));

        ArgumentCaptor<StockReservation> captor = ArgumentCaptor.forClass(StockReservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StockReservation.ReservationStatus.ACTIVE);
        assertThat(captor.getValue().getItems()).hasSize(1);
    }

    @Test
    void shouldReturnExistingReservationWhenIdempotencyKeyAlreadyUsed() {
        String idempotencyKey = "IDEMP-123";
        UUID organizationId = store.getOrganization().getId();
        StockReservation existing = captured();
        existing.setIdempotencyKey(idempotencyKey);

        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(reservationRepository.findByOrganizationIdAndIdempotencyKey(organizationId, idempotencyKey))
                .thenReturn(Optional.of(existing));
        when(reservationRepository.findDetailedById(existing.getId())).thenReturn(Optional.of(existing));
        when(mapper.toResponse(existing)).thenReturn(mockResponse());

        StockReservationCreateRequest request = new StockReservationCreateRequest(
                storeId,
                warehouseId,
                StockReservation.OriginType.SALES_ORDER,
                UUID.randomUUID(),
                "SO-000001",
                null,
                null,
                idempotencyKey,
                List.of(new StockReservationItemRequest(product.getId(), new BigDecimal("5"))));

        stockReservationService.create(request);

        verify(inventoryService, never()).reserveQuantity(any(), any(), any());
        verify(reservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void shouldPropagateBusinessRuleExceptionWhenAvailabilityInsufficient() {
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(warehouseService.requireUsable(warehouseId)).thenReturn(warehouse);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(reservationRepository.countByReservationNumberPrefix(any())).thenReturn(0L);
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> {
            StockReservation r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });
        // Simula concorrência: outra reserva consumiu o saldo disponível entre a validação e a tentativa de lock.
        org.mockito.Mockito.doThrow(new BusinessRuleException("Quantidade disponível insuficiente para reserva"))
                .when(inventoryService)
                .reserveQuantity(eq(product.getId()), eq(warehouseId), any(BigDecimal.class));

        StockReservationCreateRequest request = new StockReservationCreateRequest(
                storeId,
                warehouseId,
                StockReservation.OriginType.SALES_ORDER,
                UUID.randomUUID(),
                "SO-000002",
                null,
                null,
                null,
                List.of(new StockReservationItemRequest(product.getId(), new BigDecimal("100"))));

        assertThatThrownBy(() -> stockReservationService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("insuficiente");
    }

    @Test
    void shouldConsumeReservedQuantityThroughInventoryService() {
        UUID reservationId = UUID.randomUUID();
        StockReservation reservation = captured();
        reservation.setId(reservationId);

        when(reservationRepository.findDetailedById(reservationId)).thenReturn(Optional.of(reservation));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(reservation)).thenReturn(mockResponse());

        stockReservationService.consume(
                reservationId, List.of(new StockReservationLineRequest(product.getId(), new BigDecimal("2"))));

        verify(inventoryService).consumeReservedQuantity(product.getId(), warehouseId, new BigDecimal("2.000"));
        assertThat(reservation.getItems().get(0).getQuantityConsumed()).isEqualByComparingTo("2");
    }

    @Test
    void shouldReleaseRemainingQuantityOnCancel() {
        UUID reservationId = UUID.randomUUID();
        StockReservation reservation = captured();
        reservation.setId(reservationId);

        when(reservationRepository.findDetailedById(reservationId)).thenReturn(Optional.of(reservation));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(reservation)).thenReturn(mockResponse());

        stockReservationService.cancel(reservationId, "Cancelado pelo operador");

        verify(inventoryService).releaseReservedQuantity(product.getId(), warehouseId, new BigDecimal("5"));
        assertThat(reservation.getStatus()).isEqualTo(StockReservation.ReservationStatus.CANCELLED);
    }

    @Test
    void shouldExpirePastDueReservations() {
        StockReservation expired = captured();
        expired.setId(UUID.randomUUID());
        expired.setExpiresAt(java.time.Instant.now().minusSeconds(60));

        when(reservationRepository.findActivePastDue(any())).thenReturn(List.of(expired));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.findDetailedById(expired.getId())).thenReturn(Optional.of(expired));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(mapper.toResponse(expired)).thenReturn(mockResponse());

        int count = stockReservationService.expireExpired();

        assertThat(count).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(StockReservation.ReservationStatus.EXPIRED);
        verify(inventoryService, times(1)).releaseReservedQuantity(product.getId(), warehouseId, new BigDecimal("5"));
    }

    private StockReservation captured() {
        StockReservation reservation = new StockReservation();
        reservation.setId(UUID.randomUUID());
        reservation.setOrganization(store.getOrganization());
        reservation.setStore(store);
        reservation.setWarehouse(warehouse);
        reservation.setReservationNumber("RES-LJ01-000001");
        reservation.setOriginType(StockReservation.OriginType.SALES_ORDER);
        reservation.setOriginId(UUID.randomUUID());
        reservation.setStatus(StockReservation.ReservationStatus.ACTIVE);

        StockReservationItem item = new StockReservationItem();
        item.setProduct(product);
        item.setLineNumber(1);
        item.setQuantityReserved(new BigDecimal("5"));
        reservation.addItem(item);
        return reservation;
    }

    private StockReservationResponse mockResponse() {
        return new StockReservationResponse(
                UUID.randomUUID(),
                "RES-LJ01-000001",
                store.getOrganization().getId(),
                storeId,
                "LJ01",
                warehouseId,
                null,
                StockReservation.OriginType.SALES_ORDER,
                UUID.randomUUID(),
                null,
                StockReservation.ReservationStatus.ACTIVE,
                null,
                null,
                List.of(),
                0L,
                null,
                null);
    }
}
