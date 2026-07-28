package br.com.systemcommerce.fiscal.event.cce.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.event.cce.CceBlockedFieldsConfig;
import br.com.systemcommerce.fiscal.event.cce.dto.CorrectionLetterCreateRequest;
import br.com.systemcommerce.fiscal.event.cce.repository.CorrectionLetterEventXmlRepository;
import br.com.systemcommerce.fiscal.event.cce.repository.CorrectionLetterRepository;
import br.com.systemcommerce.fiscal.event.cce.repository.CorrectionLetterSequenceRepository;
import br.com.systemcommerce.fiscal.event.repository.FiscalEventPolicyRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
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
class CorrectionLetterServiceTest {

    @Mock
    private FiscalDocumentRepository documentRepository;

    @Mock
    private CorrectionLetterRepository letterRepository;

    @Mock
    private CorrectionLetterSequenceRepository sequenceRepository;

    @Mock
    private CorrectionLetterEventXmlRepository eventXmlRepository;

    @Mock
    private FiscalEventPolicyRepository policyRepository;

    @Mock
    private FiscalAuthorityAdapter fiscalAuthorityAdapter;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private CceBlockedFieldsConfig blockedFieldsConfig;

    @InjectMocks
    private CorrectionLetterService correctionLetterService;

    @Test
    void rejectsNfceModel65() {
        UUID docId = UUID.randomUUID();
        FiscalDocument document = new FiscalDocument();
        document.setId(docId);
        document.setModel("65");
        document.setStatus(FiscalDocumentStatus.AUTHORIZED);

        when(documentRepository.findDetailedById(docId)).thenReturn(Optional.of(document));
        when(letterRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> correctionLetterService.request(new CorrectionLetterCreateRequest(
                        docId, "Correção de endereço de entrega do cliente", "CCE:1")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("NFC-e");
    }
}
