package br.com.systemcommerce.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.catalog.dto.BrandCreateRequest;
import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.catalog.mapper.BrandMapper;
import br.com.systemcommerce.catalog.repository.BrandRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private DomainAuditService domainAuditService;

    private BrandService service;

    private final UUID organizationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BrandService(
                brandRepository, productRepository, new BrandMapper(), organizationService, domainAuditService);
    }

    @Test
    void shouldRejectDuplicatedCodeOnCreate() {
        Organization organization = new Organization();
        organization.setId(organizationId);
        when(organizationService.resolveForStoreCreate(organizationId)).thenReturn(organization);
        when(brandRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, "NIKE")).thenReturn(true);

        BrandCreateRequest request = new BrandCreateRequest(organizationId, "NIKE", "Nike", null, null, null, null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldSoftInactivateInsteadOfDeletingWhenProductsReferenceBrand() {
        Brand brand = existingBrand();
        when(brandRepository.findById(brand.getId())).thenReturn(Optional.of(brand));
        when(productRepository.existsByBrandId(brand.getId())).thenReturn(true);

        service.delete(brand.getId());

        assertThat(brand.getStatus()).isEqualTo(Brand.BrandStatus.INACTIVE);
        assertThat(brand.getActive()).isFalse();
        verify(brandRepository, never()).delete(any(Brand.class));
        verify(brandRepository, times(1)).save(brand);
    }

    @Test
    void shouldPhysicallyDeleteWhenNoProductReferencesBrand() {
        Brand brand = existingBrand();
        when(brandRepository.findById(brand.getId())).thenReturn(Optional.of(brand));
        when(productRepository.existsByBrandId(brand.getId())).thenReturn(false);

        service.delete(brand.getId());

        verify(brandRepository, times(1)).delete(brand);
        verify(brandRepository, never()).save(any());
    }

    private Brand existingBrand() {
        Organization organization = new Organization();
        organization.setId(organizationId);
        Brand brand = new Brand();
        brand.setId(UUID.randomUUID());
        brand.setOrganization(organization);
        brand.setCode("NIKE");
        brand.setName("Nike");
        brand.markActive();
        return brand;
    }
}
