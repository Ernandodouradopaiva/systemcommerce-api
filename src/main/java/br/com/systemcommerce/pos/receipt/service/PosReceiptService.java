package br.com.systemcommerce.pos.receipt.service;

import br.com.systemcommerce.payment.dto.PaymentResponse;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.payment.service.PaymentService;
import br.com.systemcommerce.payment.validation.PaymentFinancialCalculator;
import br.com.systemcommerce.pos.audit.PosAuditContext;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pos.cancellation.dto.SaleCancellationResponse;
import br.com.systemcommerce.pos.cancellation.service.PosCancellationService;
import br.com.systemcommerce.pos.cash.dto.CashClosingReceiptResponse;
import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.cash.service.CashPhysicalBalanceCalculator;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.receipt.dto.PosReceiptResponse;
import br.com.systemcommerce.pos.receipt.dto.ReceiptPrintLogResponse;
import br.com.systemcommerce.pos.receipt.dto.ReceiptPrintRequest;
import br.com.systemcommerce.pos.receipt.dto.ReceiptReprintRequest;
import br.com.systemcommerce.pos.receipt.entity.ReceiptPrintLog;
import br.com.systemcommerce.pos.receipt.repository.ReceiptPrintLogRepository;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PosReceiptService {

    public static final String DOCUMENT_DISCLAIMER = "DOCUMENTO NÃO FISCAL";
    public static final String DEFAULT_FOOTER = "Obrigado pela preferência!";

    private final ReceiptPrintLogRepository receiptPrintLogRepository;
    private final SaleService saleService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final CashSessionService cashSessionService;
    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final CashPhysicalBalanceCalculator physicalBalanceCalculator;
    private final PosCancellationService posCancellationService;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final PosAuditService posAuditService;

    @Transactional(readOnly = true)
    public PosReceiptResponse getReceipt(
            ReceiptPrintLog.PrintType type,
            UUID saleId,
            UUID paymentId,
            UUID cashSessionId,
            UUID cashMovementId,
            UUID saleCancellationId) {
        if (type == ReceiptPrintLog.PrintType.REPRINT) {
            throw new BusinessRuleException("Use POST /reprint para segunda via; informe o tipo original no GET");
        }
        return buildReceipt(
                type, saleId, paymentId, cashSessionId, cashMovementId, saleCancellationId, false, null, null, null, null);
    }

    /** Payload oficial de venda para finalize / impressão (sem registrar log). */
    @Transactional(readOnly = true)
    public PosReceiptResponse buildSaleReceipt(UUID saleId) {
        return buildReceipt(
                ReceiptPrintLog.PrintType.SALE, saleId, null, null, null, null, false, null, null, null, null);
    }

    @Transactional
    public PosReceiptResponse registerPrint(ReceiptPrintRequest request) {
        if (request.type() == ReceiptPrintLog.PrintType.REPRINT) {
            throw new BusinessRuleException("Use o endpoint de reimpressão para segunda via");
        }
        PosReceiptResponse data = buildReceipt(
                request.type(),
                request.saleId(),
                request.paymentId(),
                request.cashSessionId(),
                request.cashMovementId(),
                request.saleCancellationId(),
                false,
                null,
                null,
                null,
                null);

        ReceiptPrintLog log = persistLog(
                request.type(),
                false,
                null,
                null,
                request.copies(),
                request.layout(),
                request.notes(),
                data,
                request.cashSessionId());
        return withPrintMeta(data, log, false, null, request.copies());
    }

    @Transactional
    public PosReceiptResponse registerReprint(ReceiptReprintRequest request) {
        if (!StringUtils.hasText(request.reason())) {
            throw new BusinessRuleException("Motivo da reimpressão é obrigatório");
        }

        ReceiptPrintLog.PrintType contentType;
        UUID saleId = request.saleId();
        UUID paymentId = request.paymentId();
        UUID cashSessionId = request.cashSessionId();
        UUID cashMovementId = request.cashMovementId();
        UUID saleCancellationId = request.saleCancellationId();
        UUID originalLogId = request.originalLogId();

        if (originalLogId != null) {
            ReceiptPrintLog original = receiptPrintLogRepository
                    .findById(originalLogId)
                    .orElseThrow(() -> new ResourceNotFoundException("Registro de impressão", originalLogId));
            if (saleId == null) {
                saleId = original.getSaleId();
            }
            if (paymentId == null) {
                paymentId = original.getPaymentId();
            }
            if (cashSessionId == null) {
                cashSessionId = original.getCashSessionId();
            }
            if (cashMovementId == null) {
                cashMovementId = original.getCashMovementId();
            }
            if (saleCancellationId == null) {
                saleCancellationId = original.getSaleCancellationId();
            }
            if (original.getPrintType() == ReceiptPrintLog.PrintType.REPRINT) {
                contentType = request.type() != null && request.type() != ReceiptPrintLog.PrintType.REPRINT
                        ? request.type()
                        : resolveContentTypeFromLog(original);
            } else {
                contentType = original.getPrintType();
            }
        } else if (request.type() != null && request.type() != ReceiptPrintLog.PrintType.REPRINT) {
            contentType = request.type();
        } else {
            throw new BusinessRuleException("Informe originalLogId ou o tipo do comprovante a reimprimir");
        }

        if (saleId == null
                && contentType != ReceiptPrintLog.PrintType.OPENING
                && contentType != ReceiptPrintLog.PrintType.SESSION_CLOSE
                && contentType != ReceiptPrintLog.PrintType.CASH_SUPPLY
                && contentType != ReceiptPrintLog.PrintType.CASH_WITHDRAWAL
                && contentType != ReceiptPrintLog.PrintType.CANCELLATION
                && contentType != ReceiptPrintLog.PrintType.PAYMENT) {
            throw new BusinessRuleException("Venda relacionada é obrigatória para reimpressão deste comprovante");
        }

        PosReceiptResponse data = buildReceipt(
                contentType,
                saleId,
                paymentId,
                cashSessionId,
                cashMovementId,
                saleCancellationId,
                true,
                request.reason().trim(),
                null,
                null,
                null);

        ReceiptPrintLog log = persistLog(
                ReceiptPrintLog.PrintType.REPRINT,
                true,
                originalLogId,
                request.reason().trim(),
                request.copies(),
                request.layout(),
                request.notes(),
                data,
                cashSessionId);
        if (log.getSaleId() == null && saleId != null) {
            log.setSaleId(saleId);
            receiptPrintLogRepository.save(log);
        }

        return withPrintMeta(data, log, true, request.reason().trim(), request.copies());
    }

    @Transactional(readOnly = true)
    public Page<ReceiptPrintLogResponse> history(
            UUID saleId,
            UUID cashSessionId,
            ReceiptPrintLog.PrintType printType,
            Boolean isReprint,
            Pageable pageable) {
        Specification<ReceiptPrintLog> spec = (root, q, cb) -> cb.conjunction();
        if (saleId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("saleId"), saleId));
        }
        if (cashSessionId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("cashSessionId"), cashSessionId));
        }
        if (printType != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("printType"), printType));
        }
        if (isReprint != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("isReprint"), isReprint));
        }
        return receiptPrintLogRepository.findAll(spec, pageable).map(this::toLogResponse);
    }

    private ReceiptPrintLog.PrintType resolveContentTypeFromLog(ReceiptPrintLog log) {
        if (log.getSaleCancellationId() != null) {
            return ReceiptPrintLog.PrintType.CANCELLATION;
        }
        if (log.getPaymentId() != null) {
            return ReceiptPrintLog.PrintType.PAYMENT;
        }
        if (log.getCashMovementId() != null) {
            CashMovement m = cashMovementRepository.findById(log.getCashMovementId()).orElse(null);
            if (m != null) {
                return switch (m.getType()) {
                    case OPENING -> ReceiptPrintLog.PrintType.OPENING;
                    case SUPPLY -> ReceiptPrintLog.PrintType.CASH_SUPPLY;
                    case WITHDRAWAL -> ReceiptPrintLog.PrintType.CASH_WITHDRAWAL;
                    default -> ReceiptPrintLog.PrintType.CASH_SUPPLY;
                };
            }
        }
        if (log.getCashSessionId() != null && log.getSaleId() == null) {
            return ReceiptPrintLog.PrintType.SESSION_CLOSE;
        }
        if (log.getSaleId() != null) {
            return ReceiptPrintLog.PrintType.SALE;
        }
        return ReceiptPrintLog.PrintType.SALE;
    }

    private PosReceiptResponse buildReceipt(
            ReceiptPrintLog.PrintType type,
            UUID saleId,
            UUID paymentId,
            UUID cashSessionId,
            UUID cashMovementId,
            UUID saleCancellationId,
            boolean reprint,
            String reprintReason,
            String authenticationId,
            Integer sequenceNo,
            UUID printLogId) {

        Instant issuedAt = Instant.now();
        return switch (type) {
            case SALE -> buildSale(
                    requireSaleId(saleId), reprint, reprintReason, authenticationId, sequenceNo, printLogId, issuedAt);
            case PAYMENT -> buildPayment(
                    requirePaymentId(paymentId),
                    reprint,
                    reprintReason,
                    authenticationId,
                    sequenceNo,
                    printLogId,
                    issuedAt);
            case OPENING -> buildOpening(
                    requireSessionId(cashSessionId),
                    reprint,
                    reprintReason,
                    authenticationId,
                    sequenceNo,
                    printLogId,
                    issuedAt);
            case CASH_SUPPLY, CASH_WITHDRAWAL -> buildCashMovement(
                    type,
                    requireMovementId(cashMovementId),
                    reprint,
                    reprintReason,
                    authenticationId,
                    sequenceNo,
                    printLogId,
                    issuedAt);
            case SESSION_CLOSE -> buildSessionClose(
                    requireSessionId(cashSessionId),
                    reprint,
                    reprintReason,
                    authenticationId,
                    sequenceNo,
                    printLogId,
                    issuedAt);
            case CANCELLATION -> buildCancellation(
                    requireCancellationId(saleCancellationId),
                    reprint,
                    reprintReason,
                    authenticationId,
                    sequenceNo,
                    printLogId,
                    issuedAt);
            case REPRINT -> throw new BusinessRuleException("Tipo REPRINT é apenas para registro de log");
        };
    }

    private PosReceiptResponse buildSale(
            UUID saleId,
            boolean reprint,
            String reprintReason,
            String authenticationId,
            Integer sequenceNo,
            UUID printLogId,
            Instant issuedAt) {
        SaleResponse sale = saleService.getById(saleId);
        if (sale.channel() != Sale.SaleChannel.POS) {
            throw new BusinessRuleException("Comprovante de venda disponível somente para vendas do PDV");
        }
        List<PaymentResponse> payments = paymentService.listBySale(saleId);
        BigDecimal confirmed = paymentRepository.sumConfirmedAmountBySaleId(saleId);
        BigDecimal due = PaymentFinancialCalculator.balanceDue(sale.totalAmount(), confirmed);
        BigDecimal changeTotal = payments.stream()
                .filter(p -> p.status() == Payment.PaymentStatus.CONFIRMED)
                .map(PaymentResponse::changeAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Store store = requireStoreFromSale(sale);
        PosTerminal terminal = sale.terminalId() != null ? loadTerminalFromSale(sale) : null;
        User operator = loadSeller(sale);

        List<PosReceiptResponse.ItemLine> items = new ArrayList<>();
        if (sale.items() != null) {
            sale.items()
                    .forEach(i -> items.add(new PosReceiptResponse.ItemLine(
                            i.productName(),
                            i.productSku(),
                            i.quantity(),
                            i.unitPrice(),
                            i.discountAmount() != null ? i.discountAmount() : BigDecimal.ZERO,
                            i.lineTotal())));
        }

        List<PosReceiptResponse.PaymentLine> paymentLines = payments.stream().map(this::toPaymentLine).toList();

        String title = reprint ? "Comprovante de venda — 2ª via" : "Comprovante de venda";

        return base(
                ReceiptPrintLog.PrintType.SALE,
                title,
                reprint,
                reprintReason,
                authenticationId,
                sequenceNo,
                printLogId,
                issuedAt,
                store,
                terminal,
                operator,
                customerOf(sale),
                new PosReceiptResponse.SaleBlock(
                        sale.id(),
                        sale.saleNumber(),
                        sale.saleNumber(),
                        sale.status(),
                        sale.saleDate(),
                        sale.cashSessionId()),
                items,
                new PosReceiptResponse.TotalsBlock(
                        sale.subtotal(),
                        sale.discountAmount(),
                        sale.surchargeAmount(),
                        sale.totalAmount(),
                        confirmed,
                        due,
                        changeTotal),
                paymentLines,
                null,
                null,
                null,
                null);
    }

    private PosReceiptResponse buildPayment(
            UUID paymentId,
            boolean reprint,
            String reprintReason,
            String authenticationId,
            Integer sequenceNo,
            UUID printLogId,
            Instant issuedAt) {
        Payment payment = paymentRepository
                .findDetailedById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", paymentId));
        Sale saleEntity = payment.getSale();
        if (saleEntity == null || !saleEntity.isPos()) {
            throw new BusinessRuleException("Comprovante de pagamento disponível somente para vendas do PDV");
        }
        PosReceiptResponse saleReceipt = buildSale(
                saleEntity.getId(), reprint, reprintReason, authenticationId, sequenceNo, printLogId, issuedAt);

        String title = reprint ? "Comprovante de pagamento — 2ª via" : "Comprovante de pagamento";
        PosReceiptResponse.PaymentDetailBlock detail = new PosReceiptResponse.PaymentDetailBlock(
                payment.getId(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getAppliedAmount(),
                payment.getChangeAmount(),
                payment.getAuthorizationCode(),
                payment.getNsu(),
                payment.getPaidAt());

        return new PosReceiptResponse(
                ReceiptPrintLog.PrintType.PAYMENT,
                title,
                saleReceipt.nonFiscal(),
                saleReceipt.documentDisclaimer(),
                saleReceipt.footerMessage(),
                saleReceipt.authenticationId(),
                saleReceipt.sequenceNo(),
                saleReceipt.issuedAt(),
                saleReceipt.suggestedLayout(),
                saleReceipt.printerName(),
                reprint,
                reprintReason,
                saleReceipt.copies(),
                saleReceipt.printLogId(),
                saleReceipt.store(),
                saleReceipt.terminal(),
                saleReceipt.operator(),
                saleReceipt.customer(),
                saleReceipt.sale(),
                saleReceipt.items(),
                saleReceipt.totals(),
                saleReceipt.payments(),
                null,
                null,
                null,
                detail);
    }

    private PosReceiptResponse buildOpening(
            UUID sessionId,
            boolean reprint,
            String reprintReason,
            String authenticationId,
            Integer sequenceNo,
            UUID printLogId,
            Instant issuedAt) {
        CashSession session = cashSessionRepository
                .findDetailedById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de caixa", sessionId));

        CashMovement opening = cashMovementRepository.findByCashSessionIdOrderByOccurredAtAsc(sessionId).stream()
                .filter(m -> m.getType() == CashMovement.MovementType.OPENING)
                .findFirst()
                .orElse(null);

        PosReceiptResponse.CashMovementBlock movementBlock = null;
        if (opening != null) {
            movementBlock = toMovementBlock(opening, sessionId);
        } else {
            movementBlock = new PosReceiptResponse.CashMovementBlock(
                    null,
                    CashMovement.MovementType.OPENING,
                    session.getOpeningAmount(),
                    session.getOpenedAt(),
                    "OPENING",
                    null,
                    session.getOperator() != null ? session.getOperator().getName() : null,
                    null,
                    physicalBalanceCalculator.expectedPhysicalCash(sessionId));
        }

        String title = reprint ? "Comprovante de abertura — 2ª via" : "Comprovante de abertura de caixa";
        return base(
                ReceiptPrintLog.PrintType.OPENING,
                title,
                reprint,
                reprintReason,
                authenticationId,
                sequenceNo,
                printLogId,
                issuedAt,
                session.getStore(),
                session.getTerminal(),
                session.getOperator(),
                null,
                null,
                List.of(),
                null,
                List.of(),
                movementBlock,
                null,
                null,
                null);
    }

    private PosReceiptResponse buildCashMovement(
            ReceiptPrintLog.PrintType type,
            UUID movementId,
            boolean reprint,
            String reprintReason,
            String authenticationId,
            Integer sequenceNo,
            UUID printLogId,
            Instant issuedAt) {
        CashMovement movement = cashMovementRepository
                .findDetailedById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação de caixa", movementId));

        CashSession session = cashSessionRepository
                .findDetailedById(movement.getCashSession().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sessão de caixa", movement.getCashSession().getId()));

        if (type == ReceiptPrintLog.PrintType.CASH_SUPPLY && movement.getType() != CashMovement.MovementType.SUPPLY) {
            throw new BusinessRuleException("Movimentação não é um suprimento");
        }
        if (type == ReceiptPrintLog.PrintType.CASH_WITHDRAWAL
                && movement.getType() != CashMovement.MovementType.WITHDRAWAL) {
            throw new BusinessRuleException("Movimentação não é uma sangria");
        }

        String title =
                switch (type) {
                    case CASH_SUPPLY -> reprint
                            ? "Comprovante de suprimento — 2ª via"
                            : "Comprovante de suprimento";
                    case CASH_WITHDRAWAL -> reprint
                            ? "Comprovante de sangria — 2ª via"
                            : "Comprovante de sangria";
                    default -> "Comprovante de movimento de caixa";
                };

        return base(
                type,
                title,
                reprint,
                reprintReason,
                authenticationId,
                sequenceNo,
                printLogId,
                issuedAt,
                session.getStore(),
                session.getTerminal(),
                movement.getExecutedBy() != null ? movement.getExecutedBy() : session.getOperator(),
                null,
                null,
                List.of(),
                null,
                List.of(),
                toMovementBlock(movement, session.getId()),
                null,
                null,
                null);
    }

    private PosReceiptResponse buildSessionClose(
            UUID sessionId,
            boolean reprint,
            String reprintReason,
            String authenticationId,
            Integer sequenceNo,
            UUID printLogId,
            Instant issuedAt) {
        CashClosingReceiptResponse closing = cashSessionService.closingReceipt(sessionId);
        CashSession session = cashSessionRepository
                .findDetailedById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de caixa", sessionId));

        List<PosReceiptResponse.MethodTotalLine> methods = closing.byPaymentMethod().stream()
                .map(m -> new PosReceiptResponse.MethodTotalLine(m.method(), m.amount()))
                .toList();

        PosReceiptResponse.SessionCloseBlock block = new PosReceiptResponse.SessionCloseBlock(
                closing.sessionId(),
                closing.status(),
                closing.openedAt(),
                closing.closedAt(),
                closing.openingAmount(),
                closing.supplies(),
                closing.withdrawals(),
                closing.salesReceived(),
                closing.cancellations(),
                closing.refunds(),
                closing.salesCount(),
                closing.cancelledSalesCount(),
                closing.expectedCash(),
                closing.expectedGeneral(),
                closing.countedAmount(),
                closing.differenceAmount(),
                closing.closingNotes(),
                methods);

        String title = reprint ? "Comprovante de fechamento — 2ª via" : "Comprovante de fechamento de caixa";
        return base(
                ReceiptPrintLog.PrintType.SESSION_CLOSE,
                title,
                reprint,
                reprintReason,
                authenticationId,
                sequenceNo,
                printLogId,
                issuedAt,
                session.getStore(),
                session.getTerminal(),
                session.getOperator(),
                null,
                null,
                List.of(),
                null,
                List.of(),
                null,
                block,
                null,
                null);
    }

    private PosReceiptResponse buildCancellation(
            UUID cancellationId,
            boolean reprint,
            String reprintReason,
            String authenticationId,
            Integer sequenceNo,
            UUID printLogId,
            Instant issuedAt) {
        SaleCancellationResponse cancellation = posCancellationService.getById(cancellationId);
        SaleResponse sale = saleService.getById(cancellation.saleId());
        Store store = requireStoreFromSale(sale);
        PosTerminal terminal = sale.terminalId() != null ? loadTerminalFromSale(sale) : null;

        List<PosReceiptResponse.RefundLine> refunds = cancellation.refunds() == null
                ? List.of()
                : cancellation.refunds().stream()
                        .map(r -> new PosReceiptResponse.RefundLine(
                                r.id(), r.status() != null ? r.status().name() : null, r.amount(), r.method()))
                        .toList();

        PosReceiptResponse.CancellationBlock block = new PosReceiptResponse.CancellationBlock(
                cancellation.id(),
                cancellation.saleId(),
                cancellation.saleNumber(),
                cancellation.status() != null ? cancellation.status().name() : null,
                cancellation.reason(),
                cancellation.requestedAt(),
                cancellation.executedAt(),
                cancellation.requestedByName(),
                cancellation.authorizedByName(),
                cancellation.executedByName(),
                refunds);

        String title = reprint ? "Comprovante de cancelamento — 2ª via" : "Comprovante de cancelamento";
        return base(
                ReceiptPrintLog.PrintType.CANCELLATION,
                title,
                reprint,
                reprintReason,
                authenticationId,
                sequenceNo,
                printLogId,
                issuedAt,
                store,
                terminal,
                loadSeller(sale),
                customerOf(sale),
                new PosReceiptResponse.SaleBlock(
                        sale.id(),
                        sale.saleNumber(),
                        sale.saleNumber(),
                        sale.status(),
                        sale.saleDate(),
                        sale.cashSessionId()),
                List.of(),
                null,
                List.of(),
                null,
                null,
                block,
                null);
    }

    private PosReceiptResponse base(
            ReceiptPrintLog.PrintType type,
            String title,
            boolean reprint,
            String reprintReason,
            String authenticationId,
            Integer sequenceNo,
            UUID printLogId,
            Instant issuedAt,
            Store store,
            PosTerminal terminal,
            User operator,
            PosReceiptResponse.CustomerBlock customer,
            PosReceiptResponse.SaleBlock sale,
            List<PosReceiptResponse.ItemLine> items,
            PosReceiptResponse.TotalsBlock totals,
            List<PosReceiptResponse.PaymentLine> payments,
            PosReceiptResponse.CashMovementBlock cashMovement,
            PosReceiptResponse.SessionCloseBlock sessionClose,
            PosReceiptResponse.CancellationBlock cancellation,
            PosReceiptResponse.PaymentDetailBlock paymentDetail) {

        PosTerminal.PrintModel layout =
                terminal != null && terminal.getPrintModel() != null
                        ? terminal.getPrintModel()
                        : PosTerminal.PrintModel.THERMAL_80;
        String printerName = terminal != null ? terminal.getPrinterName() : null;

        return new PosReceiptResponse(
                type,
                title,
                true,
                DOCUMENT_DISCLAIMER,
                DEFAULT_FOOTER,
                authenticationId,
                sequenceNo,
                issuedAt,
                layout == PosTerminal.PrintModel.NONE ? PosTerminal.PrintModel.THERMAL_80 : layout,
                printerName,
                reprint,
                reprintReason,
                null,
                printLogId,
                toStoreBlock(store),
                toTerminalBlock(terminal),
                toOperatorBlock(operator),
                customer,
                sale,
                items != null ? items : List.of(),
                totals,
                payments != null ? payments : List.of(),
                cashMovement,
                sessionClose,
                cancellation,
                paymentDetail);
    }

    private ReceiptPrintLog persistLog(
            ReceiptPrintLog.PrintType logType,
            boolean isReprint,
            UUID originalLogId,
            String reason,
            Integer copies,
            ReceiptPrintLog.PrintLayout layout,
            String notes,
            PosReceiptResponse data,
            UUID cashSessionIdHint) {

        User user = userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));

        int sequence = receiptPrintLogRepository.nextSequence().intValue();
        String authId = String.format("SC-%s-%06d", logType.name(), sequence);

        ReceiptPrintLog log = new ReceiptPrintLog();
        log.setPrintType(logType);
        log.setSequenceNo(sequence);
        log.setSaleId(data.sale() != null ? data.sale().id() : null);
        if (data.paymentDetail() != null) {
            log.setPaymentId(data.paymentDetail().paymentId());
        }
        if (data.sale() != null) {
            log.setCashSessionId(data.sale().cashSessionId());
        }
        if (data.sessionClose() != null) {
            log.setCashSessionId(data.sessionClose().sessionId());
        }
        if (data.cashMovement() != null) {
            log.setCashMovementId(data.cashMovement().id());
        }
        if (data.cancellation() != null) {
            log.setSaleCancellationId(data.cancellation().id());
            if (log.getSaleId() == null) {
                log.setSaleId(data.cancellation().saleId());
            }
        }
        if (log.getCashSessionId() == null && cashSessionIdHint != null) {
            log.setCashSessionId(cashSessionIdHint);
        }
        log.setRequestedBy(user);
        log.setReason(reason);
        log.setCopies(copies != null ? copies : 1);
        log.setLayout(layout);
        log.setIsReprint(isReprint);
        log.setOriginalLogId(originalLogId);
        log.setAuthenticationId(authId);
        log.setTerminalId(data.terminal() != null ? data.terminal().id() : null);
        log.setNotes(notes);

        if (log.getCashMovementId() != null && log.getCashSessionId() == null) {
            cashMovementRepository.findById(log.getCashMovementId()).ifPresent(m -> {
                log.setCashSessionId(m.getCashSession().getId());
            });
        }

        ReceiptPrintLog saved = receiptPrintLogRepository.save(log);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", saved.getId());
        map.put("printType", saved.getPrintType().name());
        map.put("sequenceNo", saved.getSequenceNo());
        map.put("saleId", saved.getSaleId());
        map.put("isReprint", saved.getIsReprint());
        map.put("reason", saved.getReason());
        map.put("copies", saved.getCopies());
        map.put("layout", saved.getLayout().name());
        map.put("authenticationId", saved.getAuthenticationId());
        domainAuditService.record(
                "POS",
                "ReceiptPrintLog",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                map,
                isReprint ? "Reimpressão de comprovante PDV" : "Impressão de comprovante PDV");
        posAuditService.success(
                isReprint ? PosAuditEventCode.RECEIPT_REPRINT : PosAuditEventCode.RECEIPT_PRINT,
                PosAuditContext.builder()
                        .storeId(data.store() != null ? data.store().id() : null)
                        .terminalId(saved.getTerminalId())
                        .cashSessionId(saved.getCashSessionId())
                        .saleId(saved.getSaleId())
                        .operatorId(user != null ? user.getId() : null)
                        .entity("ReceiptPrintLog", saved.getId())
                        .action(AuditLog.AuditAction.CREATE)
                        .after(map)
                        .details(isReprint ? "Reimpressão de comprovante PDV" : "Impressão de comprovante PDV")
                        .build());

        return saved;
    }

    private PosReceiptResponse withPrintMeta(
            PosReceiptResponse data, ReceiptPrintLog log, boolean reprint, String reason, Integer copies) {
        return new PosReceiptResponse(
                data.type(),
                data.title(),
                data.nonFiscal(),
                data.documentDisclaimer(),
                data.footerMessage(),
                log.getAuthenticationId(),
                log.getSequenceNo(),
                data.issuedAt(),
                data.suggestedLayout(),
                data.printerName(),
                reprint,
                reason,
                copies,
                log.getId(),
                data.store(),
                data.terminal(),
                data.operator(),
                data.customer(),
                data.sale(),
                data.items(),
                data.totals(),
                data.payments(),
                data.cashMovement(),
                data.sessionClose(),
                data.cancellation(),
                data.paymentDetail());
    }

    private ReceiptPrintLogResponse toLogResponse(ReceiptPrintLog log) {
        return new ReceiptPrintLogResponse(
                log.getId(),
                log.getPrintType(),
                log.getSequenceNo(),
                log.getSaleId(),
                log.getPaymentId(),
                log.getCashSessionId(),
                log.getCashMovementId(),
                log.getSaleCancellationId(),
                log.getRequestedBy() != null ? log.getRequestedBy().getId() : null,
                log.getRequestedBy() != null ? log.getRequestedBy().getName() : null,
                log.getReason(),
                log.getCopies(),
                log.getLayout(),
                log.getIsReprint(),
                log.getOriginalLogId(),
                log.getAuthenticationId(),
                log.getTerminalId(),
                log.getNotes(),
                log.getCreatedAt());
    }

    private PosReceiptResponse.StoreBlock toStoreBlock(Store store) {
        if (store == null) {
            return null;
        }
        return new PosReceiptResponse.StoreBlock(
                store.getId(),
                store.getCode(),
                store.getName(),
                store.getTradeName(),
                store.getDocument(),
                store.getStateRegistration(),
                store.getPhone(),
                store.getEmail(),
                formatAddress(store),
                store.getZipCode(),
                store.getCity(),
                store.getState());
    }

    private PosReceiptResponse.TerminalBlock toTerminalBlock(PosTerminal terminal) {
        if (terminal == null) {
            return null;
        }
        return new PosReceiptResponse.TerminalBlock(
                terminal.getId(),
                terminal.getCode(),
                terminal.getTerminalNumber(),
                terminal.getName(),
                terminal.getPrintModel(),
                terminal.getPrinterName());
    }

    private PosReceiptResponse.OperatorBlock toOperatorBlock(User operator) {
        if (operator == null) {
            return null;
        }
        return new PosReceiptResponse.OperatorBlock(operator.getId(), operator.getName(), operator.getLogin());
    }

    private PosReceiptResponse.CustomerBlock customerOf(SaleResponse sale) {
        if (sale.customerId() == null) {
            return null;
        }
        Sale entity = saleService.requireExists(sale.id());
        String document = entity.getCustomer() != null ? entity.getCustomer().getDocument() : null;
        return new PosReceiptResponse.CustomerBlock(sale.customerId(), sale.customerName(), document);
    }

    private PosReceiptResponse.PaymentLine toPaymentLine(PaymentResponse p) {
        return new PosReceiptResponse.PaymentLine(
                p.id(),
                p.method(),
                p.status(),
                p.amount(),
                p.appliedAmount(),
                p.changeAmount(),
                p.authorizationCode(),
                p.nsu(),
                p.cardBrand(),
                p.paidAt());
    }

    private PosReceiptResponse.CashMovementBlock toMovementBlock(CashMovement movement, UUID sessionId) {
        String reason = movement.getReason();
        if (movement.getMovementReason() != null) {
            reason = movement.getMovementReason().getDescription() != null
                    ? movement.getMovementReason().getDescription()
                    : movement.getMovementReason().getCode();
        }
        return new PosReceiptResponse.CashMovementBlock(
                movement.getId(),
                movement.getType(),
                movement.getAmount(),
                movement.getOccurredAt(),
                reason,
                movement.getNotes() != null ? movement.getNotes() : movement.getDescription(),
                movement.getExecutedBy() != null ? movement.getExecutedBy().getName() : null,
                movement.getAuthorizedBy() != null ? movement.getAuthorizedBy().getName() : null,
                physicalBalanceCalculator.expectedPhysicalCash(sessionId));
    }

    private String formatAddress(Store store) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(store.getStreet())) {
            sb.append(store.getStreet());
        }
        if (StringUtils.hasText(store.getNumber())) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(store.getNumber());
        }
        if (StringUtils.hasText(store.getComplement())) {
            if (!sb.isEmpty()) {
                sb.append(" — ");
            }
            sb.append(store.getComplement());
        }
        if (StringUtils.hasText(store.getDistrict())) {
            if (!sb.isEmpty()) {
                sb.append(" — ");
            }
            sb.append(store.getDistrict());
        }
        if (StringUtils.hasText(store.getCity()) || StringUtils.hasText(store.getState())) {
            if (!sb.isEmpty()) {
                sb.append(" — ");
            }
            if (StringUtils.hasText(store.getCity())) {
                sb.append(store.getCity());
            }
            if (StringUtils.hasText(store.getState())) {
                if (StringUtils.hasText(store.getCity())) {
                    sb.append('/');
                }
                sb.append(store.getState());
            }
        }
        if (StringUtils.hasText(store.getZipCode())) {
            if (!sb.isEmpty()) {
                sb.append(" — CEP ");
            } else {
                sb.append("CEP ");
            }
            sb.append(store.getZipCode());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private Store requireStoreFromSale(SaleResponse sale) {
        Sale entity = saleService.requireExists(sale.id());
        if (entity.getStore() == null) {
            throw new BusinessRuleException("Venda sem loja vinculada — impossível montar comprovante");
        }
        return entity.getStore();
    }

    private PosTerminal loadTerminalFromSale(SaleResponse sale) {
        Sale entity = saleService.requireExists(sale.id());
        return entity.getTerminal();
    }

    private User loadSeller(SaleResponse sale) {
        Sale entity = saleService.requireExists(sale.id());
        return entity.getSeller();
    }

    private static UUID requireSaleId(UUID saleId) {
        if (saleId == null) {
            throw new BusinessRuleException("saleId é obrigatório para este comprovante");
        }
        return saleId;
    }

    private static UUID requirePaymentId(UUID paymentId) {
        if (paymentId == null) {
            throw new BusinessRuleException("paymentId é obrigatório para comprovante de pagamento");
        }
        return paymentId;
    }

    private static UUID requireSessionId(UUID cashSessionId) {
        if (cashSessionId == null) {
            throw new BusinessRuleException("cashSessionId é obrigatório para este comprovante");
        }
        return cashSessionId;
    }

    private static UUID requireMovementId(UUID cashMovementId) {
        if (cashMovementId == null) {
            throw new BusinessRuleException("cashMovementId é obrigatório para este comprovante");
        }
        return cashMovementId;
    }

    private static UUID requireCancellationId(UUID saleCancellationId) {
        if (saleCancellationId == null) {
            throw new BusinessRuleException("saleCancellationId é obrigatório para comprovante de cancelamento");
        }
        return saleCancellationId;
    }
}
