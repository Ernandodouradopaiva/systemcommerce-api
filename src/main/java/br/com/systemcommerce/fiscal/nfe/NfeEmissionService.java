package br.com.systemcommerce.fiscal.nfe;

import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentCreateRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentItemRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentPaymentRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentResponse;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.emission.FiscalEmissionOrchestrator;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberReservationResponse;
import br.com.systemcommerce.fiscal.numbering.service.FiscalNumberingService;
import br.com.systemcommerce.fiscal.party.PartyType;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import br.com.systemcommerce.fiscal.document.service.FiscalDocumentService;

@Service
@RequiredArgsConstructor
public class NfeEmissionService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentRepository paymentRepository;
    private final FiscalEstablishmentRepository establishmentRepository;
    private final FiscalDocumentRepository documentRepository;
    private final FiscalNumberingService numberingService;
    private final FiscalDocumentService documentService;
    private final FiscalEmissionOrchestrator emissionOrchestrator;

    @Transactional
    public FiscalDocumentResponse emitFromSale(UUID saleId, String idempotencyKey) {
        Sale sale = saleRepository
                .findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        if (sale.isCancelled()) {
            throw new BusinessRuleException("Venda cancelada não pode gerar NF-e");
        }
        if (!sale.isConfirmedLike() && sale.getStatus() != Sale.SaleStatus.PAID) {
            throw new BusinessRuleException("Venda deve estar confirmada/paga para emissão de NF-e");
        }

        FiscalEstablishment establishment = establishmentRepository
                .findByStoreId(sale.getStore().getId())
                .orElseThrow(() -> new BusinessRuleException("Estabelecimento fiscal não configurado para a loja"));
        if (!Boolean.TRUE.equals(establishment.getAllowsNfe())) {
            throw new BusinessRuleException("Estabelecimento não habilitado para NF-e");
        }

        String idem = StringUtils.hasText(idempotencyKey) ? idempotencyKey : "NFE:SALE:" + saleId;
        return documentRepository
                .findByOrganizationIdAndIdempotencyKey(sale.getOrganization().getId(), idem)
                .map(d -> documentService.getById(d.getId()))
                .orElseGet(() -> doEmit(sale, establishment, idem));
    }

    private FiscalDocumentResponse doEmit(Sale sale, FiscalEstablishment establishment, String idem) {
        String series = establishment.getDefaultNfeSeries() != null ? establishment.getDefaultNfeSeries() : "1";
        FiscalNumberReservationResponse reservation = numberingService.reserveNext(
                establishment.getId(), "55", series, establishment.getFiscalEnvironment(), null, idem + ":NUM");

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

        PartyType recipientType = null;
        UUID recipientId = null;
        if (sale.getCustomer() != null) {
            recipientType = PartyType.CUSTOMER;
            recipientId = sale.getCustomer().getId();
        }

        FiscalDocumentCreateRequest create = new FiscalDocumentCreateRequest(
                sale.getOrganization().getId(),
                establishment.getId(),
                sale.getStore().getId(),
                "55",
                series,
                establishment.getFiscalEnvironment(),
                "VENDA",
                "NORMAL",
                null,
                FiscalDocument.DocumentDirection.OUT,
                recipientType,
                recipientId,
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

    private static String mapPaymentCode(Payment.PaymentMethod method) {
        return switch (method) {
            case CASH -> "01";
            case CREDIT_CARD, DEBIT_CARD -> "03";
            case PIX -> "17";
            default -> "99";
        };
    }
}
