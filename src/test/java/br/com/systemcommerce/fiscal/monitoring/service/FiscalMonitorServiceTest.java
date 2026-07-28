package br.com.systemcommerce.fiscal.monitoring.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.monitoring.repository.FiscalDeadLetterItemRepository;
import br.com.systemcommerce.fiscal.monitoring.repository.FiscalEmissionQueueItemRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiscalMonitorServiceTest {

    @Mock
    private FiscalDocumentRepository documentRepository;

    @Mock
    private FiscalEmissionQueueItemRepository queueRepository;

    @Mock
    private FiscalDeadLetterItemRepository deadLetterRepository;

    @Mock
    private FiscalAuthorityAdapter fiscalAuthorityAdapter;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private FiscalMonitorService service;

    @Test
    void retransmitBlockedWhenAuthorized() {
        UUID id = UUID.randomUUID();
        FiscalDocument doc = new FiscalDocument();
        doc.setId(id);
        doc.setStatus(FiscalDocumentStatus.AUTHORIZED);
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        doc.setOrganization(org);
        FiscalEstablishment est = new FiscalEstablishment();
        est.setId(UUID.randomUUID());
        doc.setEstablishment(est);

        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.retransmitSafely(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("já autorizado");
    }

    @Test
    void retransmitBlockedOnRejection() {
        UUID id = UUID.randomUUID();
        FiscalDocument doc = new FiscalDocument();
        doc.setId(id);
        doc.setStatus(FiscalDocumentStatus.REJECTED);
        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.retransmitSafely(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Rejeição");
    }
}
