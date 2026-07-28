package br.com.systemcommerce.pos.resilience.service;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.resilience.dto.PosOperationLookupResponse;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Consulta o status real de operações PDV pela Idempotency-Key (resposta perdida / reenvio seguro).
 */
@Service
@RequiredArgsConstructor
public class PosOperationLookupService {

    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public PosOperationLookupResponse lookup(String idempotencyKey) {
        assertCanLookup();
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessRuleException("Idempotency-Key é obrigatória");
        }
        String key = idempotencyKey.trim();

        var paymentOpt = paymentRepository.findByIdempotencyKey(key);
        if (paymentOpt.isPresent()) {
            return fromPayment(key, paymentOpt.get());
        }

        var saleByStart = saleRepository.findByIdempotencyKey(key);
        if (saleByStart.isPresent()) {
            return fromSale(key, saleByStart.get(), "SALE_START", "SUCCESS");
        }

        var saleByLast = saleRepository.findByLastOperationIdempotencyKey(key);
        if (saleByLast.isPresent()) {
            Sale sale = saleByLast.get();
            String type = resolveSaleMutationType(sale);
            return fromSale(key, sale, type, "SUCCESS");
        }

        return PosOperationLookupResponse.notFound(key);
    }

    private PosOperationLookupResponse fromPayment(String key, Payment payment) {
        Sale sale = payment.getSale();
        return new PosOperationLookupResponse(
                key,
                true,
                "PAYMENT",
                payment.isConfirmed()
                        ? "COMPLETED"
                        : payment.isCancelled() || payment.isRefunded() ? "TERMINAL" : "PENDING",
                sale != null ? sale.getId() : null,
                sale != null ? sale.getSaleNumber() : null,
                sale != null ? sale.getStatus() : null,
                sale != null ? sale.getVersion() : null,
                payment.getId(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getId(),
                "Payment",
                messageForPayment(payment));
    }

    private PosOperationLookupResponse fromSale(String key, Sale sale, String type, String outcome) {
        return new PosOperationLookupResponse(
                key,
                true,
                type,
                outcome,
                sale.getId(),
                sale.getSaleNumber(),
                sale.getStatus(),
                sale.getVersion(),
                null,
                null,
                null,
                null,
                sale.getId(),
                "Sale",
                messageForSale(sale, type));
    }

    private static String resolveSaleMutationType(Sale sale) {
        if (sale.getStatus() == Sale.SaleStatus.PAID || sale.getStatus() == Sale.SaleStatus.PARTIALLY_PAID) {
            return "FINALIZE";
        }
        if (sale.getStatus() == Sale.SaleStatus.SUSPENDED) {
            return "SALE_SUSPEND";
        }
        if (sale.getStatus() == Sale.SaleStatus.CONFIRMED) {
            return "SALE_CONFIRM";
        }
        return "SALE_MUTATION";
    }

    private static String messageForPayment(Payment payment) {
        return "Pagamento encontrado: status=" + payment.getStatus().name()
                + (payment.isConfirmed()
                        ? " (não trate como aprovado apenas no cliente — use este status oficial)"
                        : "");
    }

    private static String messageForSale(Sale sale, String type) {
        return "Operação encontrada (" + type + "): venda=" + sale.getSaleNumber()
                + " status=" + sale.getStatus().name()
                + " version=" + sale.getVersion();
    }

    private void assertCanLookup() {
        if (!SecurityAuthorities.hasAuthority("POS_SALE_CREATE")
                && !SecurityAuthorities.hasAuthority("POS_PAYMENT_MANAGE")
                && !SecurityAuthorities.hasAuthority("POS_SALE_FINALIZE")
                && !SecurityAuthorities.hasAuthority("SALE_READ")) {
            throw new BusinessRuleException("Sem permissão para consultar operação por Idempotency-Key");
        }
    }
}
