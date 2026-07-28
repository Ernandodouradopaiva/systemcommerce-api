package br.com.systemcommerce.fiscal.numbering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalNumberingSeries;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalNumberingSeriesRepository;
import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberReservationResponse;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberReservation;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberSequence;
import br.com.systemcommerce.fiscal.numbering.repository.FiscalNumberGapRepository;
import br.com.systemcommerce.fiscal.numbering.repository.FiscalNumberReservationRepository;
import br.com.systemcommerce.fiscal.numbering.repository.FiscalNumberSequenceRepository;
import br.com.systemcommerce.fiscal.numbering.repository.FiscalNumberVoidingRequestRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiscalNumberingServiceTest {

    @Mock
    private FiscalNumberSequenceRepository sequenceRepository;

    @Mock
    private FiscalNumberReservationRepository reservationRepository;

    @Mock
    private FiscalNumberGapRepository gapRepository;

    @Mock
    private FiscalNumberVoidingRequestRepository voidingRepository;

    @Mock
    private FiscalNumberingSeriesRepository numberingSeriesRepository;

    @Mock
    private FiscalEstablishmentRepository establishmentRepository;

    @Mock
    private FiscalDocumentRepository documentRepository;

    @Mock
    private FiscalAuthorityAdapter fiscalAuthorityAdapter;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private FiscalNumberingService numberingService;

    @Test
    void reserveNext_allocatesUniqueNumberUnderLock() {
        UUID establishmentId = UUID.randomUUID();
        FiscalNumberSequence sequence = new FiscalNumberSequence();
        sequence.setId(UUID.randomUUID());
        sequence.setCurrentNumber(10L);
        sequence.setStatus(FiscalNumberSequence.SequenceStatus.ACTIVE);

        when(sequenceRepository.findForUpdate(
                        eq(establishmentId), eq("55"), eq("1"), eq(FiscalEstablishment.FiscalEnvironment.HOMOLOGATION)))
                .thenReturn(Optional.of(sequence));
        when(sequenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(inv -> {
            FiscalNumberReservation r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        FiscalNumberReservationResponse response = numberingService.reserveNext(
                establishmentId, "55", "1", FiscalEstablishment.FiscalEnvironment.HOMOLOGATION, null, "idem-1");

        assertThat(response.number()).isEqualTo(11L);
        assertThat(sequence.getCurrentNumber()).isEqualTo(11L);

        ArgumentCaptor<FiscalNumberReservation> captor = ArgumentCaptor.forClass(FiscalNumberReservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getNumber()).isEqualTo(11L);
    }

    @Test
    void reserveNext_syncsFromNumberingSeriesOnFirstUse() {
        UUID establishmentId = UUID.randomUUID();
        FiscalEstablishment establishment = new FiscalEstablishment();
        establishment.setId(establishmentId);

        FiscalNumberingSeries series = new FiscalNumberingSeries();
        series.setNextNumber(42L);

        when(sequenceRepository.findForUpdate(
                        eq(establishmentId), eq("55"), eq("1"), eq(FiscalEstablishment.FiscalEnvironment.HOMOLOGATION)))
                .thenReturn(Optional.empty());
        when(establishmentRepository.findById(establishmentId)).thenReturn(Optional.of(establishment));
        when(numberingSeriesRepository.findForUpdate(
                        eq(establishmentId), eq("55"), eq("1"), eq(FiscalEstablishment.FiscalEnvironment.HOMOLOGATION)))
                .thenReturn(Optional.of(series));
        when(sequenceRepository.save(any())).thenAnswer(inv -> {
            FiscalNumberSequence s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(reservationRepository.save(any())).thenAnswer(inv -> {
            FiscalNumberReservation r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        FiscalNumberReservationResponse response = numberingService.reserveNext(
                establishmentId, "55", "1", FiscalEstablishment.FiscalEnvironment.HOMOLOGATION, null, null);

        assertThat(response.number()).isEqualTo(42L);
    }
}
