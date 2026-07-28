package br.com.systemcommerce.fiscal.document.special.service;

import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentCreateRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentResponse;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentReference;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentReferenceRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.document.service.FiscalDocumentService;
import br.com.systemcommerce.fiscal.document.special.dto.SpecialDocumentEmitRequest;
import br.com.systemcommerce.fiscal.emission.FiscalEmissionOrchestrator;
import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberReservationResponse;
import br.com.systemcommerce.fiscal.numbering.service.FiscalNumberingService;
import br.com.systemcommerce.fiscal.operation.entity.FiscalOperation;
import br.com.systemcommerce.fiscal.operation.repository.FiscalOperationRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FiscalSpecialDocumentService {

    private static final String MODEL_NFE = "55";

    private final FiscalDocumentRepository documentRepository;
    private final FiscalDocumentReferenceRepository referenceRepository;
    private final FiscalDocumentService documentService;
    private final FiscalNumberingService numberingService;
    private final FiscalEmissionOrchestrator emissionOrchestrator;
    private final FiscalOperationRepository operationRepository;
    private final DomainAuditService domainAuditService;

    @Transactional
    public FiscalDocumentResponse emitComplementary(SpecialDocumentEmitRequest request) {
        return emitSpecial(request, "COMPLEMENTAR", "NFE_COMPLEMENTAR");
    }

    @Transactional
    public FiscalDocumentResponse emitAdjustment(SpecialDocumentEmitRequest request) {
        return emitSpecial(request, "AJUSTE", "NFE_AJUSTE");
    }

    @Transactional
    public FiscalDocumentResponse emitRemittance(SpecialDocumentEmitRequest request) {
        return emitSpecial(request, "REMESSA", "NFE_REMESSA");
    }

    @Transactional
    public FiscalDocumentResponse emitReturn(SpecialDocumentEmitRequest request) {
        return emitSpecial(request, "DEVOLUCAO", "NFE_DEVOLUCAO");
    }

    @Transactional
    public FiscalDocumentResponse emitOwnEntry(SpecialDocumentEmitRequest request) {
        return emitSpecial(request, "ENTRADA_PROPRIA", "NFE_ENTRADA_PROPRIA");
    }

    @Transactional
    public FiscalDocumentResponse emitAnnulmentDocument(SpecialDocumentEmitRequest request) {
        return emitSpecial(request, "ANULACAO", "NFE_ANULACAO");
    }

    private FiscalDocumentResponse emitSpecial(SpecialDocumentEmitRequest request, String purpose, String operationCode) {
        FiscalDocument refDocument = documentRepository
                .findDetailedById(request.refDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal de referência", request.refDocumentId()));

        return documentRepository
                .findByOrganizationIdAndIdempotencyKey(refDocument.getOrganization().getId(), request.idempotencyKey())
                .map(d -> documentService.getById(d.getId()))
                .orElseGet(() -> doEmit(refDocument, request, purpose, operationCode));
    }

    private FiscalDocumentResponse doEmit(
            FiscalDocument refDocument, SpecialDocumentEmitRequest request, String purpose, String operationCode) {
        FiscalOperation operation = operationRepository
                .findByOrganizationIdAndCode(refDocument.getOrganization().getId(), operationCode)
                .filter(o -> Boolean.TRUE.equals(o.getActive()))
                .orElseThrow(() -> new BusinessRuleException("Operação fiscal não configurada: " + operationCode));

        if (Boolean.TRUE.equals(operation.getRequiresReferencedDocument()) && refDocument.getAccessKey() == null) {
            throw new BusinessRuleException("Documento de referência deve possuir chave de acesso");
        }

        String series = refDocument.getSeries();
        FiscalNumberReservationResponse reservation = numberingService.reserveNext(
                refDocument.getEstablishment().getId(),
                MODEL_NFE,
                series,
                refDocument.getEnvironment(),
                null,
                request.idempotencyKey() + ":NUM");

        FiscalDocumentCreateRequest create = new FiscalDocumentCreateRequest(
                refDocument.getOrganization().getId(),
                refDocument.getEstablishment().getId(),
                refDocument.getStore().getId(),
                MODEL_NFE,
                series,
                refDocument.getEnvironment(),
                operation.getNatureOfOperation(),
                purpose,
                operation.getId(),
                FiscalDocument.DocumentDirection.OUT,
                refDocument.getRecipientPartyType(),
                refDocument.getRecipientPartyId(),
                null,
                null,
                "SPECIAL:" + purpose,
                refDocument.getId(),
                request.idempotencyKey(),
                false,
                request.items(),
                List.of());

        FiscalDocumentResponse draft = documentService.createDraftWithReservedNumber(create, reservation.number());
        numberingService.consumeReservation(reservation.id(), draft.id());

        FiscalDocumentReference reference = new FiscalDocumentReference();
        reference.setDocument(documentRepository.getReferenceById(draft.id()));
        reference.setRefType("NFE");
        reference.setRefAccessKey(refDocument.getAccessKey());
        reference.setRefDocumentId(refDocument.getId());
        referenceRepository.save(reference);

        domainAuditService.record(
                "FISCAL",
                "FiscalDocument",
                draft.id(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("purpose", purpose, "refDocumentId", refDocument.getId()),
                "Documento fiscal especial emitido: " + purpose);

        return emissionOrchestrator.emitPipeline(draft);
    }
}
