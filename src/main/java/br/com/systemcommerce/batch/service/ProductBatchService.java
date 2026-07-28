package br.com.systemcommerce.batch.service;

import br.com.systemcommerce.batch.dto.FefoPickLineResponse;
import br.com.systemcommerce.batch.dto.ProductBatchCreateRequest;
import br.com.systemcommerce.batch.dto.ProductBatchResponse;
import br.com.systemcommerce.batch.entity.BatchInventory;
import br.com.systemcommerce.batch.entity.ProductBatch;
import br.com.systemcommerce.batch.entity.ProductBatchStatus;
import br.com.systemcommerce.batch.mapper.ProductBatchMapper;
import br.com.systemcommerce.batch.repository.BatchInventoryRepository;
import br.com.systemcommerce.batch.repository.ProductBatchRepository;
import br.com.systemcommerce.batch.specification.ProductBatchSpecifications;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.repository.SupplierRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductBatchService {

    private final ProductBatchRepository batchRepository;
    private final BatchInventoryRepository batchInventoryRepository;
    private final ProductBatchMapper mapper;
    private final OrganizationService organizationService;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public Page<ProductBatchResponse> list(
            UUID organizationId, UUID productId, ProductBatchStatus status, String search, Pageable pageable) {
        return batchRepository
                .findAll(ProductBatchSpecifications.withFilters(organizationId, productId, status, search), pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductBatchResponse getById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional
    public ProductBatchResponse create(ProductBatchCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        Product product = productRepository
                .findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));
        if (!Boolean.TRUE.equals(product.getRequiresBatch())) {
            throw new BusinessRuleException("Produto não exige controle por lote");
        }

        String batchCode = request.batchCode().trim();
        if (batchRepository
                .findByOrganizationIdAndProductIdAndBatchCodeAndActiveTrue(
                        organization.getId(), product.getId(), batchCode)
                .isPresent()) {
            throw new ConflictException("Lote já cadastrado para o produto");
        }

        ProductBatch batch = new ProductBatch();
        batch.setOrganization(organization);
        batch.setProduct(product);
        batch.setBatchCode(batchCode);
        batch.setManufacturedAt(request.manufacturedAt());
        batch.setExpiresAt(request.expiresAt());
        batch.setReceivedAt(Instant.now());
        batch.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        batch.setStatus(ProductBatchStatus.ACTIVE);

        if (request.supplierId() != null) {
            Supplier supplier = supplierRepository
                    .findById(request.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", request.supplierId()));
            batch.setSupplier(supplier);
        }

        return mapper.toResponse(batchRepository.save(batch));
    }

    @Transactional
    public ProductBatchResponse block(UUID id, String reason) {
        ProductBatch batch = getEntity(id);
        if (batch.getStatus() == ProductBatchStatus.DEPLETED) {
            throw new BusinessRuleException("Lote esgotado não pode ser bloqueado");
        }
        batch.setStatus(ProductBatchStatus.BLOCKED);
        if (reason != null) {
            batch.setNotes(MoneyAndQuantityUtils.blankToNull(reason));
        }
        return mapper.toResponse(batchRepository.save(batch));
    }

    @Transactional(readOnly = true)
    public List<FefoPickLineResponse> fefoPick(UUID productId, UUID warehouseId, BigDecimal quantity) {
        BigDecimal remaining = MoneyAndQuantityUtils.positiveQuantity(quantity);
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));

        List<ProductBatch> candidates = batchRepository.findFefoCandidates(
                productId, warehouseId, ProductBatchStatus.ACTIVE);
        if (!Boolean.TRUE.equals(product.getFefoEnabled()) && candidates.size() > 1) {
            candidates = List.of(candidates.getFirst());
        }

        List<FefoPickLineResponse> lines = new ArrayList<>();
        for (ProductBatch batch : candidates) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BatchInventory inventory = batchInventoryRepository
                    .findByProductBatchIdAndWarehouseIdAndActiveTrue(batch.getId(), warehouseId)
                    .orElse(null);
            if (inventory == null) {
                continue;
            }
            BigDecimal available = inventory.getQuantity().subtract(inventory.getQuantityReserved());
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal pick = available.min(remaining).setScale(3, RoundingMode.HALF_UP);
            lines.add(new FefoPickLineResponse(batch.getId(), batch.getBatchCode(), pick));
            remaining = remaining.subtract(pick);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Saldo insuficiente em lotes para separação FEFO");
        }
        return lines;
    }

    @Transactional(readOnly = true)
    public void validateBatchForSale(UUID batchId, BigDecimal quantity) {
        ProductBatch batch = getEntity(batchId);
        if (batch.getStatus() != ProductBatchStatus.ACTIVE) {
            throw new BusinessRuleException("Lote indisponível para venda: " + batch.getStatus().name());
        }
        if (batch.getExpiresAt() != null && batch.getExpiresAt().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Lote vencido");
        }
        BigDecimal qty = MoneyAndQuantityUtils.positiveQuantity(quantity);
        BigDecimal total = batchInventoryRepository.findAll().stream()
                .filter(inv -> inv.getProductBatch().getId().equals(batchId) && Boolean.TRUE.equals(inv.getActive()))
                .map(inv -> inv.getQuantity().subtract(inv.getQuantityReserved()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(qty) < 0) {
            throw new BusinessRuleException("Quantidade indisponível no lote");
        }
    }

    private ProductBatch getEntity(UUID id) {
        return batchRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", id));
    }
}
