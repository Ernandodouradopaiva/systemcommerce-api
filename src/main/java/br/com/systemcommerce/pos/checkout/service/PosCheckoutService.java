package br.com.systemcommerce.pos.checkout.service;

import br.com.systemcommerce.finance.integration.PosFinanceIntegrationService;
import br.com.systemcommerce.payment.dto.PaymentCancelRequest;
import br.com.systemcommerce.payment.dto.PaymentCreateRequest;
import br.com.systemcommerce.payment.dto.PaymentRefundRequest;
import br.com.systemcommerce.payment.dto.PaymentResponse;
import br.com.systemcommerce.payment.dto.SaleChangeResponse;
import br.com.systemcommerce.payment.dto.SalePaymentBalanceResponse;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.payment.service.PaymentService;
import br.com.systemcommerce.payment.validation.PaymentFinancialCalculator;
import br.com.systemcommerce.pos.audit.PosAuditContexts;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditOutcome;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pos.checkout.dto.PosFinalizeResponse;
import br.com.systemcommerce.pos.checkout.dto.PosFinalizeStatusResponse;
import br.com.systemcommerce.pos.checkout.dto.PosPaymentAddRequest;
import br.com.systemcommerce.pos.receipt.service.PosReceiptService;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PosCheckoutService {

    private final SaleService saleService;
    private final SaleRepository saleRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final PosReceiptService posReceiptService;
    private final PosAuditService posAuditService;
    private final PosFinanceIntegrationService posFinanceIntegrationService;
    private final br.com.systemcommerce.fiscal.nfce.NfceEmissionService nfceEmissionService;

    @Transactional
    public PaymentResponse addPayment(UUID saleId, PosPaymentAddRequest request, String idempotencyKey) {
        Sale sale = requirePosSale(saleId);
        assertSessionOpen(sale);
        PaymentCreateRequest create = new PaymentCreateRequest(
                saleId,
                request.method(),
                request.amount(),
                null,
                request.externalReference(),
                request.notes(),
                request.installments(),
                request.tenderedAmount(),
                Boolean.TRUE.equals(request.confirmImmediately()),
                request.authorizationCode(),
                request.nsu(),
                request.cardBrand(),
                request.acquirer());
        try {
            return paymentService.register(create, idempotencyKey);
        } catch (BusinessRuleException ex) {
            posAuditService.recordIndependent(
                    PosAuditEventCode.PAYMENT_ATTEMPT,
                    PosAuditOutcome.FAILED,
                    PosAuditContexts.fromSale(sale)
                            .entity("Sale", saleId)
                            .details("Tentativa de pagamento inválida: " + ex.getMessage())
                            .errorCode("PAYMENT_ATTEMPT_FAILED")
                            .after(java.util.Map.of(
                                    "method", request.method() != null ? request.method().name() : null,
                                    "amount", request.amount()))
                            .build());
            throw ex;
        }
    }

    @Transactional
    public PaymentResponse removePending(UUID saleId, UUID paymentId) {
        requirePosSale(saleId);
        Payment payment = requirePaymentOfSale(saleId, paymentId);
        return paymentService.removePending(payment.getId());
    }

    @Transactional
    public PaymentResponse confirmPayment(UUID saleId, UUID paymentId) {
        Sale sale = requirePosSale(saleId);
        if (sale.isDraft()) {
            throw new BusinessRuleException(
                    "Confirme/finalize a venda antes de confirmar pagamentos individualmente");
        }
        requirePaymentOfSale(saleId, paymentId);
        return paymentService.confirm(paymentId);
    }

    @Transactional(readOnly = true)
    public SalePaymentBalanceResponse balance(UUID saleId) {
        requirePosSale(saleId);
        return paymentService.balance(saleId);
    }

    @Transactional(readOnly = true)
    public SaleChangeResponse change(UUID saleId, BigDecimal tenderedAmount) {
        requirePosSale(saleId);
        return paymentService.change(saleId, tenderedAmount);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(UUID saleId) {
        requirePosSale(saleId);
        return paymentService.listBySale(saleId);
    }

    @Transactional
    public PaymentResponse refund(UUID saleId, UUID paymentId, PaymentRefundRequest request) {
        requirePosSale(saleId);
        requirePaymentOfSale(saleId, paymentId);
        return paymentService.refund(paymentId, request);
    }

    /** Recusa pagamento pendente (ex.: TEF/cartão recusado). Não conclui a venda. */
    @Transactional
    public PaymentResponse refuse(UUID saleId, UUID paymentId, PaymentCancelRequest request) {
        requirePosSale(saleId);
        requirePaymentOfSale(saleId, paymentId);
        return paymentService.refuse(paymentId, request);
    }

    @Transactional(readOnly = true)
    public PosFinalizeStatusResponse finalizeStatus(UUID saleId) {
        Sale sale = requirePosSale(saleId);
        BigDecimal confirmed = paymentRepository.sumConfirmedAmountBySaleId(saleId);
        BigDecimal pending = paymentRepository.sumAmountBySaleIdAndStatus(saleId, Payment.PaymentStatus.PENDING);
        BigDecimal due = PaymentFinancialCalculator.balanceDue(sale.getTotalAmount(), confirmed);
        long pendingCount = paymentRepository.countBySaleIdAndStatus(saleId, Payment.PaymentStatus.PENDING);
        long confirmedCount = paymentRepository.countBySaleIdAndStatus(saleId, Payment.PaymentStatus.CONFIRMED);
        boolean finalized = sale.getStatus() == Sale.SaleStatus.PAID;
        BigDecimal pendingCover = pending != null ? pending : BigDecimal.ZERO;
        boolean ready = !finalized
                && !sale.isCancelled()
                && !sale.isSuspended()
                && sale.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                && confirmed.add(pendingCover).compareTo(sale.getTotalAmount()) >= 0;
        String message;
        if (finalized) {
            message = "Venda já finalizada";
        } else if (ready) {
            message = "Pronta para finalização";
        } else {
            message = "Saldo insuficiente ou venda sem cobertura de pagamentos";
        }
        return new PosFinalizeStatusResponse(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getStatus(),
                sale.getTotalAmount(),
                confirmed,
                pendingCover,
                due,
                pendingCount,
                confirmedCount,
                ready,
                finalized,
                message);
    }

    /**
     * Finalização atômica: confirma venda (estoque) se necessário, confirma pagamentos pendentes,
     * exige cobertura total. Falha em qualquer passo reverte a transação.
     */
    @Transactional
    public PosFinalizeResponse finalizeSale(UUID saleId, String idempotencyKey) {
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        if (!sale.isPos()) {
            throw new BusinessRuleException("Finalização PDV aplica-se somente a vendas do canal POS");
        }
        if (sale.isCancelled()) {
            throw new BusinessRuleException("Venda cancelada não pode ser finalizada");
        }
        if (sale.isSuspended()) {
            throw new BusinessRuleException("Recupere a venda suspensa antes de finalizar");
        }

        if (StringUtils.hasText(idempotencyKey)
                && idempotencyKey.trim().equals(sale.getLastOperationIdempotencyKey())
                && sale.getStatus() == Sale.SaleStatus.PAID) {
            return buildFinalizeResponse(saleId);
        }

        if (sale.getStatus() == Sale.SaleStatus.PAID) {
            if (StringUtils.hasText(idempotencyKey)) {
                sale.setLastOperationIdempotencyKey(idempotencyKey.trim());
                saleRepository.save(sale);
            }
            return buildFinalizeResponse(saleId);
        }

        assertSessionOpen(sale);

        if (sale.isDraft()) {
            saleService.confirm(saleId);
            sale = saleRepository
                    .findByIdForUpdate(saleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        }

        List<Payment> pending = paymentRepository.findBySaleIdOrderByCreatedAtAsc(saleId).stream()
                .filter(Payment::isPending)
                .toList();
        for (Payment payment : pending) {
            paymentService.confirm(payment.getId());
        }

        BigDecimal confirmed = paymentRepository.sumConfirmedAmountBySaleId(saleId);
        BigDecimal due = PaymentFinancialCalculator.balanceDue(sale.getTotalAmount(), confirmed);
        if (due.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException(
                    "Valor confirmado insuficiente para finalizar a venda. Saldo restante: " + due);
        }
        if (confirmed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("É necessário ao menos um pagamento confirmado para finalizar");
        }

        sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        if (StringUtils.hasText(idempotencyKey)) {
            sale.setLastOperationIdempotencyKey(idempotencyKey.trim());
            saleRepository.save(sale);
        }

        // Conta a receber + liquidação por meio (Prompt 104)
        posFinanceIntegrationService.onPosSaleFinalized(sale);

        // NFC-e soft-fail — não reverte checkout (Prompt 135)
        nfceEmissionService.emitFromPosSaleSoft(saleId);

        return buildFinalizeResponse(saleId);
    }

    private PosFinalizeResponse buildFinalizeResponse(UUID saleId) {
        SaleResponse sale = saleService.getById(saleId);
        List<PaymentResponse> payments = paymentService.listBySale(saleId);
        BigDecimal confirmed = paymentRepository.sumConfirmedAmountBySaleId(saleId);
        BigDecimal due = PaymentFinancialCalculator.balanceDue(sale.totalAmount(), confirmed);
        BigDecimal changeTotal = payments.stream()
                .filter(p -> p.status() == Payment.PaymentStatus.CONFIRMED)
                .map(PaymentResponse::changeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        var printData = posReceiptService.buildSaleReceipt(saleId);

        return new PosFinalizeResponse(
                sale,
                payments,
                sale.totalAmount(),
                due,
                changeTotal,
                sale.status(),
                sale.saleNumber(),
                printData);
    }

    private Sale requirePosSale(UUID saleId) {
        Sale sale = saleService.requireExists(saleId);
        if (!sale.isPos()) {
            throw new BusinessRuleException("Operação disponível somente para vendas do PDV");
        }
        return sale;
    }

    private Payment requirePaymentOfSale(UUID saleId, UUID paymentId) {
        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", paymentId));
        if (!payment.getSale().getId().equals(saleId)) {
            throw new BusinessRuleException("Pagamento não pertence à venda informada");
        }
        return payment;
    }

    private void assertSessionOpen(Sale sale) {
        if (sale.getCashSession() == null || !sale.getCashSession().acceptsOperations()) {
            throw new BusinessRuleException("Sessão de caixa aberta é obrigatória para operações de pagamento do PDV");
        }
    }
}
