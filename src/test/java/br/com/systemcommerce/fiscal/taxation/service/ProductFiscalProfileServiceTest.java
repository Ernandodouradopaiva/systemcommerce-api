package br.com.systemcommerce.fiscal.taxation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.taxation.entity.ProductFiscalProfile;
import br.com.systemcommerce.fiscal.taxation.repository.ProductFiscalHistoryRepository;
import br.com.systemcommerce.fiscal.taxation.repository.ProductFiscalProfileRepository;
import br.com.systemcommerce.fiscal.taxation.repository.ProductTaxClassificationRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductFiscalProfileServiceTest {

    @Mock
    private ProductFiscalProfileRepository profileRepository;

    @Mock
    private ProductTaxClassificationRepository classificationRepository;

    @Mock
    private ProductFiscalHistoryRepository historyRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private StoreService storeService;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductFiscalProfileService profileService;

    @Test
    void shouldResolveStoreSpecificProfileBeforeGlobal() {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        LocalDate onDate = LocalDate.of(2026, 1, 15);

        Product product = new Product();
        Organization organization = new Organization();
        organization.setId(orgId);
        product.setOrganization(organization);

        Store store = new Store();
        store.setId(storeId);

        ProductFiscalProfile global = buildProfile(product, organization, null, null, "12345678");
        ProductFiscalProfile storeProfile = buildProfile(product, organization, store, null, "87654321");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(profileRepository.findActiveCandidates(eq(productId), eq(orgId), eq(onDate)))
                .thenReturn(List.of(global, storeProfile));

        ProductFiscalProfile resolved = profileService.resolveProfile(productId, storeId, "SP", onDate);

        assertThat(resolved.getNcmCode()).isEqualTo("87654321");
    }

    @Test
    void shouldResolveUfProfileWhenNoStoreMatch() {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        LocalDate onDate = LocalDate.of(2026, 1, 15);

        Product product = new Product();
        Organization organization = new Organization();
        organization.setId(orgId);
        product.setOrganization(organization);

        ProductFiscalProfile global = buildProfile(product, organization, null, null, "11111111");
        ProductFiscalProfile ufProfile = buildProfile(product, organization, null, "SP", "22222222");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(profileRepository.findActiveCandidates(eq(productId), eq(orgId), eq(onDate)))
                .thenReturn(List.of(global, ufProfile));

        ProductFiscalProfile resolved = profileService.resolveProfile(productId, storeId, "SP", onDate);

        assertThat(resolved.getNcmCode()).isEqualTo("22222222");
    }

    private ProductFiscalProfile buildProfile(
            Product product, Organization organization, Store store, String uf, String ncm) {
        ProductFiscalProfile profile = new ProductFiscalProfile();
        profile.setProduct(product);
        profile.setOrganization(organization);
        profile.setStore(store);
        profile.setUf(uf);
        profile.setNcmCode(ncm);
        profile.setOriginCode("0");
        profile.setValidFrom(LocalDate.of(2020, 1, 1));
        profile.markActive();
        return profile;
    }
}
