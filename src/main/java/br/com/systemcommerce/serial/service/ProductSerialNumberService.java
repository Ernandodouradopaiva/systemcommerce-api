package br.com.systemcommerce.serial.service;

import br.com.systemcommerce.batch.entity.ProductBatch;
import br.com.systemcommerce.batch.repository.ProductBatchRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.warehouse.entity.StorageLocation;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.repository.StorageLocationRepository;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptRepository;
import br.com.systemcommerce.serial.dto.ProductSerialNumberResponse;
import br.com.systemcommerce.serial.dto.ProductSerialRegisterRequest;
import br.com.systemcommerce.serial.dto.ProductSerialStatusChangeRequest;
import br.com.systemcommerce.serial.dto.SerialNumberStatusHistoryResponse;
import br.com.systemcommerce.serial.entity.ProductSerialNumber;
import br.com.systemcommerce.serial.entity.ProductSerialStatus;
import br.com.systemcommerce.serial.entity.SerialNumberMovement;
import br.com.systemcommerce.serial.entity.SerialNumberStatusHistory;
import br.com.systemcommerce.serial.mapper.ProductSerialNumberMapper;
import br.com.systemcommerce.serial.repository.ProductSerialNumberRepository;
import br.com.systemcommerce.serial.repository.SerialNumberMovementRepository;
import br.com.systemcommerce.serial.repository.SerialNumberStatusHistoryRepository;
import br.com.systemcommerce.serial.specification.ProductSerialNumberSpecifications;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductSerialNumberService {

    private static final Set<ProductSerialStatus> ALLOWED_FROM_AVAILABLE =
            EnumSet.of(ProductSerialStatus.RESERVED, ProductSerialStatus.SOLD, ProductSerialStatus.BLOCKED);

    private final ProductSerialNumberRepository serialRepository;
    private final SerialNumberMovementRepository movementRepository;
    private final SerialNumberStatusHistoryRepository historyRepository;
    private final ProductSerialNumberMapper mapper;
    private final OrganizationService organizationService;
    private final ProductRepository productRepository;
    private final ProductBatchRepository batchRepository;
    private final WarehouseService warehouseService;
    private final StorageLocationRepository storageLocationRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ProductSerialNumberResponse> list(
            UUID organizationId, UUID productId, ProductSerialStatus status, String search, Pageable pageable) {
        return serialRepository
                .findAll(ProductSerialNumberSpecifications.withFilters(organizationId, productId, status, search), pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductSerialNumberResponse getById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<SerialNumberStatusHistoryResponse> statusHistory(UUID id) {
        getEntity(id);
        return historyRepository.findByProductSerialIdOrderByChangedAtAsc(id).stream()
                .map(mapper::toHistoryResponse)
                .toList();
    }

    @Transactional
    public ProductSerialNumberResponse registerOnReceipt(ProductSerialRegisterRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        Product product = productRepository
                .findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));
        if (!Boolean.TRUE.equals(product.getRequiresSerial())) {
            throw new BusinessRuleException("Produto não exige controle por série");
        }

        String serialNumber = request.serialNumber().trim();
        if (serialRepository.existsByOrganizationIdAndSerialNumberAndActiveTrue(organization.getId(), serialNumber)) {
            throw new ConflictException("Número de série já cadastrado na organização");
        }

        ProductSerialNumber serial = new ProductSerialNumber();
        serial.setOrganization(organization);
        serial.setProduct(product);
        serial.setSerialNumber(serialNumber);
        serial.setStatus(ProductSerialStatus.AVAILABLE);
        serial.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));

        if (request.productBatchId() != null) {
            ProductBatch batch = batchRepository
                    .findById(request.productBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lote", request.productBatchId()));
            serial.setProductBatch(batch);
        }
        if (request.warehouseId() != null) {
            Warehouse warehouse = warehouseService.requireUsable(request.warehouseId());
            serial.setWarehouse(warehouse);
        }
        if (request.storageLocationId() != null) {
            StorageLocation location = storageLocationRepository
                    .findById(request.storageLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Endereço", request.storageLocationId()));
            serial.setStorageLocation(location);
        }
        if (request.purchaseReceiptId() != null) {
            PurchaseReceipt receipt = purchaseReceiptRepository
                    .findById(request.purchaseReceiptId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recebimento", request.purchaseReceiptId()));
            serial.setPurchaseReceipt(receipt);
        }

        ProductSerialNumber saved = serialRepository.save(serial);
        recordTransition(saved, null, ProductSerialStatus.AVAILABLE, "PURCHASE_RECEIPT", request.purchaseReceiptId(), "Registro no recebimento");
        return mapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public ProductSerialNumberResponse changeStatus(UUID id, ProductSerialStatusChangeRequest request) {
        ProductSerialNumber serial = getEntity(id);
        ProductSerialStatus from = serial.getStatus();
        ProductSerialStatus to = request.targetStatus();
        assertTransitionAllowed(from, to);

        serial.setStatus(to);
        serialRepository.save(serial);
        recordTransition(serial, from, to, "MANUAL", id, request.notes());
        return mapper.toResponse(getEntity(id));
    }

    private void assertTransitionAllowed(ProductSerialStatus from, ProductSerialStatus to) {
        if (from == to) {
            return;
        }
        boolean allowed = switch (from) {
            case AVAILABLE -> ALLOWED_FROM_AVAILABLE.contains(to);
            case RESERVED -> EnumSet.of(
                            ProductSerialStatus.AVAILABLE, ProductSerialStatus.SOLD, ProductSerialStatus.BLOCKED)
                    .contains(to);
            case SOLD -> EnumSet.of(ProductSerialStatus.RETURNED, ProductSerialStatus.DEFECTIVE).contains(to);
            case RETURNED -> EnumSet.of(ProductSerialStatus.AVAILABLE, ProductSerialStatus.DEFECTIVE).contains(to);
            case IN_TRANSIT -> EnumSet.of(ProductSerialStatus.AVAILABLE, ProductSerialStatus.SOLD).contains(to);
            case BLOCKED -> EnumSet.of(ProductSerialStatus.AVAILABLE).contains(to);
            default -> false;
        };
        if (!allowed) {
            throw new BusinessRuleException("Transição de status inválida: " + from + " -> " + to);
        }
    }

    private void recordTransition(
            ProductSerialNumber serial,
            ProductSerialStatus from,
            ProductSerialStatus to,
            String originType,
            UUID originId,
            String notes) {
        User performer = CurrentUser.id().flatMap(userRepository::findById).orElse(null);

        SerialNumberStatusHistory history = new SerialNumberStatusHistory();
        history.setProductSerial(serial);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(MoneyAndQuantityUtils.blankToNull(notes));
        history.setChangedBy(performer);
        historyRepository.save(history);

        SerialNumberMovement movement = new SerialNumberMovement();
        movement.setProductSerial(serial);
        movement.setFromStatus(from);
        movement.setToStatus(to);
        movement.setOriginType(originType);
        movement.setOriginId(originId);
        movement.setWarehouse(serial.getWarehouse());
        movement.setNotes(MoneyAndQuantityUtils.blankToNull(notes));
        movement.setPerformedBy(performer);
        movementRepository.save(movement);
    }

    private ProductSerialNumber getEntity(UUID id) {
        return serialRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Número de série", id));
    }
}
