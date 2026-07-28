package br.com.systemcommerce.salesorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.quote.repository.QuoteRepository;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.salesorder.dto.SalesOrderCreateRequest;
import br.com.systemcommerce.salesorder.dto.SalesOrderItemRequest;
import br.com.systemcommerce.salesorder.dto.SalesOrderResponse;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.salesorder.mapper.SalesOrderMapper;
import br.com.systemcommerce.salesorder.repository.SalesOrderRepository;
import br.com.systemcommerce.salesorder.repository.SalesOrderStatusHistoryRepository;
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

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private SalesOrderStatusHistoryRepository statusHistoryRepository;

    @Mock
    private SalesOrderMapper salesOrderMapper;

    @Mock
    private StoreSalesOrderSequenceService storeSalesOrderSequenceService;

    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Mock
    private CustomerService customerService;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SaleService saleService;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private br.com.systemcommerce.webhook.outbox.OutboxPublisher outboxPublisher;

    @Mock
    private br.com.systemcommerce.salesorder.repository.SalesOrderBillingHistoryRepository billingHistoryRepository;

    @Mock
    private br.com.systemcommerce.seller.repository.SellerProfileRepository sellerProfileRepository;

    @InjectMocks
    private SalesOrderService salesOrderService;

    private UUID userId;
    private UUID storeId;
    private Store store;
    private Product product;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        store = new Store();
        store.setId(storeId);
        store.setCode("LJ01");
        store.setOrganization(org);

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Produto A");
        product.setSalePrice(new BigDecimal("10.00"));

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("SALES_ORDER_CREATE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateSalesOrderWithCalculatedTotals() {
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(storeSalesOrderSequenceService.allocateNextOrderNumber(store)).thenReturn("P-LJ01-000001");
        when(productService.requireUsableForSale(product.getId())).thenReturn(product);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> {
            SalesOrder o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(UUID.randomUUID());
            }
            return o;
        });
        when(salesOrderMapper.toResponse(any(SalesOrder.class))).thenAnswer(inv -> {
            SalesOrder o = inv.getArgument(0);
            return new SalesOrderResponse(
                    o.getId(),
                    o.getOrderNumber(),
                    store.getOrganization().getId(),
                    storeId,
                    "LJ01",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    o.getStatus(),
                    null,
                    false,
                    o.getSubtotalAmount(),
                    o.getDiscountAmount(),
                    o.getFreightAmount(),
                    o.getTotalAmount(),
                    null,
                    List.of(),
                    true,
                    true,
                    false,
                    0L,
                    null,
                    null);
        });

        SalesOrderCreateRequest request = new SalesOrderCreateRequest(
                storeId,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                new BigDecimal("3.00"),
                new BigDecimal("1.50"),
                List.of(new SalesOrderItemRequest(
                        product.getId(),
                        new BigDecimal("3"),
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO,
                        null)));

        SalesOrderResponse response = salesOrderService.create(request);

        ArgumentCaptor<SalesOrder> captor = ArgumentCaptor.forClass(SalesOrder.class);
        verify(salesOrderRepository).save(captor.capture());
        SalesOrder saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SalesOrder.SalesOrderStatus.DRAFT);
        assertThat(saved.getSubtotalAmount()).isEqualByComparingTo("30.00");
        assertThat(saved.getDiscountAmount()).isEqualByComparingTo("3.00");
        assertThat(saved.getFreightAmount()).isEqualByComparingTo("1.50");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("28.50");
        assertThat(response.totalAmount()).isEqualByComparingTo("28.50");
    }

    @Test
    void shouldCancelSalesOrder() {
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder();
        order.setId(orderId);
        order.setStore(store);
        order.setOrganization(store.getOrganization());
        order.setOrderNumber("P-LJ01-000002");
        order.setStatus(SalesOrder.SalesOrderStatus.APPROVED);

        when(salesOrderRepository.findDetailedById(orderId)).thenReturn(Optional.of(order));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrderMapper.toResponse(any(SalesOrder.class))).thenAnswer(inv -> {
            SalesOrder o = inv.getArgument(0);
            return new SalesOrderResponse(
                    o.getId(),
                    o.getOrderNumber(),
                    null,
                    storeId,
                    "LJ01",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    o.getStatus(),
                    null,
                    false,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    List.of(),
                    false,
                    false,
                    false,
                    0L,
                    null,
                    null);
        });

        SalesOrderResponse response = salesOrderService.cancel(orderId, "Cancelado");

        assertThat(response.status()).isEqualTo(SalesOrder.SalesOrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(SalesOrder.SalesOrderStatus.CANCELLED);
    }

    @Test
    void shouldBlockCancelWhenSaleAlreadyGenerated() {
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder();
        order.setId(orderId);
        order.setStore(store);
        order.setStatus(SalesOrder.SalesOrderStatus.INVOICED);
        br.com.systemcommerce.sale.entity.Sale sale = new br.com.systemcommerce.sale.entity.Sale();
        sale.setId(UUID.randomUUID());
        order.setGeneratedSale(sale);

        when(salesOrderRepository.findDetailedById(orderId)).thenReturn(Optional.of(order));
        when(storeAuthorizationEvaluator.assertCanAccess(eq(userId), eq(storeId))).thenReturn(store);

        assertThatThrownBy(() -> salesOrderService.cancel(orderId, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("venda gerada");
    }
}
