package br.com.systemcommerce.fiscal.document.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentUpdateRequest;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentItemRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentPaymentRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentStatusHistoryRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentXmlRepository;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalNumberingSeriesRepository;
import br.com.systemcommerce.fiscal.establishment.service.FiscalEstablishmentService;
import br.com.systemcommerce.fiscal.operation.repository.FiscalOperationRepository;
import br.com.systemcommerce.fiscal.party.service.PartyFiscalProfileService;
import br.com.systemcommerce.fiscal.taxation.engine.repository.TaxCalculationRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiscalDocumentServiceTest {

    @Mock
    private FiscalDocumentRepository documentRepository;

    @Mock
    private FiscalDocumentItemRepository itemRepository;

    @Mock
    private FiscalDocumentPaymentRepository paymentRepository;

    @Mock
    private FiscalDocumentStatusHistoryRepository statusHistoryRepository;

    @Mock
    private FiscalDocumentXmlRepository xmlRepository;

    @Mock
    private FiscalEstablishmentRepository establishmentRepository;

    @Mock
    private FiscalNumberingSeriesRepository numberingSeriesRepository;

    @Mock
    private FiscalEstablishmentService establishmentService;

    @Mock
    private FiscalOperationRepository operationRepository;

    @Mock
    private PartyFiscalProfileService partyFiscalProfileService;

    @Mock
    private TaxCalculationRepository taxCalculationRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private StoreService storeService;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FiscalDocumentService documentService;

    @Test
    void shouldNotUpdateWhenAuthorized() {
        UUID id = UUID.randomUUID();
        FiscalDocument document = new FiscalDocument();
        document.setId(id);
        document.setStatus(FiscalDocumentStatus.AUTHORIZED);

        when(documentRepository.findDetailedById(id)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.update(id, new FiscalDocumentUpdateRequest(null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("AUTHORIZED");
    }
}
