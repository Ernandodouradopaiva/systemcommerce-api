package br.com.systemcommerce.product.service;

import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.catalog.entity.Manufacturer;
import br.com.systemcommerce.catalog.entity.ProductLine;
import br.com.systemcommerce.catalog.service.BrandService;
import br.com.systemcommerce.catalog.service.ManufacturerService;
import br.com.systemcommerce.catalog.service.ProductLineService;
import br.com.systemcommerce.inventory.repository.InventoryMovementRepository;
import br.com.systemcommerce.inventory.repository.InventoryRepository;
import br.com.systemcommerce.product.dto.ProductCreateRequest;
import br.com.systemcommerce.product.dto.ProductResponse;
import br.com.systemcommerce.product.dto.ProductUpdateRequest;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.mapper.ProductMapper;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.specification.ProductSpecifications;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.repository.SaleItemRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.HashMap;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final DomainAuditService domainAuditService;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryRepository inventoryRepository;
    private final SaleItemRepository saleItemRepository;
    private final BrandService brandService;
    private final ManufacturerService manufacturerService;
    private final ProductLineService productLineService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(
            String name,
            String sku,
            String barcode,
            UUID categoryId,
            Product.ProductStatus status,
            String search,
            Pageable pageable) {
        return list(name, sku, barcode, categoryId, null, null, null, status, search, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(
            String name,
            String sku,
            String barcode,
            UUID categoryId,
            UUID brandId,
            UUID manufacturerId,
            UUID productLineId,
            Product.ProductStatus status,
            String search,
            Pageable pageable) {
        Page<Product> page = productRepository.findAll(
                ProductSpecifications.withFilters(
                        name, sku, barcode, categoryId, brandId, manufacturerId, productLineId, status, search),
                pageable);
        Map<UUID, BigDecimal> stocks = loadCurrentStocks(page.getContent());
        return page.map(product -> productMapper.toResponse(
                product, stocks.getOrDefault(product.getId(), BigDecimal.ZERO)));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        assertUniqueCodes(request.internalCode(), request.sku(), request.barcode(), null);
        Category category = categoryService.requireActiveForProduct(request.categoryId());
        Brand brand = request.brandId() != null ? brandService.getEntity(request.brandId()) : null;
        Manufacturer manufacturer =
                request.manufacturerId() != null ? manufacturerService.getEntity(request.manufacturerId()) : null;
        ProductLine productLine =
                request.productLineId() != null ? productLineService.getEntity(request.productLineId()) : null;

        Product product = new Product();
        productMapper.applyCreate(product, request, category, brand, manufacturer, productLine);
        Product saved = productRepository.save(product);
        domainAuditService.record(
                "Product", saved.getId(), AuditLog.AuditAction.CREATE, null, snapshot(saved), "Produto criado");
        return toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductUpdateRequest request) {
        Product product = getEntity(id);
        Map<String, Object> before = snapshot(product);
        assertUniqueCodes(request.internalCode(), request.sku(), request.barcode(), id);

        Category category;
        if (product.getCategory().getId().equals(request.categoryId())) {
            category = product.getCategory();
        } else {
            category = categoryService.requireActiveForProduct(request.categoryId());
        }
        Brand brand = request.brandId() != null ? brandService.getEntity(request.brandId()) : null;
        Manufacturer manufacturer =
                request.manufacturerId() != null ? manufacturerService.getEntity(request.manufacturerId()) : null;
        ProductLine productLine =
                request.productLineId() != null ? productLineService.getEntity(request.productLineId()) : null;

        productMapper.applyUpdate(product, request, category, brand, manufacturer, productLine);
        Product saved = productRepository.save(product);
        domainAuditService.record(
                "Product", id, AuditLog.AuditAction.UPDATE, before, snapshot(saved), "Produto atualizado");
        return toResponse(getEntity(id));
    }

    @Transactional
    public ProductResponse activate(UUID id) {
        Product product = getEntity(id);
        Map<String, Object> before = snapshot(product);
        product.markActive();
        Product saved = productRepository.save(product);
        domainAuditService.record(
                "Product", id, AuditLog.AuditAction.ACTIVATE, before, snapshot(saved), "Produto ativado");
        return toResponse(getEntity(id));
    }

    @Transactional
    public ProductResponse inactivate(UUID id) {
        Product product = getEntity(id);
        Map<String, Object> before = snapshot(product);
        product.markInactive();
        Product saved = productRepository.save(product);
        domainAuditService.record(
                "Product", id, AuditLog.AuditAction.DEACTIVATE, before, snapshot(saved), "Produto inativado");
        return toResponse(getEntity(id));
    }

    /**
     * Produto com movimentação (ou vínculo de estoque/venda) não é excluído fisicamente.
     */
    @Transactional
    public void delete(UUID id) {
        Product product = getEntity(id);
        Map<String, Object> before = snapshot(product);
        boolean hasMovement = inventoryMovementRepository.existsByProductId(id)
                || inventoryRepository.existsByProductId(id)
                || saleItemRepository.existsByProductId(id);

        if (hasMovement) {
            product.markInactive();
            productRepository.save(product);
            domainAuditService.record(
                    "Product",
                    id,
                    AuditLog.AuditAction.DELETE,
                    before,
                    snapshot(product),
                    "Exclusão lógica: produto possui movimentação ou vínculo");
            return;
        }

        productRepository.delete(product);
        domainAuditService.record(
                "Product", id, AuditLog.AuditAction.DELETE, before, null, "Produto removido fisicamente");
    }

    @Transactional(readOnly = true)
    public Product requireUsableForSale(UUID productId) {
        Product product = getEntity(productId);
        if (!product.isUsableForSale()) {
            throw new BusinessRuleException("Produto inativo não pode entrar em uma nova venda");
        }
        return product;
    }

    private Product getEntity(UUID id) {
        return productRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));
    }

    private ProductResponse toResponse(Product product) {
        BigDecimal currentStock = inventoryRepository.sumQuantityByProductId(product.getId());
        if (currentStock == null) {
            currentStock = BigDecimal.ZERO;
        }
        return productMapper.toResponse(product, currentStock);
    }

    private Map<UUID, BigDecimal> loadCurrentStocks(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = products.stream().map(Product::getId).toList();
        Map<UUID, BigDecimal> stocks = new HashMap<>();
        for (Object[] row : inventoryRepository.findQuantityRowsByProductIds(ids)) {
            stocks.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return stocks;
    }

    private void assertUniqueCodes(String internalCode, String sku, String barcode, UUID id) {
        String code = MoneyAndQuantityUtils.requireText(internalCode, "Código interno");
        String skuNorm = MoneyAndQuantityUtils.requireText(sku, "SKU");

        boolean internalExists = id == null
                ? productRepository.existsByInternalCodeIgnoreCase(code)
                : productRepository.existsByInternalCodeIgnoreCaseAndIdNot(code, id);
        if (internalExists) {
            throw new ConflictException("Código interno já está em uso");
        }

        boolean skuExists = id == null
                ? productRepository.existsBySkuIgnoreCase(skuNorm)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(skuNorm, id);
        if (skuExists) {
            throw new ConflictException("SKU já está em uso");
        }

        String barcodeNorm = MoneyAndQuantityUtils.blankToNull(barcode);
        if (StringUtils.hasText(barcodeNorm)) {
            boolean barcodeExists = id == null
                    ? productRepository.existsByBarcode(barcodeNorm)
                    : productRepository.existsByBarcodeAndIdNot(barcodeNorm, id);
            if (barcodeExists) {
                throw new ConflictException("Código de barras já está em uso");
            }
        }
    }

    private Map<String, Object> snapshot(Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("internalCode", product.getInternalCode());
        map.put("sku", product.getSku());
        map.put("barcode", product.getBarcode());
        map.put("name", product.getName());
        map.put("status", product.getStatus());
        map.put("salePrice", product.getSalePrice());
        map.put("costPrice", product.getCostPrice());
        map.put("categoryId", product.getCategory() != null ? product.getCategory().getId() : null);
        return map;
    }
}
