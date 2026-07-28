package br.com.systemcommerce.fiscal.integration.returns.service;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentCreateRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentItemRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentResponse;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentItem;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentReference;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentItemRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentReferenceRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.document.service.FiscalDocumentService;
import br.com.systemcommerce.fiscal.emission.FiscalEmissionOrchestrator;
import br.com.systemcommerce.fiscal.integration.returns.dto.FiscalReturnLinkResponse;
import br.com.systemcommerce.fiscal.integration.returns.dto.PurchaseReturnEmitRequest;
import br.com.systemcommerce.fiscal.integration.returns.dto.SaleReturnEmitRequest;
import br.com.systemcommerce.fiscal.integration.returns.entity.FiscalReturnLink;
import br.com.systemcommerce.fiscal.integration.returns.entity.FiscalReturnLink.LinkStatus;
import br.com.systemcommerce.fiscal.integration.returns.entity.FiscalReturnLink.ReturnType;
import br.com.systemcommerce.fiscal.integration.returns.repository.FiscalReturnLinkRepository;
import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberReservationResponse;
import br.com.systemcommerce.fiscal.numbering.service.FiscalNumberingService;
import br.com.systemcommerce.pos.cancellation.entity.SaleReturn;
import br.com.systemcommerce.pos.cancellation.entity.SaleReturnItem;
import br.com.systemcommerce.pos.cancellation.repository.SaleReturnRepository;
import br.com.systemcommerce.purchase.entity.SupplierReturn;
import br.com.systemcommerce.purchase.entity.SupplierReturnItem;
import br.com.systemcommerce.purchase.repository.SupplierReturnRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FiscalReturnService {

    private static final String MODEL_NFE = "55";

    private final SaleReturnRepository saleReturnRepository;
    private final SupplierReturnRepository supplierReturnRepository;
    private final FiscalDocumentRepository documentRepository;
    private final FiscalDocumentItemRepository documentItemRepository;
    private final FiscalDocumentReferenceRepository referenceRepository;
    private final FiscalReturnLinkRepository returnLinkRepository;
    private final FiscalDocumentService documentService;
    private final FiscalNumberingService numberingService;
    private final FiscalEmissionOrchestrator emissionOrchestrator;
    private final DomainAuditService domainAuditService;

    @Transactional
    public FiscalReturnLinkResponse emitSaleReturn(SaleReturnEmitRequest request) {
        if (!request.hasSaleReturnId()) {
            throw new BusinessRuleException("saleReturnId é obrigatório");
        }

        SaleReturn saleReturn = saleReturnRepository
                .findDetailedById(request.saleReturnId())
                .orElseThrow(() -> new ResourceNotFoundException("Devolução de venda", request.saleReturnId()));

        String idempotencyKey = "RETURN:SALE:" + saleReturn.getId();
        return returnLinkRepository
                .findByReturnTypeAndReturnId(ReturnType.SALE, saleReturn.getId())
                .map(this::toResponse)
                .orElseGet(() -> doEmitSaleReturn(saleReturn, request, idempotencyKey));
    }

    @Transactional
    public FiscalReturnLinkResponse emitPurchaseReturn(PurchaseReturnEmitRequest request) {
        SupplierReturn supplierReturn = supplierReturnRepository
                .findDetailedById(request.supplierReturnId())
                .orElseThrow(() -> new ResourceNotFoundException("Devolução ao fornecedor", request.supplierReturnId()));

        String idempotencyKey = "RETURN:PURCHASE:" + supplierReturn.getId();
        return returnLinkRepository
                .findByReturnTypeAndReturnId(ReturnType.PURCHASE, supplierReturn.getId())
                .map(this::toResponse)
                .orElseGet(() -> doEmitPurchaseReturn(supplierReturn, idempotencyKey));
    }

    @Transactional(readOnly = true)
    public FiscalReturnLinkResponse getByReturn(ReturnType returnType, UUID returnId) {
        return returnLinkRepository
                .findByReturnTypeAndReturnId(returnType, returnId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo fiscal de devolução", returnId));
    }

    private FiscalReturnLinkResponse doEmitSaleReturn(
            SaleReturn saleReturn, SaleReturnEmitRequest request, String idempotencyKey) {
        FiscalDocument original = resolveOriginalDocument(request);
        validateReturnableQuantities(saleReturn, original);

        List<FiscalDocumentItemRequest> items = buildSaleReturnItems(saleReturn);
        FiscalDocumentResponse emitted = emitReturnDocument(original, items, idempotencyKey, "SALE_RETURN", saleReturn.getId());

        FiscalReturnLink link = createLink(ReturnType.SALE, saleReturn.getId(), original, emitted);
        updateLinkStatusFromDocument(link, emitted);
        return toResponse(returnLinkRepository.save(link));
    }

    private FiscalReturnLinkResponse doEmitPurchaseReturn(SupplierReturn supplierReturn, String idempotencyKey) {
        FiscalDocument original = findOriginalForPurchaseReturn(supplierReturn);
        List<FiscalDocumentItemRequest> items = buildPurchaseReturnItems(supplierReturn);
        FiscalDocumentResponse emitted =
                emitReturnDocument(original, items, idempotencyKey, "PURCHASE_RETURN", supplierReturn.getId());

        FiscalReturnLink link = createLink(ReturnType.PURCHASE, supplierReturn.getId(), original, emitted);
        updateLinkStatusFromDocument(link, emitted);
        return toResponse(returnLinkRepository.save(link));
    }

    private FiscalDocument resolveOriginalDocument(SaleReturnEmitRequest request) {
        if (request.originalDocumentId() != null) {
            return documentRepository
                    .findDetailedById(request.originalDocumentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal original", request.originalDocumentId()));
        }
        if (StringUtils.hasText(request.originalAccessKey())) {
            return documentRepository.findAll().stream()
                    .filter(d -> request.originalAccessKey().equals(d.getAccessKey()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Documento por chave", request.originalAccessKey()));
        }
        throw new BusinessRuleException("Informe originalDocumentId ou originalAccessKey");
    }

    private FiscalDocument findOriginalForPurchaseReturn(SupplierReturn supplierReturn) {
        if (supplierReturn.getPurchaseReceipt() != null) {
            return documentRepository
                    .findFirstByOriginDocumentTypeAndOriginDocumentIdAndModelAndActiveTrue(
                            "PURCHASE_RECEIPT", supplierReturn.getPurchaseReceipt().getId(), MODEL_NFE)
                    .orElseThrow(() -> new BusinessRuleException("NF-e de entrada não encontrada para o recebimento"));
        }
        throw new BusinessRuleException("Devolução ao fornecedor requer recebimento vinculado para referência fiscal");
    }

    private void validateReturnableQuantities(SaleReturn saleReturn, FiscalDocument original) {
        List<FiscalDocumentItem> originalItems = documentItemRepository.findByDocumentIdOrderByLineNumber(original.getId());
        Map<UUID, BigDecimal> originalQtyByProduct = new HashMap<>();
        for (FiscalDocumentItem item : originalItems) {
            if (item.getProductId() != null && item.getQuantity() != null) {
                originalQtyByProduct.merge(item.getProductId(), item.getQuantity(), BigDecimal::add);
            }
        }
        for (SaleReturnItem returnItem : saleReturn.getItems()) {
            BigDecimal originalQty = originalQtyByProduct.get(returnItem.getProduct().getId());
            if (originalQty == null || returnItem.getQuantity().compareTo(originalQty) > 0) {
                throw new BusinessRuleException(
                        "Quantidade devolvida excede a do documento original para produto "
                                + returnItem.getProduct().getId());
            }
        }
    }

    private List<FiscalDocumentItemRequest> buildSaleReturnItems(SaleReturn saleReturn) {
        List<FiscalDocumentItemRequest> items = new ArrayList<>();
        for (SaleReturnItem ri : saleReturn.getItems()) {
            items.add(new FiscalDocumentItemRequest(
                    ri.getProduct().getId(),
                    ri.getProduct().getName(),
                    null,
                    null,
                    "1202",
                    ri.getQuantity(),
                    ri.getUnitPrice(),
                    null,
                    ri.getProduct().getUnitOfMeasure(),
                    ri.getProduct().getUnitOfMeasure()));
        }
        return items;
    }

    private List<FiscalDocumentItemRequest> buildPurchaseReturnItems(SupplierReturn supplierReturn) {
        List<FiscalDocumentItemRequest> items = new ArrayList<>();
        for (SupplierReturnItem ri : supplierReturn.getItems()) {
            items.add(new FiscalDocumentItemRequest(
                    ri.getProduct().getId(),
                    ri.getProduct().getName(),
                    null,
                    null,
                    "5202",
                    ri.getQuantity(),
                    ri.getUnitCost(),
                    null,
                    ri.getProduct().getUnitOfMeasure(),
                    ri.getProduct().getUnitOfMeasure()));
        }
        return items;
    }

    private FiscalDocumentResponse emitReturnDocument(
            FiscalDocument original,
            List<FiscalDocumentItemRequest> items,
            String idempotencyKey,
            String originType,
            UUID originId) {
        String series = original.getSeries();
        FiscalNumberReservationResponse reservation = numberingService.reserveNext(
                original.getEstablishment().getId(),
                MODEL_NFE,
                series,
                original.getEnvironment(),
                null,
                idempotencyKey + ":NUM");

        FiscalDocumentCreateRequest create = new FiscalDocumentCreateRequest(
                original.getOrganization().getId(),
                original.getEstablishment().getId(),
                original.getStore().getId(),
                MODEL_NFE,
                series,
                original.getEnvironment(),
                "Devolução",
                "DEVOLUCAO",
                null,
                FiscalDocument.DocumentDirection.OUT,
                original.getRecipientPartyType(),
                original.getRecipientPartyId(),
                null,
                null,
                originType,
                originId,
                idempotencyKey,
                false,
                items,
                List.of());

        FiscalDocumentResponse draft = documentService.createDraftWithReservedNumber(create, reservation.number());
        numberingService.consumeReservation(reservation.id(), draft.id());

        FiscalDocumentReference reference = new FiscalDocumentReference();
        reference.setDocument(documentRepository.getReferenceById(draft.id()));
        reference.setRefType("NFE");
        reference.setRefAccessKey(original.getAccessKey());
        reference.setRefDocumentId(original.getId());
        referenceRepository.save(reference);

        FiscalDocumentResponse emitted = emissionOrchestrator.emitPipeline(draft);
        domainAuditService.record(
                "FISCAL",
                "FiscalReturnLink",
                originId,
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("fiscalDocumentId", emitted.id()),
                "NF-e de devolução emitida — estoque não movimentado pelo módulo fiscal");
        return emitted;
    }

    private FiscalReturnLink createLink(
            ReturnType returnType, UUID returnId, FiscalDocument original, FiscalDocumentResponse emitted) {
        FiscalReturnLink link = new FiscalReturnLink();
        link.setReturnType(returnType);
        link.setReturnId(returnId);
        link.setOriginalDocument(documentRepository.getReferenceById(original.getId()));
        link.setFiscalDocument(documentRepository.getReferenceById(emitted.id()));
        link.setStatus(LinkStatus.PENDING);
        return link;
    }

    private void updateLinkStatusFromDocument(FiscalReturnLink link, FiscalDocumentResponse emitted) {
        if (emitted.status() == FiscalDocumentStatus.AUTHORIZED) {
            link.setStatus(LinkStatus.AUTHORIZED);
        } else if (emitted.status() == FiscalDocumentStatus.REJECTED
                || emitted.status() == FiscalDocumentStatus.VALIDATION_FAILED) {
            link.setStatus(LinkStatus.REJECTED);
        }
    }

    private FiscalReturnLinkResponse toResponse(FiscalReturnLink link) {
        return new FiscalReturnLinkResponse(
                link.getId(),
                link.getReturnType(),
                link.getReturnId(),
                link.getFiscalDocument() != null ? link.getFiscalDocument().getId() : null,
                link.getOriginalDocument() != null ? link.getOriginalDocument().getId() : null,
                link.getStatus());
    }
}
