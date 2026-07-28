package br.com.systemcommerce.quote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseRepository;
import br.com.systemcommerce.pricing.repository.PriceTableRepository;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.quote.dto.QuoteCreateRequest;
import br.com.systemcommerce.quote.dto.QuoteItemRequest;
import br.com.systemcommerce.quote.dto.QuoteResponse;
import br.com.systemcommerce.quote.entity.Quote;
import br.com.systemcommerce.quote.entity.QuoteItem;
import br.com.systemcommerce.quote.mapper.QuoteMapper;
import br.com.systemcommerce.quote.repository.QuoteAcceptanceRepository;
import br.com.systemcommerce.quote.repository.QuoteRepository;
import br.com.systemcommerce.quote.repository.QuoteRevisionRepository;
import br.com.systemcommerce.quote.repository.QuoteStatusHistoryRepository;
import br.com.systemcommerce.reservation.service.StockReservationService;
import br.com.systemcommerce.salesorder.dto.SalesOrderResponse;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.salesorder.service.SalesOrderService;
import br.com.systemcommerce.seller.repository.SellerProfileRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class QuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private QuoteStatusHistoryRepository statusHistoryRepository;

    @Mock
    private QuoteRevisionRepository revisionRepository;

    @Mock
    private QuoteAcceptanceRepository acceptanceRepository;

    @Mock
    private QuoteMapper quoteMapper;

    @Mock
    private StoreQuoteSequenceService storeQuoteSequenceService;

    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Mock
    private CustomerService customerService;

    @Mock
    private ProductService productService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private PriceTableRepository priceTableRepository;

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @Mock
    private PriceResolutionService priceResolutionService;

    @Mock
    private StockReservationService stockReservationService;

    @Mock
    private WarehouseRepository warehouseRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private QuoteService quoteService;

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
                        userId.toString(), null, List.of(new SimpleGrantedAuthority("QUOTE_CREATE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateQuoteWithCalculatedTotals() {
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(storeQuoteSequenceService.allocateNextQuoteNumber(store)).thenReturn("O-LJ01-000001");
        when(productService.requireUsableForSale(product.getId())).thenReturn(product);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> {
            Quote q = inv.getArgument(0);
            if (q.getId() == null) {
                q.setId(UUID.randomUUID());
            }
            return q;
        });
        when(quoteMapper.toResponse(any(Quote.class))).thenAnswer(inv -> toResponse(inv.getArgument(0)));

        QuoteCreateRequest request = new QuoteCreateRequest(
                storeId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                new BigDecimal("5.00"),
                new BigDecimal("2.00"),
                null,
                List.of(new QuoteItemRequest(
                        product.getId(), new BigDecimal("2"), new BigDecimal("10.00"), BigDecimal.ZERO, null)));

        QuoteResponse response = quoteService.create(request);

        ArgumentCaptor<Quote> captor = ArgumentCaptor.forClass(Quote.class);
        verify(quoteRepository).save(captor.capture());
        Quote saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Quote.QuoteStatus.DRAFT);
        assertThat(saved.getRevisionNumber()).isEqualTo(1);
        assertThat(saved.getSubtotalAmount()).isEqualByComparingTo("20.00");
        assertThat(saved.getDiscountAmount()).isEqualByComparingTo("5.00");
        assertThat(saved.getFreightAmount()).isEqualByComparingTo("2.00");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("17.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("17.00");
    }

    @Test
    void shouldComputeValidUntilFromValidityDaysWhenNotExplicit() {
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(storeQuoteSequenceService.allocateNextQuoteNumber(store)).thenReturn("O-LJ01-000001");
        when(productService.requireUsableForSale(product.getId())).thenReturn(product);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quoteMapper.toResponse(any(Quote.class))).thenAnswer(inv -> toResponse(inv.getArgument(0)));

        QuoteCreateRequest request = new QuoteCreateRequest(
                storeId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                15,
                null,
                null,
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                List.of(new QuoteItemRequest(product.getId(), BigDecimal.ONE, new BigDecimal("10.00"), null, null)));

        quoteService.create(request);

        ArgumentCaptor<Quote> captor = ArgumentCaptor.forClass(Quote.class);
        verify(quoteRepository).save(captor.capture());
        Quote saved = captor.getValue();
        assertThat(saved.getValidityDays()).isEqualTo(15);
        assertThat(saved.getValidUntil()).isEqualTo(java.time.LocalDate.now().plusDays(15));
    }

    @Test
    void shouldConvertApprovedQuoteToSalesOrder() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = new Quote();
        quote.setId(quoteId);
        quote.setStore(store);
        quote.setOrganization(store.getOrganization());
        quote.setQuoteNumber("O-LJ01-000001");
        quote.setStatus(Quote.QuoteStatus.APPROVED);
        quote.setReserveStock(false);
        quote.setDiscountAmount(BigDecimal.ZERO);
        quote.setFreightAmount(BigDecimal.ZERO);
        quote.setSubtotalAmount(BigDecimal.TEN);
        quote.setTotalAmount(BigDecimal.TEN);

        UUID orderId = UUID.randomUUID();
        SalesOrderResponse orderResponse = new SalesOrderResponse(
                orderId,
                "P-LJ01-000001",
                store.getOrganization().getId(),
                storeId,
                "LJ01",
                null,
                null,
                quoteId,
                null,
                null,
                null,
                null,
                null,
                SalesOrder.SalesOrderStatus.DRAFT,
                null,
                false,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.TEN,
                null,
                List.of(),
                true,
                true,
                false,
                0L,
                null,
                null);

        when(quoteRepository.findDetailedById(quoteId)).thenReturn(Optional.of(quote));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(salesOrderService.createFromQuote(eq(quote), isNull())).thenReturn(orderResponse);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));

        SalesOrderResponse result = quoteService.convert(quoteId);

        assertThat(result.id()).isEqualTo(orderId);
        assertThat(quote.getStatus()).isEqualTo(Quote.QuoteStatus.CONVERTED);
        assertThat(quote.getConvertedSalesOrderId()).isEqualTo(orderId);
        verify(salesOrderService).createFromQuote(eq(quote), isNull());
    }

    @Test
    void shouldCancelQuote() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = new Quote();
        quote.setId(quoteId);
        quote.setStore(store);
        quote.setOrganization(store.getOrganization());
        quote.setQuoteNumber("O-LJ01-000002");
        quote.setStatus(Quote.QuoteStatus.DRAFT);

        when(quoteRepository.findDetailedById(quoteId)).thenReturn(Optional.of(quote));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quoteMapper.toResponse(any(Quote.class))).thenAnswer(inv -> toResponse(inv.getArgument(0)));

        QuoteResponse response = quoteService.cancel(quoteId, "Cancelado teste");

        assertThat(response.status()).isEqualTo(Quote.QuoteStatus.CANCELLED);
        assertThat(quote.getStatus()).isEqualTo(Quote.QuoteStatus.CANCELLED);
    }

    @Test
    void shouldRejectConvertWhenNotSentOrApproved() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = new Quote();
        quote.setId(quoteId);
        quote.setStore(store);
        quote.setStatus(Quote.QuoteStatus.DRAFT);

        when(quoteRepository.findDetailedById(quoteId)).thenReturn(Optional.of(quote));
        when(storeAuthorizationEvaluator.assertCanAccess(eq(userId), eq(storeId))).thenReturn(store);

        assertThatThrownBy(() -> quoteService.convert(quoteId)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRejectExpiredConversionWithoutForcePermission() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = new Quote();
        quote.setId(quoteId);
        quote.setStore(store);
        quote.setStatus(Quote.QuoteStatus.EXPIRED);
        quote.setValidUntil(java.time.LocalDate.now().minusDays(1));

        when(quoteRepository.findDetailedById(quoteId)).thenReturn(Optional.of(quote));
        when(storeAuthorizationEvaluator.assertCanAccess(eq(userId), eq(storeId))).thenReturn(store);

        assertThatThrownBy(() -> quoteService.convert(quoteId, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("QUOTE_FORCE_CONVERT_EXPIRED");
    }

    @Test
    void shouldConvertPartiallyWhenItemsSpecified() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = new Quote();
        quote.setId(quoteId);
        quote.setStore(store);
        quote.setOrganization(store.getOrganization());
        quote.setQuoteNumber("O-LJ01-000003");
        quote.setStatus(Quote.QuoteStatus.SENT);
        quote.setReserveStock(false);

        QuoteItem item = new QuoteItem();
        item.setId(UUID.randomUUID());
        item.setProduct(product);
        item.setLineNumber(1);
        item.setQuantity(new BigDecimal("10"));
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setDiscountAmount(BigDecimal.ZERO);
        item.setLineSubtotal(new BigDecimal("100.00"));
        item.setLineTotal(new BigDecimal("100.00"));
        quote.addItem(item);

        UUID orderId = UUID.randomUUID();
        SalesOrderResponse orderResponse = new SalesOrderResponse(
                orderId,
                "P-LJ01-000002",
                store.getOrganization().getId(),
                storeId,
                "LJ01",
                null,
                null,
                quoteId,
                null,
                null,
                null,
                null,
                null,
                SalesOrder.SalesOrderStatus.DRAFT,
                null,
                false,
                new BigDecimal("40.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("40.00"),
                null,
                List.of(),
                true,
                true,
                false,
                0L,
                null,
                null);

        when(quoteRepository.findDetailedById(quoteId)).thenReturn(Optional.of(quote));
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(salesOrderService.createFromQuote(eq(quote), any())).thenReturn(orderResponse);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new br.com.systemcommerce.quote.dto.QuoteConversionRequest(
                List.of(new br.com.systemcommerce.quote.dto.QuoteConversionItemRequest(
                        item.getId(), new BigDecimal("4"))),
                false);

        SalesOrderResponse result = quoteService.convert(quoteId, request);

        assertThat(result.id()).isEqualTo(orderId);
        assertThat(quote.getStatus()).isEqualTo(Quote.QuoteStatus.PARTIALLY_CONVERTED);
    }

    private QuoteResponse toResponse(Quote q) {
        return new QuoteResponse(
                q.getId(),
                q.getQuoteNumber(),
                q.getOrganization() != null ? q.getOrganization().getId() : null,
                q.getStore() != null ? q.getStore().getId() : storeId,
                "LJ01",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                q.getChannel(),
                q.getPaymentCondition(),
                q.getCarrierName(),
                q.getExpectedDeliveryDate(),
                q.getValidityDays(),
                q.getStatus(),
                q.getValidUntil(),
                q.getNotes(),
                false,
                q.getSubtotalAmount(),
                q.getDiscountAmount(),
                q.getFreightAmount(),
                q.getSurchargeAmount(),
                q.getTotalAmount(),
                q.getRevisionNumber(),
                q.getConvertedSalesOrderId(),
                List.of(),
                true,
                true,
                false,
                0L,
                null,
                null);
    }
}
