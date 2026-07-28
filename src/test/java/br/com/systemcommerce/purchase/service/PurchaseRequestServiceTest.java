package br.com.systemcommerce.purchase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationResponse;
import br.com.systemcommerce.purchase.dto.PurchaseRequestConvertRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestItemApproval;
import br.com.systemcommerce.purchase.dto.PurchaseRequestItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestPartialApprovalRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestResponse;
import br.com.systemcommerce.purchase.entity.PurchaseRequest;
import br.com.systemcommerce.purchase.entity.PurchaseRequestItem;
import br.com.systemcommerce.purchase.mapper.PurchaseRequestMapper;
import br.com.systemcommerce.purchase.repository.PurchaseRequestRepository;
import br.com.systemcommerce.purchase.repository.PurchaseRequestStatusHistoryRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.document.DocumentConversionService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.supplier.repository.SupplierRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
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
class PurchaseRequestServiceTest {

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private PurchaseRequestStatusHistoryRepository statusHistoryRepository;

    @Mock
    private StorePurchaseRequestSequenceService storePurchaseRequestSequenceService;

    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Mock
    private WarehouseService warehouseService;

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
    private PurchaseQuotationService purchaseQuotationService;

    private PurchaseRequestService purchaseRequestService;

    private UUID userId;
    private UUID storeId;
    private Store store;
    private AtomicReference<PurchaseRequest> savedRequest;

    @BeforeEach
    void setUp() {
        purchaseRequestService = new PurchaseRequestService(
                purchaseRequestRepository,
                statusHistoryRepository,
                new PurchaseRequestMapper(),
                storePurchaseRequestSequenceService,
                storeAuthorizationEvaluator,
                warehouseService,
                productRepository,
                supplierRepository,
                userRepository,
                domainAuditService,
                documentConversionService,
                purchaseQuotationService);

        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        Organization org = new Organization();
        org.setId(UUID.randomUUID());

        store = new Store();
        store.setId(storeId);
        store.setCode("LJ01");
        store.setOrganization(org);

        savedRequest = new AtomicReference<>();

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, List.of(new SimpleGrantedAuthority("PURCHASE_REQUEST_CREATE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubPersistence() {
        when(storeAuthorizationEvaluator.assertCanAccess(userId, storeId)).thenReturn(store);
        when(storePurchaseRequestSequenceService.allocateNextRequestNumber(store)).thenReturn("SC-LJ01-000001");
        when(purchaseRequestRepository.save(any(PurchaseRequest.class))).thenAnswer(inv -> {
            PurchaseRequest r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            savedRequest.set(r);
            return r;
        });
        lenient()
                .when(purchaseRequestRepository.findDetailedById(any()))
                .thenAnswer(inv -> Optional.ofNullable(savedRequest.get()));
    }

    private PurchaseRequestResponse createDraftWithOneItem(BigDecimal quantity) {
        stubPersistence();
        PurchaseRequestCreateRequest request = new PurchaseRequestCreateRequest(
                storeId,
                null,
                "Manutenção",
                null,
                null,
                PurchaseRequest.Priority.NORMAL,
                null,
                "Necessário para produção",
                null,
                List.of(new PurchaseRequestItemRequest(
                        null, "Parafusos M6", quantity, "CX", null, null, null, null)));
        return purchaseRequestService.create(request);
    }

    @Test
    void shouldCreateDraftRequestWithSequentialNumber() {
        PurchaseRequestResponse response = createDraftWithOneItem(new BigDecimal("100"));

        assertThat(response.requestNumber()).isEqualTo("SC-LJ01-000001");
        assertThat(response.status()).isEqualTo(PurchaseRequest.PurchaseRequestStatus.DRAFT);
        assertThat(savedRequest.get().getItems()).hasSize(1);
        assertThat(savedRequest.get().getItems().getFirst().getQuantityRequested()).isEqualByComparingTo("100");
    }

    @Test
    void shouldSubmitAnalyzeAndApproveFully() {
        PurchaseRequestResponse draft = createDraftWithOneItem(new BigDecimal("50"));

        purchaseRequestService.submit(draft.id());
        assertThat(savedRequest.get().getStatus()).isEqualTo(PurchaseRequest.PurchaseRequestStatus.SUBMITTED);

        purchaseRequestService.analyze(draft.id());
        assertThat(savedRequest.get().getStatus()).isEqualTo(PurchaseRequest.PurchaseRequestStatus.UNDER_ANALYSIS);

        purchaseRequestService.approve(draft.id());
        assertThat(savedRequest.get().getStatus()).isEqualTo(PurchaseRequest.PurchaseRequestStatus.APPROVED);
        assertThat(savedRequest.get().getItems().getFirst().getQuantityApproved()).isEqualByComparingTo("50");
    }

    @Test
    void shouldPartiallyApproveWithPerItemQuantities() {
        PurchaseRequestResponse draft = createDraftWithOneItem(new BigDecimal("50"));
        purchaseRequestService.submit(draft.id());
        purchaseRequestService.analyze(draft.id());

        UUID itemId = savedRequest.get().getItems().getFirst().getId();
        purchaseRequestService.partiallyApprove(
                draft.id(),
                new PurchaseRequestPartialApprovalRequest(
                        List.of(new PurchaseRequestItemApproval(itemId, new BigDecimal("30"))), "Aprovado parcialmente"));

        assertThat(savedRequest.get().getStatus())
                .isEqualTo(PurchaseRequest.PurchaseRequestStatus.PARTIALLY_APPROVED);
        assertThat(savedRequest.get().getItems().getFirst().getQuantityApproved()).isEqualByComparingTo("30");
    }

    @Test
    void shouldRejectRequiringReason() {
        PurchaseRequestResponse draft = createDraftWithOneItem(new BigDecimal("20"));
        purchaseRequestService.submit(draft.id());
        purchaseRequestService.analyze(draft.id());

        assertThatThrownBy(() -> purchaseRequestService.reject(draft.id(), null))
                .isInstanceOf(BusinessRuleException.class);

        purchaseRequestService.reject(draft.id(), "Fora do orçamento");
        assertThat(savedRequest.get().getStatus()).isEqualTo(PurchaseRequest.PurchaseRequestStatus.REJECTED);
        assertThat(savedRequest.get().getRejectionReason()).isEqualTo("Fora do orçamento");
    }

    @Test
    void shouldConvertApprovedRequestToQuotationAndUpdateConvertedQuantity() {
        PurchaseRequestResponse draft = createDraftWithOneItem(new BigDecimal("50"));
        purchaseRequestService.submit(draft.id());
        purchaseRequestService.analyze(draft.id());
        purchaseRequestService.approve(draft.id());

        UUID quotationId = UUID.randomUUID();
        PurchaseQuotationResponse quotationResponse = new PurchaseQuotationResponse(
                quotationId,
                "CC-LJ01-000001",
                null,
                storeId,
                "LJ01",
                draft.id(),
                draft.requestNumber(),
                null,
                null,
                null,
                null,
                br.com.systemcommerce.purchase.entity.PurchaseQuotation.PurchaseQuotationStatus.DRAFT,
                br.com.systemcommerce.purchase.entity.PurchaseQuotation.SelectionCriteria.TOTAL_COST,
                false,
                null,
                null,
                List.of(),
                List.of(),
                true,
                true,
                false,
                false,
                false,
                false,
                true,
                0L,
                null,
                null);
        when(purchaseQuotationService.createFromRequest(any(), any(), any(), any(), any()))
                .thenReturn(quotationResponse);

        PurchaseQuotationResponse result =
                purchaseRequestService.convertToQuotation(draft.id(), new PurchaseRequestConvertRequest(
                        null, List.of(UUID.randomUUID()), null, "Converter tudo"));

        assertThat(result.id()).isEqualTo(quotationId);
        PurchaseRequestItem item = savedRequest.get().getItems().getFirst();
        assertThat(item.getQuantityConverted()).isEqualByComparingTo("50");
        assertThat(savedRequest.get().getStatus()).isEqualTo(PurchaseRequest.PurchaseRequestStatus.CONVERTED);
        verify(documentConversionService)
                .record(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
