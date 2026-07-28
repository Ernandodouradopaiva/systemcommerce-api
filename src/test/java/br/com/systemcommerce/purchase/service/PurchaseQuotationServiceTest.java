package br.com.systemcommerce.purchase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.purchase.dto.GeneratePurchaseOrdersRequest;
import br.com.systemcommerce.purchase.dto.InviteSuppliersRequest;
import br.com.systemcommerce.purchase.dto.PurchaseOrderResponse;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationResponse;
import br.com.systemcommerce.purchase.dto.QuotationComparisonResponse;
import br.com.systemcommerce.purchase.dto.SelectQuotationItemsRequest;
import br.com.systemcommerce.purchase.dto.SupplierQuotationResponseItemRequest;
import br.com.systemcommerce.purchase.dto.SupplierQuotationResponseRequest;
import br.com.systemcommerce.purchase.entity.PurchaseQuotation;
import br.com.systemcommerce.purchase.entity.PurchaseQuotationItem;
import br.com.systemcommerce.purchase.entity.PurchaseQuotationSupplier;
import br.com.systemcommerce.purchase.entity.SupplierQuotationResponse;
import br.com.systemcommerce.purchase.mapper.PurchaseQuotationMapper;
import br.com.systemcommerce.purchase.repository.PurchaseQuotationRepository;
import br.com.systemcommerce.purchase.repository.PurchaseQuotationStatusHistoryRepository;
import br.com.systemcommerce.purchase.repository.PurchaseQuotationSupplierRepository;
import br.com.systemcommerce.purchase.repository.SupplierQuotationResponseRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.document.DocumentConversionService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.repository.SupplierRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PurchaseQuotationServiceTest {

    @Mock
    private PurchaseQuotationRepository purchaseQuotationRepository;

    @Mock
    private PurchaseQuotationSupplierRepository purchaseQuotationSupplierRepository;

    @Mock
    private SupplierQuotationResponseRepository supplierQuotationResponseRepository;

    @Mock
    private PurchaseQuotationStatusHistoryRepository statusHistoryRepository;

    @Mock
    private StorePurchaseQuotationSequenceService storePurchaseQuotationSequenceService;

    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private DocumentConversionService documentConversionService;

    @Mock
    private PurchaseOrderService purchaseOrderService;

    private PurchaseQuotationService purchaseQuotationService;

    private UUID userId;
    private UUID storeId;
    private Store store;
    private Supplier supplierA;
    private Supplier supplierB;
    private Product product;
    private AtomicReference<PurchaseQuotation> savedQuotation;
    private List<SupplierQuotationResponse> responseStore;

    @BeforeEach
    void setUp() {
        purchaseQuotationService = new PurchaseQuotationService(
                purchaseQuotationRepository,
                purchaseQuotationSupplierRepository,
                supplierQuotationResponseRepository,
                statusHistoryRepository,
                new PurchaseQuotationMapper(),
                storePurchaseQuotationSequenceService,
                storeAuthorizationEvaluator,
                productRepository,
                supplierRepository,
                userRepository,
                domainAuditService,
                documentConversionService,
                purchaseOrderService);

        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        Organization org = new Organization();
        org.setId(UUID.randomUUID());

        store = new Store();
        store.setId(storeId);
        store.setCode("LJ01");
        store.setOrganization(org);

        supplierA = new Supplier();
        supplierA.setId(UUID.randomUUID());
        supplierA.setLegalName("Fornecedor A");

        supplierB = new Supplier();
        supplierB.setId(UUID.randomUUID());
        supplierB.setLegalName("Fornecedor B");

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Parafuso M6");

        savedQuotation = new AtomicReference<>();
        responseStore = new ArrayList<>();

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, List.of(new SimpleGrantedAuthority("PURCHASE_QUOTATION_CREATE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubPersistence() {
        lenient().when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        lenient()
                .when(storePurchaseQuotationSequenceService.allocateNextQuotationNumber(store))
                .thenReturn("CC-LJ01-000001");
        lenient().when(purchaseQuotationRepository.save(any(PurchaseQuotation.class))).thenAnswer(inv -> {
            PurchaseQuotation q = inv.getArgument(0);
            if (q.getId() == null) {
                q.setId(UUID.randomUUID());
            }
            q.getItems().forEach(item -> {
                if (item.getId() == null) {
                    item.setId(UUID.randomUUID());
                }
            });
            q.getSuppliers().forEach(supplier -> {
                if (supplier.getId() == null) {
                    supplier.setId(UUID.randomUUID());
                }
            });
            savedQuotation.set(q);
            return q;
        });
        lenient()
                .when(purchaseQuotationRepository.findDetailedById(any()))
                .thenAnswer(inv -> Optional.ofNullable(savedQuotation.get()));
        lenient().when(supplierRepository.findById(supplierA.getId())).thenReturn(Optional.of(supplierA));
        lenient().when(supplierRepository.findById(supplierB.getId())).thenReturn(Optional.of(supplierB));
        lenient()
                .when(purchaseQuotationSupplierRepository.findByPurchaseQuotationIdAndSupplierId(any(), any()))
                .thenAnswer(inv -> savedQuotation.get().getSuppliers().stream()
                        .filter(s -> s.getSupplier().getId().equals(inv.getArgument(1)))
                        .findFirst());
        lenient()
                .when(supplierQuotationResponseRepository.findByPurchaseQuotationIdAndSupplierId(any(), any()))
                .thenAnswer(inv -> responseStore.stream()
                        .filter(r -> r.getSupplier().getId().equals(inv.getArgument(1)))
                        .findFirst());
        lenient()
                .when(supplierQuotationResponseRepository.findDetailedByPurchaseQuotationId(any()))
                .thenAnswer(inv -> responseStore);
        lenient()
                .when(supplierQuotationResponseRepository.save(any(SupplierQuotationResponse.class)))
                .thenAnswer(inv -> {
                    SupplierQuotationResponse response = inv.getArgument(0);
                    if (response.getId() == null) {
                        response.setId(UUID.randomUUID());
                        responseStore.add(response);
                    }
                    response.getItems().forEach(item -> {
                        if (item.getId() == null) {
                            item.setId(UUID.randomUUID());
                        }
                    });
                    return response;
                });
        lenient()
                .when(purchaseQuotationSupplierRepository.save(any(PurchaseQuotationSupplier.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private PurchaseQuotationResponse createQuotationWithItem(BigDecimal quantity, List<UUID> supplierIds) {
        stubPersistence();
        PurchaseQuotationCreateRequest request = new PurchaseQuotationCreateRequest(
                null,
                storeId,
                null,
                null,
                PurchaseQuotation.SelectionCriteria.TOTAL_COST,
                false,
                "Cotação de parafusos",
                supplierIds,
                List.of(new PurchaseQuotationItemRequest(null, product.getId(), "Parafuso M6", quantity, "CX")));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        return purchaseQuotationService.create(request);
    }

    @Test
    void shouldCreateManualQuotationWithSequentialNumberAndInvitedSuppliers() {
        PurchaseQuotationResponse response =
                createQuotationWithItem(new BigDecimal("100"), List.of(supplierA.getId(), supplierB.getId()));

        assertThat(response.quotationNumber()).isEqualTo("CC-LJ01-000001");
        assertThat(response.status()).isEqualTo(PurchaseQuotation.PurchaseQuotationStatus.DRAFT);
        assertThat(response.suppliers()).hasSize(2);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void shouldNotSendWithoutInvitedSuppliers() {
        PurchaseQuotationResponse response = createQuotationWithItem(new BigDecimal("100"), List.of());

        assertThatThrownBy(() -> purchaseQuotationService.send(response.id()))
                .isInstanceOf(BusinessRuleException.class);
    }

    private PurchaseQuotationResponse createAndSendQuotation() {
        PurchaseQuotationResponse created =
                createQuotationWithItem(new BigDecimal("100"), List.of(supplierA.getId(), supplierB.getId()));
        return purchaseQuotationService.send(created.id());
    }

    @Test
    void shouldRegisterSupplierResponseAndComputeOfficialTotal() {
        PurchaseQuotationResponse sent = createAndSendQuotation();
        UUID itemId = savedQuotation.get().getItems().getFirst().getId();

        PurchaseQuotationResponse updated = purchaseQuotationService.registerResponse(
                sent.id(),
                supplierA.getId(),
                new SupplierQuotationResponseRequest(
                        "30 dias",
                        new BigDecimal("10.00"),
                        new BigDecimal("5.00"),
                        new BigDecimal("2.00"),
                        7,
                        null,
                        null,
                        List.of(new SupplierQuotationResponseItemRequest(
                                itemId, new BigDecimal("3.50"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, 7, null, null))));

        assertThat(updated.status()).isEqualTo(PurchaseQuotation.PurchaseQuotationStatus.RESPONSES_PENDING);
        assertThat(responseStore).hasSize(1);
        // linha: 3.50 * 100 = 350.00
        assertThat(responseStore.getFirst().getItems().getFirst().getLineTotal()).isEqualByComparingTo("350.00");
    }

    @Test
    void shouldRejectRegisterResponseWhenSupplierNotInvited() {
        PurchaseQuotationResponse sent = createAndSendQuotation();
        UUID itemId = savedQuotation.get().getItems().getFirst().getId();
        UUID uninvitedSupplierId = UUID.randomUUID();

        assertThatThrownBy(() -> purchaseQuotationService.registerResponse(
                        sent.id(),
                        uninvitedSupplierId,
                        new SupplierQuotationResponseRequest(
                                null, null, null, null, null, null, null,
                                List.of(new SupplierQuotationResponseItemRequest(
                                        itemId, new BigDecimal("1.00"), null, null, null, null, null, null, null)))))
                .isInstanceOf(BusinessRuleException.class);
    }

    private void registerResponseFor(UUID quotationId, UUID itemId, Supplier supplier, BigDecimal unitPrice) {
        purchaseQuotationService.registerResponse(
                quotationId,
                supplier.getId(),
                new SupplierQuotationResponseRequest(
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        5,
                        null,
                        null,
                        List.of(new SupplierQuotationResponseItemRequest(
                                itemId, unitPrice, new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, 5, null, null))));
    }

    @Test
    void shouldReturnComparisonFlaggingLowestOffer() {
        PurchaseQuotationResponse sent = createAndSendQuotation();
        UUID itemId = savedQuotation.get().getItems().getFirst().getId();

        registerResponseFor(sent.id(), itemId, supplierA, new BigDecimal("5.00"));
        registerResponseFor(sent.id(), itemId, supplierB, new BigDecimal("3.00"));

        QuotationComparisonResponse comparison = purchaseQuotationService.comparison(sent.id());

        assertThat(comparison.items()).hasSize(1);
        var offers = comparison.items().getFirst().offers();
        assertThat(offers).hasSize(2);
        var cheapest = offers.stream()
                .filter(o -> o.supplierId().equals(supplierB.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(cheapest.lowestPrice()).isTrue();
        var pricier = offers.stream()
                .filter(o -> o.supplierId().equals(supplierA.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(pricier.lowestPrice()).isFalse();
    }

    @Test
    void shouldSelectItemsManuallyWithoutAutoSelectingLowestPrice() {
        PurchaseQuotationResponse sent = createAndSendQuotation();
        UUID itemId = savedQuotation.get().getItems().getFirst().getId();

        registerResponseFor(sent.id(), itemId, supplierA, new BigDecimal("5.00"));
        registerResponseFor(sent.id(), itemId, supplierB, new BigDecimal("3.00"));

        UUID responseItemIdSupplierA = responseStore.stream()
                .filter(r -> r.getSupplier().getId().equals(supplierA.getId()))
                .findFirst()
                .orElseThrow()
                .getItems()
                .getFirst()
                .getId();

        PurchaseQuotationResponse selected = purchaseQuotationService.selectItems(
                sent.id(),
                new SelectQuotationItemsRequest(
                        false,
                        List.of(new SelectQuotationItemsRequest.QuotationItemSelection(
                                itemId, responseItemIdSupplierA, new BigDecimal("100")))));

        // Mesmo com fornecedor B mais barato, seleção manual deve escolher A conforme solicitado.
        assertThat(selected.status()).isEqualTo(PurchaseQuotation.PurchaseQuotationStatus.SELECTED);
        var selectedItem = savedQuotation.get().getItems().getFirst();
        assertThat(selectedItem.getQuantitySelected()).isEqualByComparingTo("100");
        var supplierAResponse = responseStore.stream()
                .filter(r -> r.getSupplier().getId().equals(supplierA.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(supplierAResponse.getItems().getFirst().getSelected()).isTrue();
        var supplierBResponse = responseStore.stream()
                .filter(r -> r.getSupplier().getId().equals(supplierB.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(supplierBResponse.getItems().getFirst().getSelected()).isFalse();
    }

    @Test
    void shouldGeneratePurchaseOrderPerSelectedSupplierAndRecordDocumentConversion() {
        PurchaseQuotationResponse sent = createAndSendQuotation();
        UUID itemId = savedQuotation.get().getItems().getFirst().getId();
        registerResponseFor(sent.id(), itemId, supplierA, new BigDecimal("5.00"));

        UUID responseItemId = responseStore.getFirst().getItems().getFirst().getId();
        purchaseQuotationService.selectItems(
                sent.id(),
                new SelectQuotationItemsRequest(
                        false,
                        List.of(new SelectQuotationItemsRequest.QuotationItemSelection(
                                itemId, responseItemId, new BigDecimal("100")))));

        UUID generatedOrderId = UUID.randomUUID();
        PurchaseOrderResponse poResponse = mock(PurchaseOrderResponse.class);
        when(poResponse.id()).thenReturn(generatedOrderId);
        when(poResponse.orderNumber()).thenReturn("PC-LJ01-000001");
        when(purchaseOrderService.create(any())).thenReturn(poResponse);

        List<PurchaseOrderResponse> orders = purchaseQuotationService.generatePurchaseOrders(
                sent.id(), new GeneratePurchaseOrdersRequest(UUID.randomUUID(), null, null));

        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().id()).isEqualTo(generatedOrderId);
        verify(documentConversionService)
                .record(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldCloseLockingResponses() {
        PurchaseQuotationResponse sent = createAndSendQuotation();
        UUID itemId = savedQuotation.get().getItems().getFirst().getId();
        registerResponseFor(sent.id(), itemId, supplierA, new BigDecimal("5.00"));
        UUID responseItemId = responseStore.getFirst().getItems().getFirst().getId();
        purchaseQuotationService.selectItems(
                sent.id(),
                new SelectQuotationItemsRequest(
                        false,
                        List.of(new SelectQuotationItemsRequest.QuotationItemSelection(
                                itemId, responseItemId, new BigDecimal("100")))));

        PurchaseQuotationResponse closed = purchaseQuotationService.close(sent.id());

        assertThat(closed.status()).isEqualTo(PurchaseQuotation.PurchaseQuotationStatus.CLOSED);
        assertThat(responseStore.getFirst().getLocked()).isTrue();
    }

    @Test
    void shouldCancelLockingResponsesWhenNotYetClosed() {
        PurchaseQuotationResponse sent = createAndSendQuotation();
        UUID itemId = savedQuotation.get().getItems().getFirst().getId();
        registerResponseFor(sent.id(), itemId, supplierA, new BigDecimal("5.00"));

        PurchaseQuotationResponse cancelled = purchaseQuotationService.cancel(sent.id(), "Necessidade extinta");

        assertThat(cancelled.status()).isEqualTo(PurchaseQuotation.PurchaseQuotationStatus.CANCELLED);
        assertThat(responseStore.getFirst().getLocked()).isTrue();
    }

    @Test
    void shouldRejectCancelWhenAlreadyClosed() {
        PurchaseQuotationResponse sent = createAndSendQuotation();
        UUID itemId = savedQuotation.get().getItems().getFirst().getId();
        registerResponseFor(sent.id(), itemId, supplierA, new BigDecimal("5.00"));
        UUID responseItemId = responseStore.getFirst().getItems().getFirst().getId();
        purchaseQuotationService.selectItems(
                sent.id(),
                new SelectQuotationItemsRequest(
                        false,
                        List.of(new SelectQuotationItemsRequest.QuotationItemSelection(
                                itemId, responseItemId, new BigDecimal("100")))));
        purchaseQuotationService.close(sent.id());

        assertThatThrownBy(() -> purchaseQuotationService.cancel(sent.id(), "Motivo qualquer"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
