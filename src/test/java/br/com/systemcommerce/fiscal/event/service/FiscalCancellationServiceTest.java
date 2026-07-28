package br.com.systemcommerce.fiscal.event.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.event.dto.CancellationRequestCreateDto;
import br.com.systemcommerce.fiscal.event.repository.FiscalCancellationAttemptRepository;
import br.com.systemcommerce.fiscal.event.repository.FiscalCancellationAuthorizationRepository;
import br.com.systemcommerce.fiscal.event.repository.FiscalCancellationRequestRepository;
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
class FiscalCancellationServiceTest {

    @Mock
    private FiscalDocumentRepository documentRepository;

    @Mock
    private FiscalCancellationRequestRepository requestRepository;

    @Mock
    private FiscalCancellationAuthorizationRepository authorizationRepository;

    @Mock
    private FiscalCancellationAttemptRepository attemptRepository;

    @Mock
    private FiscalEventPolicyRepository policyRepository;

    @Mock
    private FiscalAuthorityAdapter fiscalAuthorityAdapter;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private FiscalCancellationService cancellationService;

    @Test
    void cannotCancelNonAuthorizedDocument() {
        UUID docId = UUID.randomUUID();
        FiscalDocument document = new FiscalDocument();
        document.setId(docId);
        document.setStatus(FiscalDocumentStatus.DRAFT);

        when(documentRepository.findDetailedById(docId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> cancellationService.requestCancellation(
                        new CancellationRequestCreateDto(docId, "Justificativa valida aqui", "CANCEL:1")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("autorizados");
    }
}
