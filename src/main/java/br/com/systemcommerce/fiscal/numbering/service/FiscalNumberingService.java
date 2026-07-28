package br.com.systemcommerce.fiscal.numbering.service;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalNumberingSeries;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalNumberingSeriesRepository;
import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberGapResponse;
import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberReservationResponse;
import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberVoidingResponse;
import br.com.systemcommerce.fiscal.numbering.dto.VoidingRequestCreateDto;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberGap;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberGap.GapStatus;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberReservation;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberReservation.ReservationStatus;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberSequence;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberSequence.SequenceStatus;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberVoidingRequest;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberVoidingRequest.VoidingStatus;
import br.com.systemcommerce.fiscal.numbering.repository.FiscalNumberGapRepository;
import br.com.systemcommerce.fiscal.numbering.repository.FiscalNumberReservationRepository;
import br.com.systemcommerce.fiscal.numbering.repository.FiscalNumberSequenceRepository;
import br.com.systemcommerce.fiscal.numbering.repository.FiscalNumberVoidingRequestRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.fiscal.transmission.dto.VoidingResult;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FiscalNumberingService {

    private static final Duration RESERVATION_TTL = Duration.ofHours(24);

    private final FiscalNumberSequenceRepository sequenceRepository;
    private final FiscalNumberReservationRepository reservationRepository;
    private final FiscalNumberGapRepository gapRepository;
    private final FiscalNumberVoidingRequestRepository voidingRepository;
    private final FiscalNumberingSeriesRepository numberingSeriesRepository;
    private final FiscalEstablishmentRepository establishmentRepository;
    private final FiscalDocumentRepository documentRepository;
    private final FiscalAuthorityAdapter fiscalAuthorityAdapter;
    private final DomainAuditService domainAuditService;

    @Transactional
    public FiscalNumberReservationResponse reserveNext(
            UUID establishmentId,
            String model,
            String series,
            FiscalEstablishment.FiscalEnvironment environment,
            UUID documentId,
            String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            return reservationRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .map(this::toReservationResponse)
                    .orElseGet(() -> doReserve(
                            establishmentId, model, series, environment, documentId, idempotencyKey));
        }
        return doReserve(establishmentId, model, series, environment, documentId, null);
    }

    private FiscalNumberReservationResponse doReserve(
            UUID establishmentId,
            String model,
            String series,
            FiscalEstablishment.FiscalEnvironment environment,
            UUID documentId,
            String idempotencyKey) {
        FiscalNumberSequence sequence = lockOrCreateSequence(establishmentId, model, series, environment);
        if (sequence.isLocked()) {
            throw new BusinessRuleException("Sequência de numeração bloqueada");
        }

        long next = sequence.getCurrentNumber() + 1;
        sequence.setCurrentNumber(next);
        sequence.setLastReservedNumber(next);
        sequenceRepository.save(sequence);

        FiscalNumberReservation reservation = new FiscalNumberReservation();
        reservation.setSequence(sequence);
        reservation.setNumber(next);
        reservation.setReservedAt(Instant.now());
        reservation.setExpiresAt(Instant.now().plus(RESERVATION_TTL));
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setIdempotencyKey(idempotencyKey);
        if (documentId != null) {
            FiscalDocument doc = documentRepository
                    .findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", documentId));
            reservation.setDocument(doc);
        }
        FiscalNumberReservation saved = reservationRepository.save(reservation);

        domainAuditService.record(
                "FISCAL",
                "FiscalNumberReservation",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("sequenceId", sequence.getId(), "number", next),
                "Número fiscal reservado");

        return toReservationResponse(saved);
    }

    @Transactional
    public FiscalNumberReservationResponse consumeReservation(UUID reservationId, UUID documentId) {
        FiscalNumberReservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de numeração", reservationId));
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new BusinessRuleException("Reserva não está em status RESERVED");
        }
        FiscalDocument document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", documentId));
        reservation.setDocument(document);
        reservation.setStatus(ReservationStatus.CONSUMED);
        FiscalNumberReservation saved = reservationRepository.save(reservation);
        domainAuditService.record(
                "FISCAL",
                "FiscalNumberReservation",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                Map.of("status", ReservationStatus.RESERVED),
                Map.of("status", ReservationStatus.CONSUMED, "documentId", documentId),
                "Reserva consumida");
        return toReservationResponse(saved);
    }

    @Transactional
    public FiscalNumberReservationResponse releaseReservation(UUID reservationId) {
        FiscalNumberReservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de numeração", reservationId));
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new BusinessRuleException("Somente reservas RESERVED podem ser liberadas");
        }
        reservation.setStatus(ReservationStatus.RELEASED);
        FiscalNumberReservation saved = reservationRepository.save(reservation);
        domainAuditService.record(
                "FISCAL",
                "FiscalNumberReservation",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                Map.of("status", ReservationStatus.RESERVED),
                Map.of("status", ReservationStatus.RELEASED),
                "Reserva liberada");
        return toReservationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FiscalNumberGapResponse> listGaps(UUID sequenceId, GapStatus status) {
        GapStatus filter = status != null ? status : GapStatus.OPEN;
        return gapRepository.findBySequenceIdAndStatusOrderByFromNumberAsc(sequenceId, filter).stream()
                .map(this::toGapResponse)
                .toList();
    }

    @Transactional
    public FiscalNumberGapResponse detectGap(UUID sequenceId, long fromNumber, long toNumber, String reason) {
        if (toNumber < fromNumber) {
            throw new BusinessRuleException("Intervalo de lacuna inválido");
        }
        FiscalNumberSequence sequence = sequenceRepository
                .findById(sequenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sequência de numeração", sequenceId));

        FiscalNumberGap gap = new FiscalNumberGap();
        gap.setSequence(sequence);
        gap.setFromNumber(fromNumber);
        gap.setToNumber(toNumber);
        gap.setReason(reason);
        gap.setStatus(GapStatus.OPEN);
        FiscalNumberGap saved = gapRepository.save(gap);
        domainAuditService.record(
                "FISCAL",
                "FiscalNumberGap",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("from", fromNumber, "to", toNumber),
                "Lacuna de numeração detectada");
        return toGapResponse(saved);
    }

    @Transactional
    public FiscalNumberVoidingResponse createVoidingRequest(VoidingRequestCreateDto request) {
        voidingRepository.findByIdempotencyKey(request.idempotencyKey()).ifPresent(v -> {
            throw new ConflictException("Inutilização já registrada para esta chave de idempotência");
        });

        FiscalEstablishment establishment = establishmentRepository
                .findById(request.establishmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", request.establishmentId()));

        assertRangeNotUsed(establishment.getId(), request.model(), request.series(), request.environment(),
                request.fromNumber(), request.toNumber());

        FiscalNumberVoidingRequest voiding = new FiscalNumberVoidingRequest();
        voiding.setEstablishment(establishment);
        voiding.setModel(request.model());
        voiding.setSeries(request.series());
        voiding.setEnvironment(request.environment());
        voiding.setFromNumber(request.fromNumber());
        voiding.setToNumber(request.toNumber());
        voiding.setJustification(request.justification());
        voiding.setIdempotencyKey(request.idempotencyKey());
        voiding.setStatus(VoidingStatus.DRAFT);
        FiscalNumberVoidingRequest saved = voidingRepository.save(voiding);

        domainAuditService.record(
                "FISCAL",
                "FiscalNumberVoidingRequest",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshotVoiding(saved),
                "Pedido de inutilização criado");
        return toVoidingResponse(saved);
    }

    @Transactional
    public FiscalNumberVoidingResponse transmitVoiding(UUID voidingId) {
        FiscalNumberVoidingRequest voiding = voidingRepository
                .findById(voidingId)
                .orElseThrow(() -> new ResourceNotFoundException("Inutilização", voidingId));
        if (voiding.getStatus() != VoidingStatus.DRAFT && voiding.getStatus() != VoidingStatus.QUEUED) {
            throw new BusinessRuleException("Inutilização não pode ser transmitida no status atual");
        }

        voiding.setStatus(VoidingStatus.QUEUED);
        voidingRepository.save(voiding);

        VoidingResult result = fiscalAuthorityAdapter.inutilizar(
                voiding.getEstablishment().getId(),
                voiding.getModel(),
                voiding.getSeries(),
                voiding.getEnvironment(),
                voiding.getFromNumber(),
                voiding.getToNumber(),
                voiding.getJustification());

        voiding.setTransmittedAt(Instant.now());
        voiding.setProtocolNumber(result.protocolNumber());
        voiding.setSefazCstat(result.cstat());
        voiding.setSefazXmotivo(result.xmotivo());
        voiding.setXmlEventRef(result.xmlRef());

        if (result.success()) {
            voiding.setStatus(VoidingStatus.AUTHORIZED);
            FiscalNumberSequence sequence = lockOrCreateSequence(
                    voiding.getEstablishment().getId(),
                    voiding.getModel(),
                    voiding.getSeries(),
                    voiding.getEnvironment());
            detectGapInternal(sequence, voiding.getFromNumber(), voiding.getToNumber(),
                    "Inutilização autorizada " + result.protocolNumber());
            for (FiscalNumberGap gap : gapRepository.findBySequenceIdAndStatusOrderByFromNumberAsc(
                    sequence.getId(), GapStatus.OPEN)) {
                if (gap.getFromNumber() >= voiding.getFromNumber() && gap.getToNumber() <= voiding.getToNumber()) {
                    gap.setStatus(GapStatus.VOIDED);
                    gapRepository.save(gap);
                }
            }
        } else {
            voiding.setStatus(VoidingStatus.REJECTED);
        }

        FiscalNumberVoidingRequest saved = voidingRepository.save(voiding);
        domainAuditService.record(
                "FISCAL",
                "FiscalNumberVoidingRequest",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                Map.of("status", VoidingStatus.QUEUED),
                snapshotVoiding(saved),
                "Inutilização transmitida");
        return toVoidingResponse(saved);
    }

    private FiscalNumberSequence lockOrCreateSequence(
            UUID establishmentId,
            String model,
            String series,
            FiscalEstablishment.FiscalEnvironment environment) {
        return sequenceRepository
                .findForUpdate(establishmentId, model, series, environment)
                .orElseGet(() -> createSequenceFromSeries(establishmentId, model, series, environment));
    }

    private FiscalNumberSequence createSequenceFromSeries(
            UUID establishmentId,
            String model,
            String series,
            FiscalEstablishment.FiscalEnvironment environment) {
        FiscalEstablishment establishment = establishmentRepository
                .findById(establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", establishmentId));

        FiscalNumberingSeries seriesEntity = numberingSeriesRepository
                .findForUpdate(establishmentId, model, series, environment)
                .orElseThrow(() -> new BusinessRuleException(
                        "Série de numeração não configurada para estabelecimento/modelo/série/ambiente"));

        long syncFrom = Math.max(0L, seriesEntity.getNextNumber() - 1);
        FiscalNumberSequence sequence = new FiscalNumberSequence();
        sequence.setEstablishment(establishment);
        sequence.setModel(model);
        sequence.setSeries(series);
        sequence.setEnvironment(environment);
        sequence.setCurrentNumber(syncFrom);
        sequence.setStatus(SequenceStatus.ACTIVE);
        return sequenceRepository.save(sequence);
    }

    private void assertRangeNotUsed(
            UUID establishmentId,
            String model,
            String series,
            FiscalEstablishment.FiscalEnvironment environment,
            long from,
            long to) {
        for (long n = from; n <= to; n++) {
            boolean used = documentRepository.existsByEstablishmentIdAndModelAndSeriesAndNumberAndEnvironmentAndStatusNotInAndActive(
                    establishmentId,
                    model,
                    series,
                    n,
                    environment,
                    List.of(FiscalDocumentStatus.CANCELLED, FiscalDocumentStatus.VOIDED),
                    true);
            if (used) {
                throw new BusinessRuleException("Número " + n + " já utilizado em documento fiscal");
            }
        }
    }

    private void detectGapInternal(FiscalNumberSequence sequence, long from, long to, String reason) {
        FiscalNumberGap gap = new FiscalNumberGap();
        gap.setSequence(sequence);
        gap.setFromNumber(from);
        gap.setToNumber(to);
        gap.setReason(reason);
        gap.setStatus(GapStatus.OPEN);
        gapRepository.save(gap);
    }

    private FiscalNumberReservationResponse toReservationResponse(FiscalNumberReservation r) {
        return new FiscalNumberReservationResponse(
                r.getId(),
                r.getSequence().getId(),
                r.getNumber(),
                r.getReservedAt(),
                r.getExpiresAt(),
                r.getDocument() != null ? r.getDocument().getId() : null,
                r.getStatus(),
                r.getIdempotencyKey());
    }

    private FiscalNumberGapResponse toGapResponse(FiscalNumberGap g) {
        return new FiscalNumberGapResponse(
                g.getId(),
                g.getSequence().getId(),
                g.getFromNumber(),
                g.getToNumber(),
                g.getDetectedAt(),
                g.getReason(),
                g.getStatus(),
                g.getNotes());
    }

    private FiscalNumberVoidingResponse toVoidingResponse(FiscalNumberVoidingRequest v) {
        return new FiscalNumberVoidingResponse(
                v.getId(),
                v.getEstablishment().getId(),
                v.getModel(),
                v.getSeries(),
                v.getFromNumber(),
                v.getToNumber(),
                v.getStatus(),
                v.getProtocolNumber(),
                v.getSefazCstat(),
                v.getSefazXmotivo(),
                v.getTransmittedAt(),
                v.getIdempotencyKey());
    }

    private Map<String, Object> snapshotVoiding(FiscalNumberVoidingRequest v) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", v.getId());
        map.put("status", v.getStatus());
        map.put("from", v.getFromNumber());
        map.put("to", v.getToNumber());
        map.put("protocol", v.getProtocolNumber());
        return map;
    }
}
