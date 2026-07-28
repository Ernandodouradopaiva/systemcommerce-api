package br.com.systemcommerce.stocktransfer.service;

import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.stocktransfer.dto.StockTransferActionRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferCreateRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferDivergenceRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferInTransitItemResponse;
import br.com.systemcommerce.stocktransfer.dto.StockTransferItemCreateRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferReceiveRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferResponse;
import br.com.systemcommerce.stocktransfer.entity.StockTransfer;
import br.com.systemcommerce.stocktransfer.entity.StockTransferDispatch;
import br.com.systemcommerce.stocktransfer.entity.StockTransferDivergence;
import br.com.systemcommerce.stocktransfer.entity.StockTransferDivergenceType;
import br.com.systemcommerce.stocktransfer.entity.StockTransferItem;
import br.com.systemcommerce.stocktransfer.entity.StockTransferReceipt;
import br.com.systemcommerce.stocktransfer.entity.StockTransferStatus;
import br.com.systemcommerce.stocktransfer.entity.StockTransferStatusHistory;
import br.com.systemcommerce.stocktransfer.mapper.StockTransferMapper;
import br.com.systemcommerce.stocktransfer.repository.StockTransferDispatchRepository;
import br.com.systemcommerce.stocktransfer.repository.StockTransferDivergenceRepository;
import br.com.systemcommerce.stocktransfer.repository.StockTransferItemRepository;
import br.com.systemcommerce.stocktransfer.repository.StockTransferReceiptRepository;
import br.com.systemcommerce.stocktransfer.repository.StockTransferRepository;
import br.com.systemcommerce.stocktransfer.repository.StockTransferStatusHistoryRepository;
import br.com.systemcommerce.stocktransfer.specification.StockTransferSpecifications;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
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
public class StockTransferService {

    private static final EnumSet<StockTransferStatus> RECEIVABLE = EnumSet.of(
            StockTransferStatus.IN_TRANSIT, StockTransferStatus.PARTIALLY_RECEIVED);
    private static final EnumSet<StockTransferStatus> BEFORE_DISPATCH = EnumSet.of(
            StockTransferStatus.DRAFT,
            StockTransferStatus.REQUESTED,
            StockTransferStatus.APPROVED,
            StockTransferStatus.PREPARING);
    private static final EnumSet<StockTransferStatus> AFTER_DISPATCH = EnumSet.of(
            StockTransferStatus.DISPATCHED,
            StockTransferStatus.IN_TRANSIT,
            StockTransferStatus.PARTIALLY_RECEIVED);

    private final StockTransferRepository transferRepository;
    private final StockTransferItemRepository itemRepository;
    private final StockTransferReceiptRepository receiptRepository;
    private final StockTransferDispatchRepository dispatchRepository;
    private final StockTransferDivergenceRepository divergenceRepository;
    private final StockTransferStatusHistoryRepository historyRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final WarehouseService warehouseService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final StockTransferMapper stockTransferMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<StockTransferResponse> list(
            UUID organizationId,
            UUID originStoreId,
            UUID destinationStoreId,
            StockTransferStatus status,
            String search,
            Pageable pageable) {
        return transferRepository
                .findAll(
                        StockTransferSpecifications.withFilters(
                                organizationId, originStoreId, destinationStoreId, status, search),
                        pageable)
                .map(stockTransferMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StockTransferResponse getById(UUID id) {
        return stockTransferMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<StockTransferInTransitItemResponse> listInTransitItems(UUID storeId) {
        return itemRepository.findInTransitItems(storeId).stream()
                .map(stockTransferMapper::toInTransitItemResponse)
                .toList();
    }

    @Transactional
    public StockTransferResponse create(StockTransferCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = transferRepository.findByOrganizationIdAndIdempotencyKey(
                    organization.getId(), request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return stockTransferMapper.toResponse(getEntity(existing.get().getId()));
            }
        }

        Store originStore = storeService.requireUsable(request.originStoreId());
        Store destinationStore = storeService.requireUsable(request.destinationStoreId());
        Warehouse originWarehouse = warehouseService.requireUsable(request.originWarehouseId());
        Warehouse destinationWarehouse = warehouseService.requireUsable(request.destinationWarehouseId());

        assertSameOrganization(organization, originStore, destinationStore, originWarehouse, destinationWarehouse);
        assertDistinctWarehouses(originWarehouse, destinationWarehouse);
        assertWarehouseBelongsToStore(originWarehouse, originStore);
        assertWarehouseBelongsToStore(destinationWarehouse, destinationStore);

        StockTransfer transfer = new StockTransfer();
        transfer.setOrganization(organization);
        transfer.setNumber(nextTransferNumber(organization.getId()));
        transfer.setOriginStore(originStore);
        transfer.setOriginWarehouse(originWarehouse);
        transfer.setDestinationStore(destinationStore);
        transfer.setDestinationWarehouse(destinationWarehouse);
        transfer.setStatus(StockTransferStatus.DRAFT);
        transfer.setObservation(MoneyAndQuantityUtils.blankToNull(request.observation()));
        transfer.setReason(MoneyAndQuantityUtils.blankToNull(request.reason()));
        if (StringUtils.hasText(request.idempotencyKey())) {
            transfer.setIdempotencyKey(request.idempotencyKey().trim());
        }
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(transfer::setRequester);

        StockTransfer saved = transferRepository.save(transfer);
        recordStatusChange(saved, null, StockTransferStatus.DRAFT, "Transferência criada", null);
        domainAuditService.record(
                "STOCK_TRANSFER",
                "StockTransfer",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Transferência criada");
        return stockTransferMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public StockTransferResponse addItem(UUID transferId, StockTransferItemCreateRequest request) {
        StockTransfer transfer = getEntity(transferId);
        assertStatus(transfer, StockTransferStatus.DRAFT, "Inclusão de item");

        if (itemRepository
                .findByTransferIdAndProductIdAndActiveTrue(transferId, request.productId())
                .isPresent()) {
            throw new ConflictException("Produto já consta na transferência");
        }

        Product product = productRepository
                .findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BusinessRuleException("Produto inativo não pode ser transferido");
        }

        StockTransferItem item = new StockTransferItem();
        item.setTransfer(transfer);
        item.setProduct(product);
        item.setQuantityRequested(MoneyAndQuantityUtils.positiveQuantity(request.quantity()));
        item.setObservation(MoneyAndQuantityUtils.blankToNull(request.observation()));
        itemRepository.save(item);

        return stockTransferMapper.toResponse(getEntity(transferId));
    }

    @Transactional
    public StockTransferResponse request(UUID transferId, StockTransferActionRequest request) {
        StockTransfer transfer = getEntity(transferId);
        assertStatus(transfer, StockTransferStatus.DRAFT, "Solicitação");
        if (!itemRepository.existsByTransferIdAndActiveTrue(transferId)) {
            throw new BusinessRuleException("Transferência deve conter ao menos um item");
        }
        applyAction(transfer, StockTransferStatus.REQUESTED, request, "Transferência solicitada");
        transfer.setRequestedAt(Instant.now());
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(transfer::setRequester);
        transferRepository.save(transfer);
        return stockTransferMapper.toResponse(getEntity(transferId));
    }

    @Transactional
    public StockTransferResponse approve(UUID transferId, StockTransferActionRequest request) {
        StockTransfer transfer = getEntity(transferId);
        assertStatus(transfer, StockTransferStatus.REQUESTED, "Aprovação");
        List<StockTransferItem> items = itemRepository.findActiveByTransferId(transferId);
        for (StockTransferItem item : items) {
            BigDecimal approved = item.getQuantityRequested();
            item.setQuantityApproved(approved);
            BigDecimal available = inventoryService.availableQuantity(
                    item.getProduct().getId(), transfer.getOriginWarehouse().getId());
            if (available.compareTo(approved) < 0) {
                throw new BusinessRuleException(
                        "Estoque insuficiente no depósito de origem para o produto "
                                + item.getProduct().getSku()
                                + " (disponível: "
                                + available
                                + ", solicitado: "
                                + approved
                                + ")");
            }
            itemRepository.save(item);
        }
        applyAction(transfer, StockTransferStatus.APPROVED, request, "Transferência aprovada");
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(transfer::setApprover);
        transferRepository.save(transfer);
        return stockTransferMapper.toResponse(getEntity(transferId));
    }

    @Transactional
    public StockTransferResponse reject(UUID transferId, StockTransferActionRequest request) {
        StockTransfer transfer = getEntity(transferId);
        assertStatus(transfer, StockTransferStatus.REQUESTED, "Rejeição");
        applyAction(transfer, StockTransferStatus.REJECTED, request, "Transferência rejeitada");
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(transfer::setApprover);
        transferRepository.save(transfer);
        return stockTransferMapper.toResponse(getEntity(transferId));
    }

    @Transactional
    public StockTransferResponse prepare(UUID transferId, StockTransferActionRequest request) {
        StockTransfer transfer = getEntity(transferId);
        assertStatus(transfer, StockTransferStatus.APPROVED, "Preparação");
        applyAction(transfer, StockTransferStatus.PREPARING, request, "Transferência em preparação");
        transferRepository.save(transfer);
        return stockTransferMapper.toResponse(getEntity(transferId));
    }

    @Transactional
    public StockTransferResponse dispatch(UUID transferId, StockTransferActionRequest request) {
        StockTransfer transfer = getEntity(transferId);
        assertStatus(transfer, StockTransferStatus.PREPARING, "Expedição");

        String idempotencyKey = request != null && StringUtils.hasText(request.idempotencyKey())
                ? request.idempotencyKey().trim()
                : null;
        if (idempotencyKey != null) {
            var existing = dispatchRepository.findByTransferIdAndIdempotencyKey(transferId, idempotencyKey);
            if (existing.isPresent()) {
                return stockTransferMapper.toResponse(getEntity(transferId));
            }
        }

        List<StockTransferItem> items = itemRepository.findActiveByTransferId(transferId);
        UUID originWarehouseId = transfer.getOriginWarehouse().getId();
        UUID destinationWarehouseId = transfer.getDestinationWarehouse().getId();

        for (StockTransferItem item : items) {
            BigDecimal qty = item.getQuantityApproved() != null
                    ? item.getQuantityApproved()
                    : item.getQuantityRequested();
            item.setQuantityDispatched(qty);
            inventoryService.registerTransferOut(
                    item.getProduct().getId(), originWarehouseId, qty, transfer.getId());
            inventoryService.registerTransferInTransit(
                    item.getProduct().getId(), destinationWarehouseId, qty, transfer.getId());
            itemRepository.save(item);
        }

        User dispatcher = CurrentUser.id()
                .flatMap(userRepository::findById)
                .orElse(null);
        StockTransferDispatch dispatch = new StockTransferDispatch();
        dispatch.setTransfer(transfer);
        dispatch.setDispatchedBy(dispatcher);
        dispatch.setNotes(request != null ? MoneyAndQuantityUtils.blankToNull(request.observation()) : null);
        if (idempotencyKey != null) {
            dispatch.setIdempotencyKey(idempotencyKey);
        }
        dispatchRepository.save(dispatch);

        transfer.setDispatchedAt(Instant.now());
        if (dispatcher != null) {
            transfer.setDispatcher(dispatcher);
        }
        applyAction(transfer, StockTransferStatus.DISPATCHED, request, "Transferência despachada");
        applyAction(transfer, StockTransferStatus.IN_TRANSIT, request, "Transferência em trânsito");
        transferRepository.save(transfer);
        return stockTransferMapper.toResponse(getEntity(transferId));
    }

    @Transactional
    public StockTransferResponse receive(UUID transferId, StockTransferReceiveRequest request) {
        StockTransfer transfer = getEntity(transferId);
        assertReceivable(transfer);
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = receiptRepository.findByTransferIdAndIdempotencyKey(
                    transferId, request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return stockTransferMapper.toResponse(getEntity(transferId));
            }
        }

        List<StockTransferItem> items = itemRepository.findActiveByTransferId(transferId);
        List<StockTransferReceiveRequest.ReceiveLine> lines = items.stream()
                .map(item -> new StockTransferReceiveRequest.ReceiveLine(item.getId(), pendingQuantity(item)))
                .filter(line -> line.quantity().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (lines.isEmpty()) {
            throw new BusinessRuleException("Não há quantidade pendente para recebimento");
        }

        processReceiveLines(transfer, lines, request.observation(), request.idempotencyKey(), true);
        return stockTransferMapper.toResponse(getEntity(transferId));
    }

    @Transactional
    public StockTransferResponse receivePartial(UUID transferId, StockTransferReceiveRequest request) {
        StockTransfer transfer = getEntity(transferId);
        assertReceivable(transfer);
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = receiptRepository.findByTransferIdAndIdempotencyKey(
                    transferId, request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return stockTransferMapper.toResponse(getEntity(transferId));
            }
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessRuleException("Informe ao menos um item para recebimento parcial");
        }
        processReceiveLines(transfer, request.items(), request.observation(), request.idempotencyKey(), false);
        return stockTransferMapper.toResponse(getEntity(transferId));
    }

    @Transactional
    public StockTransferResponse registerDivergence(UUID transferId, StockTransferDivergenceRequest request) {
        StockTransfer transfer = getEntity(transferId);
        assertReceivable(transfer);
        User receiver = CurrentUser.id()
                .flatMap(userRepository::findById)
                .orElse(null);

        for (StockTransferDivergenceRequest.DivergenceLine line : request.items()) {
            StockTransferItem item = findItem(transfer.getId(), line.itemId());
            BigDecimal pending = pendingQuantity(item);
            BigDecimal divergenceQty = MoneyAndQuantityUtils.positiveQuantity(line.divergenceQuantity());
            if (divergenceQty.compareTo(pending) > 0) {
                throw new BusinessRuleException("Quantidade divergente excede pendência do item");
            }

            inventoryService.registerTransferRevertInTransit(
                    item.getProduct().getId(),
                    transfer.getDestinationWarehouse().getId(),
                    divergenceQty,
                    transfer.getId());

            item.setQuantityDivergent(defaultZero(item.getQuantityDivergent()).add(divergenceQty));
            itemRepository.save(item);

            StockTransferReceipt receipt = new StockTransferReceipt();
            receipt.setTransfer(transfer);
            receipt.setItem(item);
            receipt.setQuantityReceived(BigDecimal.ZERO);
            receipt.setQuantityExpected(pending);
            receipt.setDivergenceQuantity(divergenceQty);
            receipt.setDivergenceReason(MoneyAndQuantityUtils.blankToNull(line.divergenceReason()));
            receipt.setReceivedBy(receiver);
            receipt.setReceivedAt(Instant.now());
            receiptRepository.save(receipt);

            StockTransferDivergence divergence = new StockTransferDivergence();
            divergence.setTransfer(transfer);
            divergence.setItem(item);
            divergence.setReceipt(receipt);
            divergence.setDivergenceType(
                    line.divergenceType() != null ? line.divergenceType() : StockTransferDivergenceType.SHORTAGE);
            divergence.setQuantity(divergenceQty);
            divergence.setDescription(
                    StringUtils.hasText(line.divergenceReason())
                            ? line.divergenceReason().trim()
                            : "Divergência " + divergence.getDivergenceType().name());
            divergenceRepository.save(divergence);
        }

        if (StringUtils.hasText(request.observation())) {
            transfer.setObservation(MoneyAndQuantityUtils.blankToNull(request.observation()));
        }
        resolveReceiveStatus(transfer);
        if (transfer.getReceiver() == null) {
            transfer.setReceiver(receiver);
        }
        transferRepository.save(transfer);
        return stockTransferMapper.toResponse(getEntity(transferId));
    }

    @Transactional
    public StockTransferResponse cancel(UUID transferId, StockTransferActionRequest request) {
        StockTransfer transfer = getEntity(transferId);
        if (transfer.getStatus().isTerminal()) {
            throw new BusinessRuleException("Transferência já encerrada");
        }

        if (AFTER_DISPATCH.contains(transfer.getStatus())) {
            revertDispatchedInventory(transfer);
        } else if (!BEFORE_DISPATCH.contains(transfer.getStatus())) {
            throw new BusinessRuleException("Status não permite cancelamento");
        }

        applyAction(transfer, StockTransferStatus.CANCELLED, request, "Transferência cancelada");
        transferRepository.save(transfer);
        domainAuditService.record(
                "STOCK_TRANSFER",
                "StockTransfer",
                transferId,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                snapshot(transfer),
                request != null && StringUtils.hasText(request.reason())
                        ? request.reason()
                        : "Transferência cancelada");
        return stockTransferMapper.toResponse(getEntity(transferId));
    }

    private void processReceiveLines(
            StockTransfer transfer,
            List<StockTransferReceiveRequest.ReceiveLine> lines,
            String observation,
            String idempotencyKey,
            boolean fullReceive) {
        User receiver = CurrentUser.id()
                .flatMap(userRepository::findById)
                .orElse(null);
        UUID destinationWarehouseId = transfer.getDestinationWarehouse().getId();
        String normalizedKey = StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : null;

        for (StockTransferReceiveRequest.ReceiveLine line : lines) {
            StockTransferItem item = findItem(transfer.getId(), line.itemId());
            BigDecimal qty = MoneyAndQuantityUtils.positiveQuantity(line.quantity());
            BigDecimal pending = pendingQuantity(item);
            if (qty.compareTo(pending) > 0) {
                throw new BusinessRuleException("Quantidade recebida excede pendência do item");
            }
            if (fullReceive && qty.compareTo(pending) != 0 && pending.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessRuleException("Recebimento total deve usar a quantidade pendente completa");
            }

            inventoryService.registerTransferReceive(
                    item.getProduct().getId(), destinationWarehouseId, qty, transfer.getId());
            item.setQuantityReceived(defaultZero(item.getQuantityReceived()).add(qty));
            itemRepository.save(item);

            StockTransferReceipt receipt = new StockTransferReceipt();
            receipt.setTransfer(transfer);
            receipt.setItem(item);
            receipt.setQuantityReceived(qty);
            receipt.setQuantityExpected(pending);
            receipt.setDivergenceQuantity(BigDecimal.ZERO);
            receipt.setReceivedBy(receiver);
            receipt.setReceivedAt(Instant.now());
            if (normalizedKey != null) {
                receipt.setIdempotencyKey(normalizedKey);
            }
            receiptRepository.save(receipt);
        }

        transfer.setReceivedAt(Instant.now());
        transfer.setReceiver(receiver);
        if (StringUtils.hasText(observation)) {
            transfer.setObservation(MoneyAndQuantityUtils.blankToNull(observation));
        }
        resolveReceiveStatus(transfer);
        transferRepository.save(transfer);
    }

    private void revertDispatchedInventory(StockTransfer transfer) {
        List<StockTransferItem> items = itemRepository.findActiveByTransferId(transfer.getId());
        UUID originWarehouseId = transfer.getOriginWarehouse().getId();
        UUID destinationWarehouseId = transfer.getDestinationWarehouse().getId();

        for (StockTransferItem item : items) {
            BigDecimal dispatched = defaultZero(item.getQuantityDispatched());
            BigDecimal received = defaultZero(item.getQuantityReceived());
            BigDecimal divergent = defaultZero(item.getQuantityDivergent());
            BigDecimal inTransitRemaining = dispatched.subtract(received).subtract(divergent);
            if (inTransitRemaining.compareTo(BigDecimal.ZERO) > 0) {
                inventoryService.registerTransferRevertInTransit(
                        item.getProduct().getId(),
                        destinationWarehouseId,
                        inTransitRemaining,
                        transfer.getId());
            }
            BigDecimal revertOut = dispatched.subtract(received);
            if (revertOut.compareTo(BigDecimal.ZERO) > 0) {
                inventoryService.registerTransferRevertOut(
                        item.getProduct().getId(), originWarehouseId, revertOut, transfer.getId());
            }
        }
    }

    private void resolveReceiveStatus(StockTransfer transfer) {
        List<StockTransferItem> items = itemRepository.findActiveByTransferId(transfer.getId());
        boolean anyPending = items.stream().anyMatch(item -> pendingQuantity(item).compareTo(BigDecimal.ZERO) > 0);
        boolean anyReceived = items.stream()
                .anyMatch(item -> defaultZero(item.getQuantityReceived()).compareTo(BigDecimal.ZERO) > 0);

        if (anyPending) {
            if (anyReceived) {
                changeStatus(transfer, StockTransferStatus.PARTIALLY_RECEIVED, "Recebimento parcial", null);
            }
            return;
        }
        changeStatus(transfer, StockTransferStatus.RECEIVED, "Recebimento concluído", null);
    }

    private void applyAction(
            StockTransfer transfer, StockTransferStatus target, StockTransferActionRequest request, String defaultReason) {
        String reason = request != null && StringUtils.hasText(request.reason()) ? request.reason() : defaultReason;
        String observation = request != null ? MoneyAndQuantityUtils.blankToNull(request.observation()) : null;
        if (observation != null) {
            transfer.setObservation(observation);
        }
        if (request != null && StringUtils.hasText(request.reason())) {
            transfer.setReason(request.reason().trim());
        }
        changeStatus(transfer, target, reason, observation);
    }

    private void changeStatus(
            StockTransfer transfer, StockTransferStatus target, String reason, String observation) {
        StockTransferStatus from = transfer.getStatus();
        if (from == target) {
            return;
        }
        transfer.setStatus(target);
        recordStatusChange(transfer, from, target, reason, observation);
    }

    private void recordStatusChange(
            StockTransfer transfer,
            StockTransferStatus from,
            StockTransferStatus to,
            String reason,
            String observation) {
        StockTransferStatusHistory history = new StockTransferStatusHistory();
        history.setTransfer(transfer);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setReason(reason);
        history.setObservation(observation);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        historyRepository.save(history);
    }

    private StockTransferItem findItem(UUID transferId, UUID itemId) {
        return itemRepository
                .findActiveByTransferId(transferId).stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item da transferência", itemId));
    }

    private BigDecimal pendingQuantity(StockTransferItem item) {
        return defaultZero(item.getQuantityDispatched())
                .subtract(defaultZero(item.getQuantityReceived()))
                .subtract(defaultZero(item.getQuantityDivergent()));
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void assertReceivable(StockTransfer transfer) {
        if (!RECEIVABLE.contains(transfer.getStatus())) {
            throw new BusinessRuleException("Transferência não está disponível para recebimento");
        }
    }

    private void assertStatus(StockTransfer transfer, StockTransferStatus expected, String action) {
        if (transfer.getStatus() != expected) {
            throw new BusinessRuleException(
                    action + " não permitida no status " + transfer.getStatus().name());
        }
    }

    private void assertDistinctWarehouses(Warehouse origin, Warehouse destination) {
        if (origin.getId().equals(destination.getId())) {
            throw new BusinessRuleException("Depósito de origem e destino devem ser diferentes");
        }
    }

    private void assertWarehouseBelongsToStore(Warehouse warehouse, Store store) {
        if (!warehouse.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Depósito não pertence à loja informada");
        }
    }

    private void assertSameOrganization(
            Organization organization, Store originStore, Store destinationStore, Warehouse origin, Warehouse dest) {
        UUID orgId = organization.getId();
        if (!originStore.getOrganization().getId().equals(orgId)
                || !destinationStore.getOrganization().getId().equals(orgId)
                || !origin.getStore().getOrganization().getId().equals(orgId)
                || !dest.getStore().getOrganization().getId().equals(orgId)) {
            throw new BusinessRuleException("Lojas e depósitos devem pertencer à mesma organização");
        }
    }

    private String nextTransferNumber(UUID organizationId) {
        String datePart = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "TRF-" + datePart + "-";
        long sequence = transferRepository.countByNumberPrefix(organizationId, prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    private StockTransfer getEntity(UUID id) {
        return transferRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transferência", id));
    }

    private Map<String, Object> snapshot(StockTransfer transfer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", transfer.getId());
        map.put("number", transfer.getNumber());
        map.put("status", transfer.getStatus().name());
        map.put("originStoreId", transfer.getOriginStore().getId());
        map.put("destinationStoreId", transfer.getDestinationStore().getId());
        return map;
    }
}
