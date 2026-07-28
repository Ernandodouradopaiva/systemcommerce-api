package br.com.systemcommerce.supplier.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.supplier.dto.SupplierProductRequest;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.mapper.SupplierProductMapper;
import br.com.systemcommerce.supplier.repository.SupplierProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierProductServiceTest {

    @Mock
    private SupplierProductRepository supplierProductRepository;

    @Mock
    private SupplierProductMapper supplierProductMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SupplierService supplierService;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private SupplierProductService supplierProductService;

    @Test
    void shouldRejectDuplicateProductLinkForSameSupplier() {
        UUID supplierId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        when(supplierService.getEntity(supplierId)).thenReturn(supplier);
        when(supplierProductRepository.existsBySupplierIdAndProductId(supplierId, productId)).thenReturn(true);

        SupplierProductRequest request =
                new SupplierProductRequest(productId, "SKU-1", BigDecimal.TEN, 5, true);

        assertThatThrownBy(() -> supplierProductService.create(supplierId, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldRequireExistingProductToCreateLink() {
        UUID supplierId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        when(supplierService.getEntity(supplierId)).thenReturn(supplier);
        when(supplierProductRepository.existsBySupplierIdAndProductId(supplierId, productId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.of(new Product()));
        when(supplierProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SupplierProductRequest request =
                new SupplierProductRequest(productId, "SKU-1", BigDecimal.TEN, 5, true);

        supplierProductService.create(supplierId, request);

        org.mockito.Mockito.verify(productRepository).findById(productId);
        org.mockito.Mockito.verify(supplierProductRepository).save(any());
    }
}
