package br.com.systemcommerce.supplier.service;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.supplier.dto.SupplierProductRequest;
import br.com.systemcommerce.supplier.dto.SupplierProductResponse;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.entity.SupplierProduct;
import br.com.systemcommerce.supplier.mapper.SupplierProductMapper;
import br.com.systemcommerce.supplier.repository.SupplierProductRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catálogo fornecedor x produto — referência para compra; preço/estoque reais permanecem no pedido de compra/estoque. */
@Service
@RequiredArgsConstructor
public class SupplierProductService {

    private final SupplierProductRepository supplierProductRepository;
    private final SupplierProductMapper supplierProductMapper;
    private final ProductRepository productRepository;
    private final SupplierService supplierService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<SupplierProductResponse> list(UUID supplierId) {
        supplierService.getEntity(supplierId);
        return supplierProductRepository.findBySupplierIdOrderByCreatedAtAsc(supplierId).stream()
                .map(supplierProductMapper::toResponse)
                .toList();
    }

    @Transactional
    public SupplierProductResponse create(UUID supplierId, SupplierProductRequest request) {
        Supplier supplier = supplierService.getEntity(supplierId);
        if (supplierProductRepository.existsBySupplierIdAndProductId(supplierId, request.productId())) {
            throw new ConflictException("Produto já vinculado a este fornecedor");
        }
        Product product = requireProduct(request.productId());
        SupplierProduct entity = new SupplierProduct();
        entity.setSupplier(supplier);
        entity.setProduct(product);
        supplierProductMapper.apply(entity, request);
        SupplierProduct saved = supplierProductRepository.save(entity);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierProduct",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Produto vinculado ao fornecedor");
        return supplierProductMapper.toResponse(saved);
    }

    @Transactional
    public SupplierProductResponse update(UUID supplierId, UUID linkId, SupplierProductRequest request) {
        SupplierProduct entity = getOwned(supplierId, linkId);
        if (!entity.getProduct().getId().equals(request.productId())
                && supplierProductRepository.existsBySupplierIdAndProductIdAndIdNot(
                        supplierId, request.productId(), linkId)) {
            throw new ConflictException("Produto já vinculado a este fornecedor");
        }
        if (!entity.getProduct().getId().equals(request.productId())) {
            entity.setProduct(requireProduct(request.productId()));
        }
        supplierProductMapper.apply(entity, request);
        SupplierProduct saved = supplierProductRepository.save(entity);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierProduct",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                null,
                "Vínculo produto/fornecedor atualizado");
        return supplierProductMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID supplierId, UUID linkId) {
        SupplierProduct entity = getOwned(supplierId, linkId);
        supplierProductRepository.delete(entity);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierProduct",
                linkId,
                AuditLog.AuditAction.DELETE,
                null,
                null,
                "Vínculo produto/fornecedor removido");
    }

    private Product requireProduct(UUID productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
    }

    private SupplierProduct getOwned(UUID supplierId, UUID linkId) {
        SupplierProduct entity = supplierProductRepository
                .findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto do fornecedor", linkId));
        if (!entity.getSupplier().getId().equals(supplierId)) {
            throw new ResourceNotFoundException("Produto do fornecedor", linkId);
        }
        return entity;
    }
}
