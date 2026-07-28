package br.com.systemcommerce.fiscal.nfce;

import br.com.systemcommerce.fiscal.contingency.service.FiscalContingencyService;
import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentCreateRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentItemRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentPaymentRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentResponse;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.document.service.FiscalDocumentService;
import br.com.systemcommerce.fiscal.emission.FiscalEmissionOrchestrator;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberReservationResponse;
import br.com.systemcommerce.fiscal.numbering.service.FiscalNumberingService;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.sale.repository.SaleItemRepository;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NfceEmissionService {

    private static final Logger log = LoggerFactory.getLogger(NfceEmissionService.class);

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentRepository paymentRepository;
    private final FiscalEstablishmentRepository establishmentRepository;
    private final FiscalDocumentRepository documentRepository;
    private final FiscalNumberingService numberingService;
    private final FiscalDocumentService documentService;
    private final FiscalEmissionOrchestrator emissionOrchestrator;
    private final FiscalContingencyService contingencyService;

    @Transactional
    public FiscalDocumentResponse emitFromPosSale(UUID saleId) {
        return emitFromPosSaleInternal(saleId, "NFCE:SALE:" + saleId, false);
    }

    @Transactional
    public FiscalDocumentResponse emitFromPosSaleSoft(UUID saleId) {
        try {
            return emitFromPosSaleInternal(saleId, "NFCE:SALE:" + saleId, true);
        } catch (Exception ex) {
            log.warn("Emissão NFC-e soft-fail para venda {}: {}", saleId, ex.getMessage());
            if (isNetworkFailure(ex)) {
                saleRepository.findById(saleId).ifPresent(sale -> establishmentRepository
                        .findByStoreId(sale.getStore().getId())
                        .ifPresent(est -> contingencyService.maybeActivateOnNetworkFailure(
                                est.getId(), "65", ex.getMessage())));
            }
            markContingencyPending(saleId);
            return null;
        }
    }

    private FiscalDocumentResponse emitFromPosSaleInternal(UUID saleId, String idem, boolean soft) {
        Sale sale = saleRepository
                .findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        if (!sale.isPos()) {
            throw new BusinessRuleException("NFC-e PDV aplica-se somente a vendas POS");
        }
        if (sale.getStatus() != Sale.SaleStatus.PAID) {
            throw new BusinessRuleException("Venda PDV deve estar paga para NFC-e");
        }

        FiscalEstablishment establishment = establishmentRepository
                .findByStoreId(sale.getStore().getId())
                .orElseThrow(() -> new BusinessRuleException("Estabelecimento fiscal não configurado"));
        if (!Boolean.TRUE.equals(establishment.getAllowsNfce())) {
            if (soft) {
                return null;
            }
            throw new BusinessRuleException("Estabelecimento não habilitado para NFC-e");
        }

        return documentRepository
                .findByOrganizationIdAndIdempotencyKey(sale.getOrganization().getId(), idem)
                .map(d -> documentService.getById(d.getId()))
                .orElseGet(() -> doEmit(sale, establishment, idem));
    }

    private FiscalDocumentResponse doEmit(Sale sale, FiscalEstablishment establishment, String idem) {
        String series = establishment.getDefaultNfceSeries() != null ? establishment.getDefaultNfceSeries() : "1";
        FiscalNumberReservationResponse reservation = numberingService.reserveNext(
                establishment.getId(), "65", series, establishment.getFiscalEnvironment(), null, idem + ":NUM");

        List<SaleItem> saleItems = saleItemRepository.findBySaleId(sale.getId());
        List<FiscalDocumentItemRequest> items = new ArrayList<>();
        for (SaleItem si : saleItems) {
            items.add(new FiscalDocumentItemRequest(
                    si.getProduct().getId(),
                    si.getDescription(),
                    null,
                    null,
                    "5102",
                    si.getQuantity(),
                    si.getUnitPrice(),
                    null,
                    si.getProduct().getUnitOfMeasure(),
                    si.getProduct().getUnitOfMeasure()));
        }

        List<FiscalDocumentPaymentRequest> payments = paymentRepository.findBySaleIdOrderByCreatedAtAsc(sale.getId()).stream()
                .filter(Payment::isConfirmed)
                .map(p -> new FiscalDocumentPaymentRequest(mapPaymentCode(p.getMethod()), p.getAmount(), null))
                .toList();

        FiscalDocumentCreateRequest create = new FiscalDocumentCreateRequest(
                sale.getOrganization().getId(),
                establishment.getId(),
                sale.getStore().getId(),
                "65",
                series,
                establishment.getFiscalEnvironment(),
                "VENDA",
                "NORMAL",
                null,
                FiscalDocument.DocumentDirection.OUT,
                null,
                null,
                null,
                null,
                "SALE",
                sale.getId(),
                idem,
                false,
                items,
                payments);

        FiscalDocumentResponse draft = documentService.createDraftWithReservedNumber(create, reservation.number());
        numberingService.consumeReservation(reservation.id(), draft.id());
        return emissionOrchestrator.emitPipeline(draft);
    }

    private void markContingencyPending(UUID saleId) {
        documentRepository
                .findFirstByOriginDocumentTypeAndOriginDocumentIdAndModelAndActiveTrue("SALE", saleId, "65")
                .ifPresent(doc -> {
                    doc.setContingency(true);
                    doc.setStatus(FiscalDocumentStatus.CONTINGENCY_PENDING);
                    documentRepository.save(doc);
                });
    }

    private static boolean isNetworkFailure(Throwable ex) {
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        return ex instanceof java.net.SocketException
                || ex instanceof java.net.ConnectException
                || ex instanceof java.net.UnknownHostException
                || msg.contains("timeout")
                || msg.contains("network")
                || msg.contains("connection");
    }

    private static String mapPaymentCode(Payment.PaymentMethod method) {
        return switch (method) {
            case CASH -> "01";
            case CREDIT_CARD, DEBIT_CARD -> "03";
            case PIX -> "17";
            default -> "99";
        };
    }
}
