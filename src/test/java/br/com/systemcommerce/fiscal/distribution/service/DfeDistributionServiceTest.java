package br.com.systemcommerce.fiscal.distribution.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.distribution.entity.DfeSequenceControl;
import br.com.systemcommerce.fiscal.distribution.repository.DfeDistributionDocumentRepository;
import br.com.systemcommerce.fiscal.distribution.repository.DfeDistributionQueryRepository;
import br.com.systemcommerce.fiscal.distribution.repository.DfeSequenceControlRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalDocumentRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DfeDistributionServiceTest {

    @Mock
    private FiscalEstablishmentRepository establishmentRepository;

    @Mock
    private DfeSequenceControlRepository sequenceControlRepository;

    @Mock
    private DfeDistributionQueryRepository queryRepository;

    @Mock
    private DfeDistributionDocumentRepository documentRepository;

    @Mock
    private IncomingFiscalDocumentRepository incomingRepository;

    @Mock
    private FiscalAuthorityAdapter fiscalAuthorityAdapter;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private DfeDistributionService service;

    @Test
    void throttleBlocksAggressiveQuery() {
        UUID estId = UUID.randomUUID();
        FiscalEstablishment est = new FiscalEstablishment();
        est.setId(estId);
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        est.setOrganization(org);
        est.setUf("CE");

        DfeSequenceControl control = new DfeSequenceControl();
        control.setEstablishment(est);
        control.setOrganization(org);
        control.setLastNsu(10L);
        control.setNextAllowedQueryAt(Instant.now().plusSeconds(3600));

        when(establishmentRepository.findById(estId)).thenReturn(Optional.of(est));
        when(sequenceControlRepository.findByEstablishmentId(estId)).thenReturn(Optional.of(control));
        when(queryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.queryIncremental(estId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("agressivamente");
    }
}
