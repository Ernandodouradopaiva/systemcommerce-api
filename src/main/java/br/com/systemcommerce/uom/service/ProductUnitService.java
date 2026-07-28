package br.com.systemcommerce.uom.service;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.uom.dto.ProductUnitResponse;
import br.com.systemcommerce.uom.dto.ProductUnitUpsertRequest;
import br.com.systemcommerce.uom.entity.ProductUnit;
import br.com.systemcommerce.uom.entity.RoundingModeOption;
import br.com.systemcommerce.uom.entity.UnitOfMeasure;
import br.com.systemcommerce.uom.repository.ProductUnitRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductUnitService {

    private final ProductUnitRepository productUnitRepository;
    private final ProductRepository productRepository;
    private final UnitOfMeasureService unitOfMeasureService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public ProductUnitResponse getByProduct(UUID productId) {
        return productUnitRepository
                .findByProduct_Id(productId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unidades do produto não configuradas para o produto", productId));
    }

    @Transactional
    public ProductUnitResponse upsert(UUID productId, ProductUnitUpsertRequest request) {
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
        UnitOfMeasure stockUnit = unitOfMeasureService.getEntity(request.stockUnitId());
        UnitOfMeasure purchaseUnit = unitOfMeasureService.getEntity(request.purchaseUnitId());
        UnitOfMeasure salesUnit = unitOfMeasureService.getEntity(request.salesUnitId());

        ProductUnit productUnit = productUnitRepository.findByProduct_Id(productId).orElseGet(() -> {
            ProductUnit created = new ProductUnit();
            created.setProduct(product);
            return created;
        });
        productUnit.setStockUnit(stockUnit);
        productUnit.setPurchaseUnit(purchaseUnit);
        productUnit.setSalesUnit(salesUnit);
        productUnit.setPurchaseToStockFactor(
                request.purchaseToStockFactor() != null ? request.purchaseToStockFactor() : BigDecimal.ONE);
        productUnit.setSalesToStockFactor(
                request.salesToStockFactor() != null ? request.salesToStockFactor() : BigDecimal.ONE);
        productUnit.setRoundingMode(request.roundingMode() != null ? request.roundingMode() : RoundingModeOption.HALF_UP);
        productUnit.setActive(true);

        ProductUnit saved = productUnitRepository.save(productUnit);
        domainAuditService.record(
                "CATALOG",
                "ProductUnit",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                java.util.Map.of(
                        "productId", productId,
                        "stockUnitId", stockUnit.getId(),
                        "purchaseUnitId", purchaseUnit.getId(),
                        "salesUnitId", salesUnit.getId()),
                "Unidades do produto configuradas");
        return toResponse(saved);
    }

    private ProductUnitResponse toResponse(ProductUnit unit) {
        return new ProductUnitResponse(
                unit.getId(),
                unit.getProduct().getId(),
                unit.getStockUnit().getId(),
                unit.getStockUnit().getCode(),
                unit.getPurchaseUnit().getId(),
                unit.getPurchaseUnit().getCode(),
                unit.getSalesUnit().getId(),
                unit.getSalesUnit().getCode(),
                unit.getPurchaseToStockFactor(),
                unit.getSalesToStockFactor(),
                unit.getRoundingMode(),
                unit.getActive());
    }
}
