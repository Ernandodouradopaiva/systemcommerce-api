package br.com.systemcommerce.pos.warehouse.service;

import br.com.systemcommerce.pos.warehouse.dto.ProductStorageLocationRequest;
import br.com.systemcommerce.pos.warehouse.dto.ProductStorageLocationResponse;
import br.com.systemcommerce.pos.warehouse.entity.ProductStorageLocation;
import br.com.systemcommerce.pos.warehouse.entity.StorageLocation;
import br.com.systemcommerce.pos.warehouse.repository.ProductStorageLocationRepository;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Vínculo produto ↔ localização de estoque (endereçamento físico) — Prompt 67. */
@Service
@RequiredArgsConstructor
public class ProductStorageLocationService {

    private final ProductStorageLocationRepository repository;
    private final ProductRepository productRepository;
    private final WarehouseLocationService warehouseLocationService;
    private final DomainAuditService domainAuditService;

    @Transactional
    public ProductStorageLocationResponse link(ProductStorageLocationRequest request) {
        Product product = productRepository
                .findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));
        StorageLocation location = warehouseLocationService.requireStorageLocation(request.storageLocationId());
        if (repository.existsByProduct_IdAndStorageLocation_Id(product.getId(), location.getId())) {
            throw new ConflictException("Produto já vinculado a esta localização");
        }
        ProductStorageLocation link = new ProductStorageLocation();
        link.setProduct(product);
        link.setStorageLocation(location);
        link.setPreferred(Boolean.TRUE.equals(request.preferred()));
        link.setMinQuantity(request.minQuantity());
        link.setMaxQuantity(request.maxQuantity());
        ProductStorageLocation saved = repository.save(link);
        domainAuditService.record(
                "WAREHOUSE",
                "ProductStorageLocation",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Produto vinculado à localização de estoque");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductStorageLocationResponse> listByProduct(UUID productId) {
        return repository.findByProduct_Id(productId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void unlink(UUID id) {
        ProductStorageLocation link = repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo produto/localização", id));
        repository.delete(link);
        domainAuditService.record(
                "WAREHOUSE",
                "ProductStorageLocation",
                id,
                AuditLog.AuditAction.DELETE,
                null,
                null,
                "Vínculo produto/localização removido");
    }

    private ProductStorageLocationResponse toResponse(ProductStorageLocation link) {
        return new ProductStorageLocationResponse(
                link.getId(),
                link.getProduct().getId(),
                link.getStorageLocation().getId(),
                link.getStorageLocation().getCode(),
                link.getPreferred(),
                link.getMinQuantity(),
                link.getMaxQuantity(),
                link.getQuantityAtLocation(),
                link.getActive());
    }
}
