package br.com.systemcommerce.fiscal.migration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentProtocolRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentXmlRepository;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.migration.dto.ExternalFiscalHistoryImportRequest;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalFiscalHistoryServiceTest {

    @Mock
    private FiscalDocumentRepository documentRepository;

    @Mock
    private FiscalDocumentXmlRepository xmlRepository;

    @Mock
    private FiscalDocumentProtocolRepository protocolRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private FiscalEstablishmentRepository establishmentRepository;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private ExternalFiscalHistoryService service;

    @Test
    void rejectsImportWithoutFormalProcedure() {
        ExternalFiscalHistoryImportRequest req = new ExternalFiscalHistoryImportRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "55",
                "1",
                1L,
                "35250712345678000190550010000000011000000001",
                "<nfe/>",
                "PROT-1",
                null,
                "AUTHORIZED",
                "HOMOLOGATION",
                "LEGACY",
                null,
                null,
                null,
                "EXT:1",
                null);

        assertThatThrownBy(() -> service.importExternalHistory(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("procedimento formal");
    }

    @Test
    void rejectsInvalidAccessKey() {
        ExternalFiscalHistoryImportRequest req = new ExternalFiscalHistoryImportRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "55",
                "1",
                1L,
                "123",
                "<nfe/>",
                "PROT-1",
                null,
                "AUTHORIZED",
                "HOMOLOGATION",
                "LEGACY",
                null,
                null,
                null,
                "EXT:2",
                "ATA-001");

        assertThatThrownBy(() -> service.importExternalHistory(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("44");
    }
}
