package br.com.systemcommerce.sale.service;

import br.com.systemcommerce.commission.service.CommissionService;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.payment.validation.PaymentFinancialCalculator;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.pricing.repository.PriceTableRepository;
import br.com.systemcommerce.pricing.repository.ProductPriceRepository;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.config.SaleDiscountProperties;
import br.com.systemcommerce.sale.dto.SaleCancelRequest;
import br.com.systemcommerce.sale.dto.SaleChangeStoreRequest;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.dto.SaleCustomerRequest;
import br.com.systemcommerce.sale.dto.SaleDiscountRequest;
import br.com.systemcommerce.sale.dto.SaleFreightRequest;
import br.com.systemcommerce.sale.dto.SaleItemRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.dto.SaleSellerHistoryResponse;
import br.com.systemcommerce.sale.dto.SaleSellerRequest;
import br.com.systemcommerce.sale.dto.SaleStatusHistoryResponse;
import br.com.systemcommerce.sale.dto.StoreSaleSequenceResponse;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.sale.entity.SaleSellerHistory;
import br.com.systemcommerce.sale.entity.SaleStatusHistory;
import br.com.systemcommerce.sale.mapper.SaleMapper;
import br.com.systemcommerce.sale.repository.SaleItemRepository;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.sale.repository.SaleSellerHistoryRepository;
import br.com.systemcommerce.sale.repository.SaleStatusHistoryRepository;
import br.com.systemcommerce.sale.specification.SaleSpecifications;
import br.com.systemcommerce.sale.validation.SaleTotalsCalculator;
import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.seller.service.SellerService;
import br.com.systemcommerce.settings.entity.SystemSettingKeys;
import br.com.systemcommerce.settings.service.SystemSettingService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.storeproduct.dto.StoreProductResponse;
import br.com.systemcommerce.storeproduct.entity.SaleChannel;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseResponse;
import br.com.systemcommerce.seller.dto.SellerResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SaleStatusHistoryRepository statusHistoryRepository;
    private final SaleSellerHistoryRepository sellerHistoryRepository;
    private final SaleMapper saleMapper;
    private final CustomerService customerService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final SaleDiscountProperties discountProperties;
    private final StoreService storeService;
    private final WarehouseService warehouseService;
    private final SellerService sellerService;
    private final StoreProductService storeProductService;
    private final CommissionService commissionService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final StoreSaleSequenceService storeSaleSequenceService;
    private final SystemSettingService systemSettingService;
    private final PriceResolutionService priceResolutionService;
    private final PriceTableRepository priceTableRepository;
    private final ProductPriceRepository productPriceRepository;

    @Transactional(readOnly = true)
    public Page<SaleResponse> list(
            Sale.SaleStatus status,
            UUID storeId,
            UUID customerId,
            UUID sellerId,
            String saleNumber,
            Instant from,
            Instant to,
            String search,
            Sale.SaleChannel channel,
            Pageable pageable) {
        Collection<UUID> allowedStoreIds = resolveListStoreFilter(storeId, false);
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return saleRepository
                .findAll(
                        SaleSpecifications.withFilters(
                                status,
                                customerId,
                                sellerId,
                                saleNumber,
                                from,
                                to,
                                search,
                                channel,
                                storeId,
                                allowedStoreIds),
                        pageable)
                .map(sale -> saleMapper.toResponse(sale, List.of()));
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> listConsolidated(
            Sale.SaleStatus status,
            UUID customerId,
            UUID sellerId,
            String saleNumber,
            Instant from,
            Instant to,
            String search,
            Sale.SaleChannel channel,
            Pageable pageable) {
        assertConsolidatedAccess();
        return saleRepository
                .findAll(
                        SaleSpecifications.withFilters(
                                status,
                                customerId,
                                sellerId,
                                saleNumber,
                                from,
                                to,
                                search,
                                channel,
                                null,
                                null),
                        pageable)
                .map(sale -> saleMapper.toResponse(sale, List.of()));
    }

    @Transactional(readOnly = true)
    public StoreSaleSequenceResponse getStoreSequence(UUID storeId) {
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        Store store = storeService.getEntity(storeId);
        return storeSaleSequenceService.getSequence(storeId, store.getCode());
    }

    @Transactional(readOnly = true)
    public List<SellerResponse> listAuthorizedSellers(UUID storeId) {
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        return sellerService.listByStore(storeId);
    }

    @Transactional(readOnly = true)
    public Page<WarehouseResponse> listValidWarehouses(UUID storeId, Pageable pageable) {
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        return warehouseService.list(storeId, Warehouse.WarehouseStatus.ACTIVE, true, null, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<StoreProductResponse> listAvailableProducts(UUID storeId, Pageable pageable) {
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        return storeProductService.listProductsByStore(storeId, true, pageable);
    }

    @Transactional(readOnly = true)
    public SaleResponse getById(UUID id) {
        Sale sale = requireAccessibleSale(id);
        return toDetailedResponse(sale.getId());
    }

    @Transactional(readOnly = true)
    public List<SaleStatusHistoryResponse> statusHistory(UUID saleId) {
        requireAccessibleSale(saleId);
        return statusHistoryRepository.findBySaleIdOrderByChangedAtAsc(saleId).stream()
                .map(saleMapper::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleSellerHistoryResponse> sellerHistory(UUID saleId) {
        requireAccessibleSale(saleId);
        return sellerHistoryRepository.findBySaleIdOrderByCreatedAtAsc(saleId).stream()
                .map(saleMapper::toSellerHistoryResponse)
                .toList();
    }

    @Transactional
    public SaleResponse createDraft(SaleCreateRequest request) {
        UUID userId = CurrentUser.requireId();
        Store store = storeAuthorizationEvaluator.assertCanAccess(userId, request.storeId());
        if (!store.canRegisterSales()) {
            throw new BusinessRuleException("Loja não permite novas vendas");
        }

        Warehouse warehouse = warehouseService.requireUsable(request.warehouseId());
        if (!warehouse.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Depósito não pertence à loja informada");
        }
        if (!Boolean.TRUE.equals(warehouse.getAllowsSale())) {
            throw new BusinessRuleException("Depósito não autorizado para venda");
        }

        User operator = requireCurrentSeller();
        Sale sale = new Sale();
        sale.setSaleNumber(storeSaleSequenceService.allocateNextSaleNumber(store));
        sale.setOrganization(store.getOrganization());
        sale.setStore(store);
        sale.setWarehouse(warehouse);
        sale.setSeller(operator);
        sale.setSaleDate(Instant.now());
        sale.setStatus(Sale.SaleStatus.DRAFT);
        sale.setChannel(Sale.SaleChannel.ADMIN);
        sale.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        sale.setSubtotal(BigDecimal.ZERO);
        sale.setDiscountAmount(BigDecimal.ZERO);
        sale.setSurchargeAmount(BigDecimal.ZERO);
        sale.setFreightAmount(BigDecimal.ZERO);
        sale.setTotalAmount(BigDecimal.ZERO);

        if (request.priceTableId() != null) {
            sale.setPriceTable(resolvePriceTable(request.priceTableId(), store));
        }

        if (request.customerId() != null) {
            /* Venda exige cliente ACTIVE — BLOCKED nunca gera novo pedido/venda. */
            Customer customer = customerService.assertCanCreateOrder(request.customerId());
            sale.setCustomer(customer);
            sale.setCustomerNameSnapshot(customer.getName());
            sale.setCustomerDocumentSnapshot(customer.getDocument());
        }

        if (isSellerRequired(store, Sale.SaleChannel.ADMIN)) {
            if (request.sellerProfileId() == null) {
                throw new BusinessRuleException("Vendedor comercial é obrigatório para esta loja");
            }
            applySellerProfile(
                    sale, sellerService.requireAuthorizedForSale(request.sellerProfileId(), store.getId()));
        } else if (request.sellerProfileId() != null) {
            applySellerProfile(
                    sale, sellerService.requireAuthorizedForSale(request.sellerProfileId(), store.getId()));
        }

        Sale saved = saleRepository.save(sale);
        appendHistory(saved, null, Sale.SaleStatus.DRAFT, "Rascunho criado");
        domainAuditService.record(
                "Sale", saved.getId(), AuditLog.AuditAction.CREATE, null, snapshot(saved), "Venda rascunho criada");
        return toDetailedResponse(saved.getId());
    }

    @Transactional
    public SaleResponse changeStore(UUID saleId, SaleChangeStoreRequest request) {
        Sale sale = requireEditable(saleId);
        if (!saleItemRepository.findBySaleId(saleId).isEmpty()) {
            throw new BusinessRuleException("Somente rascunhos sem itens podem trocar de loja");
        }

        UUID userId = CurrentUser.requireId();
        Store newStore = storeAuthorizationEvaluator.assertCanAccess(userId, request.newStoreId());
        if (!newStore.canRegisterSales()) {
            throw new BusinessRuleException("Loja não permite novas vendas");
        }
        Warehouse newWarehouse = warehouseService.requireUsable(request.newWarehouseId());
        if (!newWarehouse.getStore().getId().equals(newStore.getId())) {
            throw new BusinessRuleException("Depósito não pertence à loja informada");
        }
        if (!Boolean.TRUE.equals(newWarehouse.getAllowsSale())) {
            throw new BusinessRuleException("Depósito não autorizado para venda");
        }

        Map<String, Object> before = snapshot(sale);
        sale.setStore(newStore);
        sale.setWarehouse(newWarehouse);
        sale.setOrganization(newStore.getOrganization());
        sale.setSaleNumber(storeSaleSequenceService.allocateNextSaleNumber(newStore));

        if (sale.getSellerProfile() != null) {
            SellerProfile profile = sellerService.requireAuthorizedForSale(
                    sale.getSellerProfile().getId(), newStore.getId());
            applySellerProfile(sale, profile);
        } else if (isSellerRequired(newStore, Sale.SaleChannel.ADMIN)) {
            throw new BusinessRuleException("Vendedor comercial é obrigatório na nova loja");
        }

        saleRepository.save(sale);
        domainAuditService.record(
                "Sale",
                saleId,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(sale),
                "Loja/depósito da venda alterados");
        return toDetailedResponse(saleId);
    }

    @Transactional
    public SaleResponse selectSeller(UUID saleId, SaleSellerRequest request) {
        assertAuthority("SALE_SELLER_SELECT");
        Sale sale = requireEditable(saleId);
        if (sale.getSellerProfile() != null) {
            throw new BusinessRuleException("Venda já possui vendedor; use alteração de vendedor");
        }
        return applySellerChange(sale, request, "Vendedor selecionado");
    }

    @Transactional
    public SaleResponse changeSeller(UUID saleId, SaleSellerRequest request) {
        assertAuthority("SALE_SELLER_CHANGE");
        Sale sale = requireEditable(saleId);
        if (sale.getSellerProfile() == null) {
            throw new BusinessRuleException("Venda sem vendedor; use seleção de vendedor");
        }
        return applySellerChange(sale, request, "Vendedor alterado");
    }

    @Transactional
    public SaleResponse correctSeller(UUID saleId, SaleSellerRequest request) {
        assertAuthority("SALE_SELLER_CORRECT");
        String reason = MoneyAndQuantityUtils.requireText(request.reason(), "Motivo da correção");
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        if (!sale.isConfirmedLike()) {
            throw new BusinessRuleException("Correção de vendedor aplica-se a vendas confirmadas");
        }
        UUID storeId = sale.getStore().getId();
        SellerProfile profile = sellerService.requireAuthorizedForSale(request.sellerProfileId(), storeId);
        recordSellerHistory(sale, profile, reason);
        applySellerProfile(sale, profile);
        saleRepository.save(sale);
        domainAuditService.record(
                "Sale",
                saleId,
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(sale),
                "Correção administrativa de vendedor: " + reason);
        return toDetailedResponse(saleId);
    }

    @Transactional
    public SaleResponse setCustomer(UUID saleId, SaleCustomerRequest request) {
        Sale sale = requireEditable(saleId);
        Customer customer = customerService.assertCanCreateOrder(request.customerId());
        Map<String, Object> before = snapshot(sale);
        sale.setCustomer(customer);
        sale.setCustomerNameSnapshot(customer.getName());
        sale.setCustomerDocumentSnapshot(customer.getDocument());
        saleRepository.save(sale);
        domainAuditService.record(
                "Sale", saleId, AuditLog.AuditAction.UPDATE, before, snapshot(sale), "Cliente definido");
        return toDetailedResponse(saleId);
    }

    @Transactional
    public SaleResponse clearCustomer(UUID saleId) {
        Sale sale = requireEditable(saleId);
        Map<String, Object> before = snapshot(sale);
        sale.setCustomer(null);
        saleRepository.save(sale);
        domainAuditService.record(
                "Sale", saleId, AuditLog.AuditAction.UPDATE, before, snapshot(sale), "Cliente removido");
        return toDetailedResponse(saleId);
    }

    @Transactional
    public SaleResponse applyDiscount(UUID saleId, SaleDiscountRequest request) {
        Sale sale = requireEditable(saleId);
        Map<String, Object> before = snapshot(sale);
        sale.setDiscountAmount(request.discountAmount());
        recalculateTotals(sale);
        saleRepository.save(sale);
        domainAuditService.record(
                "Sale", saleId, AuditLog.AuditAction.UPDATE, before, snapshot(sale), "Desconto aplicado");
        return toDetailedResponse(saleId);
    }

    @Transactional
    public SaleResponse applyFreight(UUID saleId, SaleFreightRequest request) {
        Sale sale = requireEditable(saleId);
        Map<String, Object> before = snapshot(sale);
        if (request.freightAmount() != null) {
            sale.setFreightAmount(request.freightAmount());
        }
        if (request.surchargeAmount() != null) {
            sale.setSurchargeAmount(request.surchargeAmount());
        }
        recalculateTotals(sale);
        saleRepository.save(sale);
        domainAuditService.record(
                "Sale", saleId, AuditLog.AuditAction.UPDATE, before, snapshot(sale), "Frete/acréscimo aplicado");
        return toDetailedResponse(saleId);
    }

    @Transactional
    public SaleResponse addItem(UUID saleId, SaleItemRequest request) {
        Sale sale = requireEditable(saleId);
        Product product = productService.requireUsableForSale(request.productId());
        assertSellableForErp(sale, product.getId());

        SaleItem existing = saleItemRepository
                .findBySaleIdAndProductId(saleId, product.getId())
                .orElse(null);

        if (existing != null) {
            BigDecimal newQty = existing.getQuantity().add(MoneyAndQuantityUtils.positiveQuantity(request.quantity()));
            applyItemValues(
                    sale,
                    existing,
                    product,
                    newQty,
                    request.unitPrice() != null ? request.unitPrice() : existing.getUnitPrice(),
                    request.discountAmount() != null ? request.discountAmount() : existing.getDiscountAmount(),
                    request.description());
            saleItemRepository.save(existing);
        } else {
            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            applyItemValues(
                    sale,
                    item,
                    product,
                    request.quantity(),
                    request.unitPrice(),
                    request.discountAmount(),
                    request.description());
            saleItemRepository.save(item);
        }

        recalculateAndSave(sale);
        return toDetailedResponse(saleId);
    }

    @Transactional
    public SaleResponse updateItem(UUID saleId, UUID itemId, SaleItemRequest request) {
        Sale sale = requireEditable(saleId);
        SaleItem item = saleItemRepository
                .findByIdAndSaleId(itemId, saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Item da venda", itemId));

        Product product = productService.requireUsableForSale(
                request.productId() != null ? request.productId() : item.getProduct().getId());
        assertSellableForErp(sale, product.getId());

        if (!product.getId().equals(item.getProduct().getId())) {
            saleItemRepository
                    .findBySaleIdAndProductId(saleId, product.getId())
                    .ifPresent(other -> {
                        if (!other.getId().equals(itemId)) {
                            throw new BusinessRuleException("Produto já existe em outro item desta venda");
                        }
                    });
            item.setProduct(product);
        }

        applyItemValues(
                sale,
                item,
                product,
                request.quantity(),
                request.unitPrice(),
                request.discountAmount(),
                request.description());
        saleItemRepository.save(item);
        recalculateAndSave(sale);
        return toDetailedResponse(saleId);
    }

    @Transactional
    public SaleResponse removeItem(UUID saleId, UUID itemId) {
        Sale sale = requireEditable(saleId);
        SaleItem item = saleItemRepository
                .findByIdAndSaleId(itemId, saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Item da venda", itemId));
        saleItemRepository.delete(item);
        recalculateAndSave(sale);
        return toDetailedResponse(saleId);
    }

    @Transactional
    public SaleResponse confirm(UUID saleId) {
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));

        if (sale.isConfirmedLike()) {
            return toDetailedResponse(saleId);
        }
        if (sale.isCancelled()) {
            throw new BusinessRuleException("Venda cancelada não pode ser confirmada");
        }
        if (sale.isSuspended()) {
            throw new BusinessRuleException("Recupere a venda suspensa antes de confirmar");
        }
        if (!sale.isDraft()) {
            throw new BusinessRuleException("Somente rascunhos podem ser confirmados");
        }

        if (sale.getStore() == null || sale.getWarehouse() == null) {
            throw new BusinessRuleException("Loja e depósito são obrigatórios para confirmar a venda");
        }

        if (sale.isPos()) {
            if (sale.getCashSession() == null || !sale.getCashSession().acceptsOperations()) {
                throw new BusinessRuleException("Sessão de caixa aberta é obrigatória para confirmar venda do PDV");
            }
        } else {
            if (sale.getCustomer() == null) {
                throw new BusinessRuleException("Informe o cliente antes de confirmar a venda");
            }
            customerService.assertCanCreateOrder(sale.getCustomer().getId());
        }

        assertSellerPresentIfRequired(sale);

        if (sale.getCustomer() != null) {
            customerService.assertCanCreateOrder(sale.getCustomer().getId());
        }

        List<SaleItem> items = saleItemRepository.findBySaleId(saleId);
        if (items.isEmpty()) {
            throw new BusinessRuleException("Venda deve possuir ao menos um item para confirmação");
        }

        UUID warehouseId = sale.getWarehouse().getId();
        for (SaleItem item : items) {
            assertSellableForErp(sale, item.getProduct().getId());
            assertStockAvailable(sale, item.getProduct(), item.getQuantity());
            inventoryService.registerSale(item.getProduct().getId(), warehouseId, item.getQuantity(), saleId);
        }

        Sale.SaleStatus from = sale.getStatus();
        sale.setStatus(Sale.SaleStatus.CONFIRMED);
        saleRepository.save(sale);
        appendHistory(sale, from, Sale.SaleStatus.CONFIRMED, "Venda confirmada");
        domainAuditService.record(
                "Sale",
                saleId,
                AuditLog.AuditAction.UPDATE,
                Map.of("status", from.name()),
                snapshot(sale),
                "Venda confirmada");
        return toDetailedResponse(saleId);
    }

    @Transactional
    public SaleResponse cancel(UUID saleId, SaleCancelRequest request) {
        String reason = MoneyAndQuantityUtils.requireText(request.reason(), "Motivo do cancelamento");
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));

        if (sale.isCancelled()) {
            return toDetailedResponse(saleId);
        }
        if (paymentRepository.hasConfirmedPayments(saleId)) {
            throw new BusinessRuleException(
                    "Venda com pagamentos confirmados não pode ser cancelada; cancele os pagamentos antes");
        }

        Sale.SaleStatus from = sale.getStatus();
        boolean restoreStock = sale.isConfirmedLike();

        if (restoreStock) {
            UUID warehouseId = sale.getWarehouse() != null ? sale.getWarehouse().getId() : null;
            for (SaleItem item : saleItemRepository.findBySaleId(saleId)) {
                inventoryService.registerSaleCancel(
                        item.getProduct().getId(), warehouseId, item.getQuantity(), saleId);
            }
        }

        sale.setStatus(Sale.SaleStatus.CANCELLED);
        saleRepository.save(sale);
        appendHistory(sale, from, Sale.SaleStatus.CANCELLED, reason);
        domainAuditService.record(
                "Sale",
                saleId,
                AuditLog.AuditAction.UPDATE,
                Map.of("status", from.name()),
                snapshot(sale),
                "Venda cancelada: " + reason);
        commissionService.reverseForSale(saleId);
        return toDetailedResponse(saleId);
    }

    @Transactional(readOnly = true)
    public Sale requireExists(UUID saleId) {
        return saleRepository
                .findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
    }

    @Transactional(readOnly = true)
    public Sale requirePayable(UUID saleId) {
        Sale sale = requireExists(saleId);
        if (!sale.canReceivePayment()) {
            throw new BusinessRuleException(
                    "Venda não pode receber pagamento no status atual (cancelada, suspensa, quitada ou rascunho administrativo)");
        }
        return sale;
    }

    @Transactional
    public Sale requirePayableForUpdate(UUID saleId) {
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        if (!sale.canReceivePayment()) {
            throw new BusinessRuleException(
                    "Venda não pode receber pagamento no status atual (cancelada, suspensa, quitada ou rascunho administrativo)");
        }
        return sale;
    }

    @Transactional
    public Sale requireForUpdate(UUID saleId) {
        return saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
    }

    @Transactional
    public void refreshFinancialStatusFromPayments(UUID saleId) {
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        if (!sale.isConfirmedLike()) {
            return;
        }

        BigDecimal confirmed = paymentRepository.sumConfirmedAmountBySaleId(saleId);
        BigDecimal due = PaymentFinancialCalculator.balanceDue(sale.getTotalAmount(), confirmed);

        Sale.SaleStatus from = sale.getStatus();
        Sale.SaleStatus to;
        if (confirmed.compareTo(BigDecimal.ZERO) <= 0) {
            to = Sale.SaleStatus.CONFIRMED;
        } else if (due.compareTo(BigDecimal.ZERO) <= 0) {
            to = Sale.SaleStatus.PAID;
        } else {
            to = Sale.SaleStatus.PARTIALLY_PAID;
        }

        if (from != to) {
            sale.setStatus(to);
            saleRepository.save(sale);
            appendHistory(sale, from, to, "Status financeiro atualizado pelos pagamentos");
            domainAuditService.record(
                    "Sale",
                    saleId,
                    AuditLog.AuditAction.UPDATE,
                    Map.of("status", from.name()),
                    snapshot(sale),
                    "Status financeiro: " + to.name());
        }
    }

    private SaleResponse applySellerChange(Sale sale, SaleSellerRequest request, String auditMessage) {
        UUID storeId = sale.getStore().getId();
        SellerProfile profile = sellerService.requireAuthorizedForSale(request.sellerProfileId(), storeId);
        recordSellerHistory(sale, profile, request.reason());
        applySellerProfile(sale, profile);
        saleRepository.save(sale);
        domainAuditService.record(
                "Sale",
                sale.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(sale),
                auditMessage);
        return toDetailedResponse(sale.getId());
    }

    private void recordSellerHistory(Sale sale, SellerProfile newProfile, String reason) {
        SaleSellerHistory history = new SaleSellerHistory();
        history.setSale(sale);
        if (sale.getSellerProfile() != null) {
            history.setPreviousSellerProfile(sale.getSellerProfile());
            history.setPreviousSellerCode(
                    StringUtils.hasText(sale.getSellerCodeSnapshot())
                            ? sale.getSellerCodeSnapshot()
                            : sale.getSellerProfile().getSellerCode());
            history.setPreviousSellerName(
                    StringUtils.hasText(sale.getSellerNameSnapshot())
                            ? sale.getSellerNameSnapshot()
                            : sale.getSellerProfile().getEmployee().getName());
        }
        history.setNewSellerProfile(newProfile);
        history.setNewSellerCode(newProfile.getSellerCode());
        history.setNewSellerName(newProfile.getEmployee().getName());
        history.setReason(MoneyAndQuantityUtils.blankToNull(reason));
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        CurrentUser.id().ifPresent(history::setCreatedBy);
        sellerHistoryRepository.save(history);
    }

    private void applySellerProfile(Sale sale, SellerProfile profile) {
        sale.setSellerProfile(profile);
        sale.setSellerCodeSnapshot(profile.getSellerCode());
        sale.setSellerNameSnapshot(profile.getEmployee().getName());
        if (profile.getSupervisor() != null && profile.getSupervisor().getUser() != null) {
            sale.setSupervisor(profile.getSupervisor().getUser());
        } else {
            sale.setSupervisor(null);
        }
    }

    private PriceTable resolvePriceTable(UUID priceTableId, Store store) {
        PriceTable table = priceTableRepository
                .findById(priceTableId)
                .orElseThrow(() -> new ResourceNotFoundException("Tabela de preços", priceTableId));
        if (!Boolean.TRUE.equals(table.getActive())) {
            throw new BusinessRuleException("Tabela de preços inativa");
        }
        return table;
    }

    private boolean isSellerRequired(Store store, Sale.SaleChannel channel) {
        if (store == null) {
            return false;
        }
        boolean storeRequires = channel == Sale.SaleChannel.POS ? store.isRequireSellerPos() : store.isRequireSellerAdmin();
        if (storeRequires) {
            return true;
        }
        return systemSettingService.getEffectiveBoolean(
                SystemSettingKeys.REQUIRE_SELLER,
                store.getOrganization().getId(),
                null,
                store.getId(),
                null,
                false);
    }

    private void assertSellerPresentIfRequired(Sale sale) {
        if (isSellerRequired(sale.getStore(), sale.getChannel()) && sale.getSellerProfile() == null) {
            throw new BusinessRuleException("Vendedor comercial é obrigatório para confirmar a venda");
        }
    }

    private Collection<UUID> resolveListStoreFilter(UUID storeId, boolean consolidated) {
        if (storeId != null || consolidated) {
            return null;
        }
        if (storeAuthorizationEvaluator.hasGlobalAccess()) {
            return null;
        }
        if (SecurityAuthorities.hasAuthority("STORE_CONSOLIDATED_READ")) {
            return null;
        }
        return storeAuthorizationEvaluator.listEffectiveAccess(CurrentUser.requireId()).stream()
                .map(a -> a.getStore().getId())
                .toList();
    }

    private void assertConsolidatedAccess() {
        if (!storeAuthorizationEvaluator.hasGlobalAccess()
                && !SecurityAuthorities.hasAuthority("STORE_CONSOLIDATED_READ")) {
            throw new BusinessRuleException("Sem permissão para listagem consolidada de vendas");
        }
    }

    private void assertAuthority(String authority) {
        if (!SecurityAuthorities.hasAuthority(authority)) {
            throw new BusinessRuleException("Sem permissão: " + authority);
        }
    }

    private void applyItemValues(
            Sale sale,
            SaleItem item,
            Product product,
            BigDecimal quantity,
            BigDecimal requestedUnitPrice,
            BigDecimal requestedDiscount,
            String description) {
        BigDecimal unitPrice;
        if (requestedUnitPrice != null) {
            unitPrice = MoneyAndQuantityUtils.money(requestedUnitPrice);
        } else {
            UUID storeId = sale.getStore() != null ? sale.getStore().getId() : null;
            var resolved = priceResolutionService.resolve(
                    product.getId(), storeId, quantity, Instant.now(), PriceChannel.ERP);
            unitPrice = resolved.unitPrice();
            item.setPriceSource(resolved.priceSource());
            item.setPriceTable(
                    resolved.priceTableId() != null
                            ? priceTableRepository.getReferenceById(resolved.priceTableId())
                            : null);
            item.setProductPrice(
                    resolved.productPriceId() != null
                            ? productPriceRepository.getReferenceById(resolved.productPriceId())
                            : null);
        }
        var totals = SaleTotalsCalculator.calculateLine(quantity, unitPrice, requestedDiscount);
        item.setQuantity(MoneyAndQuantityUtils.positiveQuantity(quantity));
        item.setUnitPrice(unitPrice);
        item.setDiscountAmount(totals.discountAmount());
        item.setLineSubtotal(totals.lineSubtotal());
        item.setLineTotal(totals.lineTotal());
        String desc = MoneyAndQuantityUtils.blankToNull(description);
        item.setDescription(desc != null ? desc : product.getName());
    }

    private void assertStockAvailable(Sale sale, Product product, BigDecimal requiredQty) {
        if (Boolean.TRUE.equals(product.getAllowNegativeStock())) {
            return;
        }
        UUID warehouseId = sale.getWarehouse().getId();
        BigDecimal available = inventoryService.availableQuantity(product.getId(), warehouseId);
        if (requiredQty.compareTo(available) > 0) {
            throw new BusinessRuleException(
                    "Estoque insuficiente no depósito da venda (disponível=" + available + ")");
        }
    }

    private void recalculateAndSave(Sale sale) {
        recalculateTotals(sale);
        saleRepository.save(sale);
    }

    private void recalculateTotals(Sale sale) {
        List<SaleItem> items = saleItemRepository.findBySaleId(sale.getId());
        BigDecimal itemsSubtotal = items.stream()
                .map(SaleItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totals = SaleTotalsCalculator.calculateSale(
                itemsSubtotal,
                sale.getDiscountAmount(),
                sale.getSurchargeAmount(),
                sale.getFreightAmount(),
                discountProperties);
        sale.setSubtotal(totals.subtotal());
        sale.setDiscountAmount(totals.discountAmount());
        sale.setSurchargeAmount(totals.surchargeAmount());
        sale.setFreightAmount(totals.freightAmount());
        sale.setTotalAmount(totals.totalAmount());
    }

    private Sale requireEditable(UUID saleId) {
        Sale sale = saleRepository
                .findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        if (!sale.isEditable()) {
            throw new BusinessRuleException("Venda não pode ter itens ou valores alterados no status atual");
        }
        return sale;
    }

    private SaleResponse toDetailedResponse(UUID id) {
        Sale sale = saleRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", id));
        List<SaleItem> items = saleItemRepository.findBySaleId(id);
        Map<UUID, BigDecimal> stocks = Map.of();
        if (sale.getWarehouse() != null && !items.isEmpty()) {
            stocks = new LinkedHashMap<>();
            UUID warehouseId = sale.getWarehouse().getId();
            for (SaleItem item : items) {
                UUID productId = item.getProduct().getId();
                if (!stocks.containsKey(productId)) {
                    stocks.put(productId, inventoryService.availableQuantity(productId, warehouseId));
                }
            }
        }
        return saleMapper.toResponse(sale, items, stocks);
    }

    private Sale requireAccessibleSale(UUID id) {
        Sale sale = saleRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", id));
        if (sale.getStore() != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), sale.getStore().getId());
        }
        return sale;
    }

    private void ensureExists(UUID id) {
        if (!saleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Venda", id);
        }
    }

    private void assertSellableForErp(Sale sale, UUID productId) {
        UUID storeId = sale.getStore() != null ? sale.getStore().getId() : null;
        SaleChannel channel = sale.isPos() ? SaleChannel.POS : SaleChannel.ERP;
        storeProductService.assertSellable(productId, storeId, channel);
    }

    private User requireCurrentSeller() {
        UUID userId = CurrentUser.requireId();
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void appendHistory(Sale sale, Sale.SaleStatus from, Sale.SaleStatus to, String reason) {
        SaleStatusHistory history = new SaleStatusHistory();
        history.setSale(sale);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setReason(reason);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private Map<String, Object> snapshot(Sale sale) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("saleNumber", sale.getSaleNumber());
        map.put("status", sale.getStatus());
        map.put("storeId", sale.getStore() != null ? sale.getStore().getId() : null);
        map.put("warehouseId", sale.getWarehouse() != null ? sale.getWarehouse().getId() : null);
        map.put("organizationId", sale.getOrganization() != null ? sale.getOrganization().getId() : null);
        map.put("sellerProfileId", sale.getSellerProfile() != null ? sale.getSellerProfile().getId() : null);
        map.put("customerId", sale.getCustomer() != null ? sale.getCustomer().getId() : null);
        map.put("subtotal", sale.getSubtotal());
        map.put("discountAmount", sale.getDiscountAmount());
        map.put("surchargeAmount", sale.getSurchargeAmount());
        map.put("freightAmount", sale.getFreightAmount());
        map.put("totalAmount", sale.getTotalAmount());
        return map;
    }
}
