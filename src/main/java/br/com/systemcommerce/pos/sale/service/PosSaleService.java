package br.com.systemcommerce.pos.sale.service;

import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.audit.PosAuditContext;
import br.com.systemcommerce.pos.audit.PosAuditContexts;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByBarcodeRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByProductIdRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddBySkuRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleCustomerRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleDiscardRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleHeaderDiscountRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleItemDiscountRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleQuantityRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleResumeRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleStartRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleSurchargeRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleSuspendRequest;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleResumeRequest;
import br.com.systemcommerce.pos.settings.entity.PosSettingKeys;
import br.com.systemcommerce.pos.settings.service.PosSettingService;
import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.seller.service.SellerService;
import br.com.systemcommerce.settings.entity.SystemSettingKeys;
import br.com.systemcommerce.settings.service.SystemSettingService;
import br.com.systemcommerce.storeproduct.entity.SaleChannel;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pricing.repository.PriceTableRepository;
import br.com.systemcommerce.pricing.repository.ProductPriceRepository;
import br.com.systemcommerce.pricing.service.DiscountLimitService;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.config.SaleDiscountProperties;
import br.com.systemcommerce.sale.dto.SaleCancelRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.sale.entity.SaleStatusHistory;
import br.com.systemcommerce.sale.mapper.SaleMapper;
import br.com.systemcommerce.sale.repository.SaleItemRepository;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.sale.repository.SaleStatusHistoryRepository;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.sale.service.StoreSaleSequenceService;
import br.com.systemcommerce.sale.validation.SaleTotalsCalculator;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PosSaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SaleStatusHistoryRepository statusHistoryRepository;
    private final SaleMapper saleMapper;
    private final SaleService saleService;
    private final CashSessionService cashSessionService;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final CustomerService customerService;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final PosAuditService posAuditService;
    private final SaleDiscountProperties saleDiscountProperties;
    private final PriceResolutionService priceResolutionService;
    private final DiscountLimitService discountLimitService;
    private final PriceTableRepository priceTableRepository;
    private final ProductPriceRepository productPriceRepository;
    private final PosSuspendedSaleService posSuspendedSaleService;
    private final PosSettingService posSettingService;
    private final StoreService storeService;
    private final SellerService sellerService;
    private final StoreProductService storeProductService;
    private final StoreSaleSequenceService storeSaleSequenceService;
    private final SystemSettingService systemSettingService;

    @Transactional
    public SaleResponse start(PosSaleStartRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_CREATE");
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = saleRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return toSummary(existing.get().getId());
            }
        }

        CashSession session = cashSessionService.requireOpenSession(request.cashSessionId());
        assertOperatorOwnsSession(session);
        storeService.requireAllowsSales(session.getStore().getId());
        PosTerminal terminal = session.getTerminal();
        if (!terminal.isEligibleToOpenCashSession()) {
            throw new BusinessRuleException("Terminal não está apto para venda no PDV");
        }

        User operator = requireCurrentUser();
        var drafts = saleRepository.findCurrentPosDrafts(terminal.getId(), operator.getId(), session.getId());
        if (!drafts.isEmpty()) {
            return toSummary(drafts.getFirst().getId());
        }

        Sale sale = new Sale();
        sale.setSaleNumber(storeSaleSequenceService.allocateNextSaleNumber(session.getStore()));
        sale.setOrganization(session.getStore().getOrganization());
        sale.setSeller(operator);
        sale.setSaleDate(Instant.now());
        sale.setStatus(Sale.SaleStatus.DRAFT);
        sale.setChannel(Sale.SaleChannel.POS);
        sale.setStore(session.getStore());
        sale.setTerminal(terminal);
        sale.setCashSession(session);
        sale.setWarehouse(terminal.getWarehouse());

        UUID storeId = session.getStore().getId();
        UUID sellerProfileId = request.sellerProfileId();
        if (sellerProfileId == null && terminal.getDefaultSellerProfile() != null) {
            sellerProfileId = terminal.getDefaultSellerProfile().getId();
        }
        if (sellerProfileId != null) {
            applySellerProfile(sale, sellerService.requireAuthorizedForSale(sellerProfileId, storeId));
        } else if (isSellerRequired(session.getStore())) {
            throw new BusinessRuleException("Vendedor comercial é obrigatório para venda no PDV");
        }
        sale.setSubtotal(BigDecimal.ZERO);
        sale.setDiscountAmount(BigDecimal.ZERO);
        sale.setSurchargeAmount(BigDecimal.ZERO);
        sale.setFreightAmount(BigDecimal.ZERO);
        sale.setTotalAmount(BigDecimal.ZERO);
        if (StringUtils.hasText(idempotencyKey)) {
            sale.setIdempotencyKey(idempotencyKey.trim());
        }

        try {
            Sale saved = saleRepository.saveAndFlush(sale);
            appendHistory(saved, null, Sale.SaleStatus.DRAFT, "Venda PDV iniciada");
            domainAuditService.record(
                    "POS",
                    "Sale",
                    saved.getId(),
                    AuditLog.AuditAction.CREATE,
                    null,
                    snapshot(saved),
                    "Venda PDV iniciada");
            return toSummary(saved.getId());
        } catch (DataIntegrityViolationException ex) {
            if (StringUtils.hasText(idempotencyKey)) {
                return saleRepository
                        .findByIdempotencyKey(idempotencyKey.trim())
                        .map(s -> toSummary(s.getId()))
                        .orElseThrow(() -> new ConflictException("Conflito ao iniciar venda do PDV"));
            }
            throw new ConflictException("Conflito ao iniciar venda do PDV");
        }
    }

    @Transactional(readOnly = true)
    public SaleResponse currentByTerminal(UUID terminalId) {
        assertPermission("POS_SALE_CREATE");
        var sessionResponse = cashSessionService.getCurrent(terminalId);
        CashSession session = cashSessionService.requireOpenSession(sessionResponse.id());
        assertOperatorOwnsSession(session);
        var drafts = saleRepository.findCurrentPosDrafts(terminalId, CurrentUser.requireId(), session.getId());
        if (drafts.isEmpty()) {
            throw new ResourceNotFoundException("Venda atual do PDV", terminalId);
        }
        return toSummary(drafts.getFirst().getId());
    }

    @Transactional(readOnly = true)
    public SaleResponse summary(UUID saleId) {
        requirePosSale(saleId);
        if (!SecurityAuthorities.hasAuthority("POS_SALE_CREATE")
                && !SecurityAuthorities.hasAuthority("SALE_READ")) {
            throw new BusinessRuleException("Sem permissão para consultar venda do PDV");
        }
        return toSummary(saleId);
    }

    @Transactional
    public SaleResponse addByBarcode(UUID saleId, PosSaleAddByBarcodeRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_CREATE");
        Product product = resolveByBarcode(request.barcode());
        return addProduct(saleId, product, request.quantity(), request.expectedVersion(), idempotencyKey);
    }

    @Transactional
    public SaleResponse addBySku(UUID saleId, PosSaleAddBySkuRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_CREATE");
        Product product = productRepository
                .findBySkuIgnoreCase(request.sku().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Produto (SKU)", request.sku()));
        if (!product.isUsableForSale()) {
            throw new BusinessRuleException("Produto inativo não pode entrar em uma nova venda");
        }
        return addProduct(saleId, product, request.quantity(), request.expectedVersion(), idempotencyKey);
    }

    @Transactional
    public SaleResponse addByProductId(UUID saleId, PosSaleAddByProductIdRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_CREATE");
        Product product = productRepository
                .findDetailedById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));
        return addProduct(saleId, product, request.quantity(), request.expectedVersion(), idempotencyKey);
    }

    @Transactional
    public SaleResponse updateQuantity(
            UUID saleId, UUID itemId, PosSaleQuantityRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_CREATE");
        Sale sale = lockPosEditable(saleId, request.expectedVersion(), idempotencyKey);
        if (sale == null) {
            return toSummary(saleId);
        }
        SaleItem item = requireItem(saleId, itemId);
        Product product = item.getProduct();
        storeProductService.assertSellable(
                product.getId(), sale.getStore() != null ? sale.getStore().getId() : null, SaleChannel.POS);
        BigDecimal qty = MoneyAndQuantityUtils.positiveQuantity(request.quantity());
        assertStockAvailable(sale, product, qty);
        applyResolvedPrice(sale, item, product, qty, item.getDiscountAmount());
        saleItemRepository.save(item);
        recalculate(sale);
        finishOperation(sale, idempotencyKey, "Quantidade alterada");
        return toSummary(saleId);
    }

    @Transactional
    public SaleResponse removeItem(UUID saleId, UUID itemId, Long expectedVersion, String idempotencyKey) {
        assertPermission("POS_SALE_ITEM_REMOVE");
        Sale sale = lockPosEditable(saleId, expectedVersion, idempotencyKey);
        if (sale == null) {
            return toSummary(saleId);
        }
        SaleItem item = requireItem(saleId, itemId);
        saleItemRepository.delete(item);
        recalculate(sale);
        finishOperation(sale, idempotencyKey, "Item removido");
        return toSummary(saleId);
    }

    @Transactional
    public SaleResponse cancelItem(UUID saleId, UUID itemId, Long expectedVersion, String idempotencyKey) {
        return removeItem(saleId, itemId, expectedVersion, idempotencyKey);
    }

    @Transactional
    public SaleResponse identifyCustomer(UUID saleId, PosSaleCustomerRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_CREATE");
        Sale sale = lockPosEditable(saleId, request.expectedVersion(), idempotencyKey);
        if (sale == null) {
            return toSummary(saleId);
        }
        var customer = customerService.assertCanCreateOrder(request.customerId());
        sale.setCustomer(customer);
        sale.setCustomerNameSnapshot(customer.getName());
        sale.setCustomerDocumentSnapshot(customer.getDocument());
        finishOperation(sale, idempotencyKey, "Cliente identificado");
        return toSummary(saleId);
    }

    @Transactional
    public SaleResponse clearCustomer(UUID saleId, Long expectedVersion, String idempotencyKey) {
        assertPermission("POS_SALE_CREATE");
        Sale sale = lockPosEditable(saleId, expectedVersion, idempotencyKey);
        if (sale == null) {
            return toSummary(saleId);
        }
        sale.setCustomer(null);
        finishOperation(sale, idempotencyKey, "Identificação de cliente removida");
        return toSummary(saleId);
    }

    @Transactional
    public SaleResponse itemDiscount(
            UUID saleId, UUID itemId, PosSaleItemDiscountRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_DISCOUNT");
        Sale sale = lockPosEditable(saleId, request.expectedVersion(), idempotencyKey);
        if (sale == null) {
            return toSummary(saleId);
        }
        SaleItem item = requireItem(saleId, itemId);
        Product product = item.getProduct();
        storeProductService.assertSellable(
                product.getId(), sale.getStore() != null ? sale.getStore().getId() : null, SaleChannel.POS);
        BigDecimal discount = MoneyAndQuantityUtils.money(request.discountAmount());
        BigDecimal lineBase = item.getUnitPrice()
                .multiply(item.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
        User authorizer = discountLimitService.assertDiscountAllowed(
                saleId, itemId, product, lineBase, discount, request.authorizedById());
        applyResolvedPrice(sale, item, product, item.getQuantity(), discount);
        item.setDiscountAuthorizedBy(authorizer);
        saleItemRepository.save(item);
        recalculate(sale);
        finishOperation(sale, idempotencyKey, "Desconto de item aplicado", authorizer != null ? authorizer.getId() : null);
        return toSummary(saleId);
    }

    @Transactional
    public SaleResponse headerDiscount(UUID saleId, PosSaleHeaderDiscountRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_DISCOUNT");
        Sale sale = lockPosEditable(saleId, request.expectedVersion(), idempotencyKey);
        if (sale == null) {
            return toSummary(saleId);
        }
        BigDecimal discount = MoneyAndQuantityUtils.money(request.discountAmount());
        User authorizer = discountLimitService.assertDiscountAllowed(
                saleId, null, null, sale.getSubtotal(), discount, request.authorizedById());
        sale.setDiscountAmount(discount);
        sale.setDiscountAuthorizedBy(authorizer);
        recalculate(sale);
        finishOperation(sale, idempotencyKey, "Desconto geral aplicado", authorizer != null ? authorizer.getId() : null);
        return toSummary(saleId);
    }

    @Transactional
    public SaleResponse applySurcharge(UUID saleId, PosSaleSurchargeRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_CREATE");
        Sale sale = lockPosEditable(saleId, request.expectedVersion(), idempotencyKey);
        if (sale == null) {
            return toSummary(saleId);
        }
        sale.setSurchargeAmount(MoneyAndQuantityUtils.money(request.surchargeAmount()));
        recalculate(sale);
        finishOperation(sale, idempotencyKey, "Acréscimo aplicado");
        return toSummary(saleId);
    }

    @Transactional
    public SaleResponse suspend(UUID saleId, PosSaleSuspendRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_SUSPEND");
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        requirePosSale(sale);
        if (matchesIdempotency(sale, idempotencyKey)) {
            return toSummary(saleId);
        }
        assertVersion(sale, request != null ? request.expectedVersion() : null);
        if (!sale.isDraft()) {
            throw new BusinessRuleException("Somente rascunho pode ser suspenso");
        }
        assertOperatorOwnsSale(sale);
        if (sale.getCashSession() == null || !sale.getCashSession().acceptsOperations()) {
            throw new BusinessRuleException("Sessão de caixa aberta é obrigatória");
        }
        UUID storeId = sale.getStore() != null ? sale.getStore().getId() : null;
        UUID terminalId = sale.getTerminal() != null ? sale.getTerminal().getId() : null;
        if (!posSettingService.getEffectiveBoolean(
                PosSettingKeys.ALLOW_SUSPENDED_SALE, storeId, terminalId, true)) {
            throw new BusinessRuleException("Suspensão de venda desabilitada nas configurações do PDV");
        }

        Sale.SaleStatus from = sale.getStatus();
        Instant now = Instant.now();
        User operator = requireCurrentUser();
        sale.setStatus(Sale.SaleStatus.SUSPENDED);
        sale.setSuspendedAt(now);
        sale.setSuspendReason(MoneyAndQuantityUtils.blankToNull(request != null ? request.reason() : null));
        posSuspendedSaleService.applySuspensionMetadata(sale, operator, now);
        finishOperation(sale, idempotencyKey, null);
        appendHistory(
                sale,
                from,
                Sale.SaleStatus.SUSPENDED,
                sale.getSuspendReason() != null ? sale.getSuspendReason() : "Venda suspensa");
        domainAuditService.record(
                "POS",
                "Sale",
                saleId,
                AuditLog.AuditAction.STATUS_CHANGE,
                Map.of("status", from.name()),
                snapshot(sale),
                "Venda PDV suspensa (sem baixa definitiva de estoque)");
        posAuditService.success(
                PosAuditEventCode.SALE_SUSPEND,
                PosAuditContexts.fromSale(sale)
                        .entity("Sale", saleId)
                        .action(AuditLog.AuditAction.STATUS_CHANGE)
                        .before(Map.of("status", from.name()))
                        .after(snapshot(sale))
                        .details("Venda PDV suspensa (sem baixa definitiva de estoque)")
                        .build());
        return toSummary(saleId);
    }

    @Transactional
    public SaleResponse resume(UUID saleId, PosSaleResumeRequest request, String idempotencyKey) {
        return posSuspendedSaleService.resume(
                saleId,
                new SuspendedSaleResumeRequest(request.cashSessionId(), request.expectedVersion(), false),
                idempotencyKey);
    }

    @Transactional
    public SaleResponse discard(UUID saleId, PosSaleDiscardRequest request, String idempotencyKey) {
        assertPermission("POS_SALE_CANCEL");
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        requirePosSale(sale);
        if (matchesIdempotency(sale, idempotencyKey)) {
            return toSummary(saleId);
        }
        assertVersion(sale, request.expectedVersion());
        if (!sale.isDraft() && !sale.isSuspended()) {
            throw new BusinessRuleException("Somente rascunho ou suspensa pode ser descartada");
        }
        assertOperatorOwnsSale(sale);
        if (StringUtils.hasText(idempotencyKey)) {
            sale.setLastOperationIdempotencyKey(idempotencyKey.trim());
            saleRepository.save(sale);
        }
        return saleService.cancel(saleId, new SaleCancelRequest(request.reason()));
    }

    private SaleResponse addProduct(
            UUID saleId, Product product, BigDecimal rawQty, Long expectedVersion, String idempotencyKey) {
        Sale sale = lockPosEditable(saleId, expectedVersion, idempotencyKey);
        if (sale == null) {
            return toSummary(saleId);
        }
        UUID storeId = sale.getStore() != null ? sale.getStore().getId() : null;
        storeProductService.assertSellable(product.getId(), storeId, SaleChannel.POS);
        BigDecimal qty = MoneyAndQuantityUtils.positiveQuantity(rawQty);

        SaleItem existing = saleItemRepository.findBySaleIdAndProductId(saleId, product.getId()).orElse(null);
        BigDecimal targetQty = existing != null ? existing.getQuantity().add(qty) : qty;
        assertStockAvailable(sale, product, targetQty);

        if (existing != null) {
            applyResolvedPrice(sale, existing, product, targetQty, existing.getDiscountAmount());
            saleItemRepository.save(existing);
        } else {
            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            applyResolvedPrice(sale, item, product, qty, BigDecimal.ZERO);
            saleItemRepository.save(item);
        }
        recalculate(sale);
        finishOperation(sale, idempotencyKey, "Item incluído");
        return toSummary(saleId);
    }

    private Product resolveByBarcode(String rawBarcode) {
        String barcode = MoneyAndQuantityUtils.requireText(rawBarcode, "Código de barras");
        List<Product> matches = productRepository.findAllByBarcode(barcode);
        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("Produto (código de barras)", barcode);
        }
        if (matches.size() > 1) {
            throw new ConflictException("Código de barras identifica mais de um produto");
        }
        Product product = matches.getFirst();
        if (!product.isUsableForSale()) {
            throw new BusinessRuleException("Produto inativo não pode entrar em uma nova venda");
        }
        return product;
    }

    private void applyResolvedPrice(
            Sale sale, SaleItem item, Product product, BigDecimal quantity, BigDecimal discount) {
        UUID storeId = sale.getStore() != null ? sale.getStore().getId() : null;
        var resolved = priceResolutionService.resolve(
                product.getId(), storeId, quantity, Instant.now(), br.com.systemcommerce.pricing.entity.PriceChannel.POS);
        var totals = SaleTotalsCalculator.calculateLine(quantity, resolved.unitPrice(), discount);
        item.setQuantity(MoneyAndQuantityUtils.positiveQuantity(quantity));
        item.setUnitPrice(resolved.unitPrice());
        item.setDiscountAmount(totals.discountAmount());
        item.setLineSubtotal(totals.lineSubtotal());
        item.setLineTotal(totals.lineTotal());
        item.setDescription(product.getName());
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

    private void assertStockAvailable(Sale sale, Product product, BigDecimal requiredQty) {
        if (Boolean.TRUE.equals(product.getAllowNegativeStock())) {
            return;
        }
        UUID warehouseId = sale.getWarehouse().getId();
        BigDecimal available = inventoryService.availableQuantity(product.getId(), warehouseId);
        if (requiredQty.compareTo(available) > 0) {
            throw new BusinessRuleException(
                    "Estoque insuficiente no depósito do terminal (disponível=" + available + ")");
        }
    }

    private Sale lockPosEditable(UUID saleId, Long expectedVersion, String idempotencyKey) {
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        requirePosSale(sale);
        if (matchesIdempotency(sale, idempotencyKey)) {
            return null;
        }
        assertVersion(sale, expectedVersion);
        if (!sale.isEditable()) {
            throw new BusinessRuleException("Venda não pode ser alterada no status atual");
        }
        assertOperatorOwnsSale(sale);
        if (sale.getCashSession() == null || !sale.getCashSession().acceptsOperations()) {
            throw new BusinessRuleException("Sessão de caixa aberta é obrigatória para operar a venda");
        }
        posSuspendedSaleService.assertAndRefreshEditLock(
                sale, sale.getCashSession(), requireCurrentUser(), Instant.now());
        return sale;
    }

    private void assertVersion(Sale sale, Long expectedVersion) {
        if (expectedVersion == null) {
            return;
        }
        if (!expectedVersion.equals(sale.getVersion())) {
            throw new ConflictException("Versão da venda desatualizada; recarregue o resumo oficial");
        }
    }

    private boolean matchesIdempotency(Sale sale, String idempotencyKey) {
        return StringUtils.hasText(idempotencyKey)
                && idempotencyKey.trim().equals(sale.getLastOperationIdempotencyKey());
    }

    private void finishOperation(Sale sale, String idempotencyKey, String details) {
        finishOperation(sale, idempotencyKey, details, null);
    }

    private void finishOperation(Sale sale, String idempotencyKey, String details, UUID authorizedById) {
        if (StringUtils.hasText(idempotencyKey)) {
            sale.setLastOperationIdempotencyKey(idempotencyKey.trim());
        }
        try {
            saleRepository.saveAndFlush(sale);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("Conflito de concorrência na venda; recarregue o resumo oficial");
        }
        if (details != null) {
            Map<String, Object> after = snapshot(sale);
            domainAuditService.record(
                    "POS", "Sale", sale.getId(), AuditLog.AuditAction.UPDATE, null, after, details);
            PosAuditEventCode event = mapSaleOperationEvent(details);
            if (event != null) {
                posAuditService.success(
                        event,
                        PosAuditContexts.fromSale(sale)
                                .authorizedById(authorizedById)
                                .entity("Sale", sale.getId())
                                .action(AuditLog.AuditAction.UPDATE)
                                .after(after)
                                .details(details)
                                .build());
            }
        }
    }

    private static PosAuditEventCode mapSaleOperationEvent(String details) {
        return switch (details) {
            case "Item incluído" -> PosAuditEventCode.SALE_ITEM_ADD;
            case "Item removido" -> PosAuditEventCode.SALE_ITEM_REMOVE;
            case "Quantidade alterada" -> PosAuditEventCode.SALE_ITEM_QTY_CHANGE;
            case "Desconto de item aplicado", "Desconto geral aplicado" -> PosAuditEventCode.DISCOUNT_APPROVED;
            default -> null;
        };
    }

    private void recalculate(Sale sale) {
        List<SaleItem> items = saleItemRepository.findBySaleId(sale.getId());
        BigDecimal itemsSubtotal = items.stream()
                .map(SaleItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totals = SaleTotalsCalculator.calculateSale(
                itemsSubtotal,
                sale.getDiscountAmount(),
                sale.getSurchargeAmount(),
                sale.getFreightAmount(),
                saleDiscountProperties);
        sale.setSubtotal(totals.subtotal());
        sale.setDiscountAmount(totals.discountAmount());
        sale.setSurchargeAmount(totals.surchargeAmount());
        sale.setFreightAmount(totals.freightAmount());
        sale.setTotalAmount(totals.totalAmount());
    }

    private SaleResponse toSummary(UUID saleId) {
        Sale sale = saleRepository
                .findDetailedById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        List<SaleItem> items = saleItemRepository.findBySaleId(saleId);
        Map<UUID, BigDecimal> stocks = new LinkedHashMap<>();
        if (sale.getWarehouse() != null) {
            UUID warehouseId = sale.getWarehouse().getId();
            for (SaleItem item : items) {
                UUID productId = item.getProduct().getId();
                stocks.putIfAbsent(productId, inventoryService.availableQuantity(productId, warehouseId));
            }
        }
        return saleMapper.toResponse(sale, items, stocks);
    }

    private Sale requirePosSale(UUID saleId) {
        Sale sale = saleRepository
                .findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        return requirePosSale(sale);
    }

    private Sale requirePosSale(Sale sale) {
        if (!sale.isPos()) {
            throw new BusinessRuleException("Operação disponível apenas para vendas do canal POS");
        }
        return sale;
    }

    private SaleItem requireItem(UUID saleId, UUID itemId) {
        return saleItemRepository
                .findByIdAndSaleId(itemId, saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Item da venda", itemId));
    }

    private void assertOperatorOwnsSession(CashSession session) {
        if (!session.getOperator().getId().equals(CurrentUser.requireId())) {
            throw new BusinessRuleException("Operador deve ser o titular da sessão de caixa aberta");
        }
    }

    private void assertOperatorOwnsSale(Sale sale) {
        if (!sale.getSeller().getId().equals(CurrentUser.requireId())) {
            throw new BusinessRuleException("Venda vinculada a outro operador");
        }
    }

    private void assertPermission(String code) {
        if (!SecurityAuthorities.hasAuthority(code)) {
            throw new BusinessRuleException("Sem permissão: " + code);
        }
    }

    private User requireCurrentUser() {
        return userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));
    }

    private void applySellerProfile(Sale sale, SellerProfile profile) {
        sale.setSellerProfile(profile);
        sale.setSellerCodeSnapshot(profile.getSellerCode());
        sale.setSellerNameSnapshot(profile.getEmployee().getName());
        if (profile.getSupervisor() != null && profile.getSupervisor().getUser() != null) {
            sale.setSupervisor(profile.getSupervisor().getUser());
        }
    }

    private boolean isSellerRequired(br.com.systemcommerce.pos.store.entity.Store store) {
        if (store.isRequireSellerPos()) {
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

    private String nextSaleNumber() {
        return String.format("V%08d", saleRepository.nextSaleNumberValue());
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
        map.put("channel", sale.getChannel());
        map.put("totalAmount", sale.getTotalAmount());
        map.put("cashSessionId", sale.getCashSession() != null ? sale.getCashSession().getId() : null);
        map.put("version", sale.getVersion());
        return map;
    }
}
