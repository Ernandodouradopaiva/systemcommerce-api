package br.com.systemcommerce.product.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.inventory.repository.InventoryMovementRepository;
import br.com.systemcommerce.inventory.repository.InventoryRepository;
import br.com.systemcommerce.product.dto.ProductCreateRequest;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.mapper.ProductMapper;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.sale.repository.SaleItemRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldRejectDuplicateSku() {
        when(productRepository.existsByInternalCodeIgnoreCase("INT-1")).thenReturn(false);
        when(productRepository.existsBySkuIgnoreCase("SKU-1")).thenReturn(true);

        ProductCreateRequest request = new ProductCreateRequest(
                "INT-1",
                "SKU-1",
                null,
                "Produto",
                null,
                UUID.randomUUID(),
                "UN",
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                false,
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> productService.create(request)).isInstanceOf(ConflictException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldBlockInactiveProductForSale() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        product.markInactive();
        when(productRepository.findDetailedById(id)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.requireUsableForSale(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void shouldSoftDeleteWhenProductHasStockMovement() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setId(id);
        product.setInternalCode("X");
        product.setSku("X");
        product.setName("X");
        product.markActive();
        Category category = new Category();
        category.setId(UUID.randomUUID());
        product.setCategory(category);

        when(productRepository.findDetailedById(id)).thenReturn(Optional.of(product));
        when(inventoryMovementRepository.existsByProductId(id)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.delete(id);

        verify(productRepository).save(any(Product.class));
        verify(productRepository, never()).delete(any(Product.class));
    }
}
