package br.com.systemcommerce.fiscal.party.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.party.PartyType;
import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
import br.com.systemcommerce.fiscal.party.entity.PartyFiscalProfile;
import br.com.systemcommerce.fiscal.party.repository.PartyFiscalHistoryRepository;
import br.com.systemcommerce.fiscal.party.repository.PartyFiscalProfileRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartyFiscalProfileServiceTest {

    @Mock
    private PartyFiscalProfileRepository profileRepository;

    @Mock
    private PartyFiscalHistoryRepository historyRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private StoreService storeService;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PartyFiscalProfileService profileService;

    @Test
    void shouldResolveStoreOverrideBeforeGlobal() {
        UUID partyId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        LocalDate onDate = LocalDate.of(2026, 1, 15);

        Organization organization = new Organization();
        organization.setId(orgId);

        Store store = new Store();
        store.setId(storeId);

        PartyFiscalProfile global = buildProfile(organization, null, PartyType.CUSTOMER, partyId, "GLOBAL");
        PartyFiscalProfile storeProfile = buildProfile(organization, store, PartyType.CUSTOMER, partyId, "STORE");

        when(profileRepository.findActiveCandidates(eq(PartyType.CUSTOMER), eq(partyId), eq(orgId), eq(onDate)))
                .thenReturn(List.of(global, storeProfile));

        PartyFiscalProfile resolved =
                profileService.resolveProfile(PartyType.CUSTOMER, partyId, orgId, storeId, onDate);

        assertThat(resolved.getTaxRegime()).isEqualTo("STORE");
    }

    private PartyFiscalProfile buildProfile(
            Organization organization, Store store, PartyType partyType, UUID partyId, String marker) {
        PartyFiscalProfile profile = new PartyFiscalProfile();
        profile.setOrganization(organization);
        profile.setStore(store);
        profile.setPartyType(partyType);
        profile.setPartyId(partyId);
        profile.setTaxpayerIndicator(TaxpayerIndicator.NON_CONTRIBUTOR);
        profile.setFinalConsumer(true);
        profile.setTaxRegime(marker);
        profile.setValidFrom(LocalDate.of(2020, 1, 1));
        profile.markActive();
        return profile;
    }
}
