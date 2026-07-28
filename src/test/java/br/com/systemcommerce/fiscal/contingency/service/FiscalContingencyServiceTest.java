package br.com.systemcommerce.fiscal.contingency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.contingency.dto.ContingencyActivateRequest;
import br.com.systemcommerce.fiscal.contingency.entity.FiscalContingency;
import br.com.systemcommerce.fiscal.contingency.entity.FiscalContingency.Mode;
import br.com.systemcommerce.fiscal.contingency.entity.FiscalContingency.Status;
import br.com.systemcommerce.fiscal.contingency.repository.ContingencyActivationRepository;
import br.com.systemcommerce.fiscal.contingency.repository.ContingencyDocumentRepository;
import br.com.systemcommerce.fiscal.contingency.repository.ContingencyTransmissionAttemptRepository;
import br.com.systemcommerce.fiscal.contingency.repository.FiscalContingencyRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiscalContingencyServiceTest {

    @Mock
    private FiscalContingencyRepository contingencyRepository;

    @Mock
    private ContingencyActivationRepository activationRepository;

    @Mock
    private ContingencyDocumentRepository contingencyDocumentRepository;

    @Mock
    private ContingencyTransmissionAttemptRepository attemptRepository;

    @Mock
    private FiscalEstablishmentRepository establishmentRepository;

    @Mock
    private FiscalDocumentRepository documentRepository;

    @Mock
    private FiscalAuthorityAdapter fiscalAuthorityAdapter;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private FiscalContingencyService contingencyService;

    @Test
    void activateAndClose() {
        UUID establishmentId = UUID.randomUUID();
        FiscalEstablishment establishment = new FiscalEstablishment();
        establishment.setId(establishmentId);
        establishment.setUf("CE");
        establishment.setFiscalEnvironment(FiscalEstablishment.FiscalEnvironment.HOMOLOGATION);

        when(establishmentRepository.findById(establishmentId)).thenReturn(Optional.of(establishment));
        when(contingencyRepository.findFirstByEstablishmentAndModelAndEnvironmentAndStatusAndActiveTrue(
                        any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(contingencyRepository.save(any())).thenAnswer(inv -> {
            FiscalContingency c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });

        var activated = contingencyService.activate(new ContingencyActivateRequest(
                establishmentId,
                "65",
                FiscalEstablishment.FiscalEnvironment.HOMOLOGATION,
                Mode.OFFLINE_NFCE,
                "Falha SEFAZ",
                null,
                null));

        assertThat(activated.status()).isEqualTo(Status.ACTIVE);

        when(contingencyRepository.findById(activated.id())).thenAnswer(inv -> {
            FiscalContingency c = new FiscalContingency();
            c.setId(activated.id());
            c.setEstablishment(establishment);
            c.setModel("65");
            c.setEnvironment(FiscalEstablishment.FiscalEnvironment.HOMOLOGATION);
            c.setMode(Mode.OFFLINE_NFCE);
            c.setStatus(Status.ACTIVE);
            return Optional.of(c);
        });

        var closed = contingencyService.close(activated.id());
        assertThat(closed.status()).isEqualTo(Status.CLOSED);
    }
}
