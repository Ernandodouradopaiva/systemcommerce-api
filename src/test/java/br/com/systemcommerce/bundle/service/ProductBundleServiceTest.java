package br.com.systemcommerce.bundle.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.bundle.dto.ProductBundleCreateRequest;
import br.com.systemcommerce.bundle.entity.ProductBundle;
import br.com.systemcommerce.bundle.entity.ProductBundleItem;
import br.com.systemcommerce.bundle.entity.ProductBundleType;
import br.com.systemcommerce.bundle.mapper.ProductBundleMapper;
import br.com.systemcommerce.bundle.repository.ProductBundleItemRepository;
import br.com.systemcommerce.bundle.repository.ProductBundleRepository;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductBundleServiceTest {

    @Mock
    private ProductBundleRepository bundleRepository;
    @Mock
    private ProductBundleItemRepository itemRepository;
    @Mock
    private ProductBundleMapper mapper;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryService inventoryService;

    private ProductBundleService productBundleService;

    private UUID orgId;
    private UUID kitProductId;
    private UUID componentProductId;

    @BeforeEach
    void setUp() {
        productBundleService = new ProductBundleService(
                bundleRepository,
                itemRepository,
                mapper,
                organizationService,
                productRepository,
                inventoryService);

        orgId = UUID.randomUUID();
        kitProductId = UUID.randomUUID();
        componentProductId = UUID.randomUUID();
    }

    @Test
    void create_shouldRejectCircularBundleReference() {
        Organization organization = new Organization();
        organization.setId(orgId);

        Product kitProduct = new Product();
        kitProduct.setId(kitProductId);

        Product componentProduct = new Product();
        componentProduct.setId(componentProductId);

        ProductBundle existingBundle = new ProductBundle();
        existingBundle.setProduct(componentProduct);

        ProductBundleItem existingItem = new ProductBundleItem();
        existingItem.setProductBundle(existingBundle);
        existingItem.setComponentProduct(kitProduct);

        when(organizationService.resolveForStoreCreate(orgId)).thenReturn(organization);
        when(productRepository.findById(kitProductId)).thenReturn(Optional.of(kitProduct));
        when(bundleRepository.findByOrganizationIdAndCodeAndActiveTrue(orgId, "KIT-A")).thenReturn(Optional.empty());
        when(itemRepository.findActiveByOrganizationId(orgId)).thenReturn(List.of(existingItem));

        ProductBundleCreateRequest request = new ProductBundleCreateRequest(
                orgId,
                kitProductId,
                ProductBundleType.COMMERCIAL_KIT,
                "KIT-A",
                "Kit A",
                null,
                null,
                null,
                null,
                null,
                List.of(new ProductBundleCreateRequest.BundleItemRequest(
                        componentProductId, BigDecimal.ONE, 1, false)));

        assertThatThrownBy(() -> productBundleService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("circular");
    }
}
