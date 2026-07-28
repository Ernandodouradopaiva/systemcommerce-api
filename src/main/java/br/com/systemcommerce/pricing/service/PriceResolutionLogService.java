package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.pricing.dto.ApplicablePriceResponse;
import br.com.systemcommerce.pricing.entity.PriceResolutionLog;
import br.com.systemcommerce.pricing.repository.PriceResolutionLogRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra cada resolução de preço em transação própria ({@code REQUIRES_NEW}) — não deve nunca impedir a
 * resolução em si (best-effort; falhas de log são apenas registradas em log de aplicação).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceResolutionLogService {

    private final PriceResolutionLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            UUID organizationId,
            UUID storeId,
            UUID productId,
            String channel,
            UUID customerId,
            BigDecimal quantity,
            ApplicablePriceResponse resolved) {
        try {
            PriceResolutionLog entry = new PriceResolutionLog();
            entry.setOrganizationId(organizationId);
            entry.setStoreId(storeId);
            entry.setProductId(productId);
            entry.setChannel(channel);
            entry.setCustomerId(customerId);
            entry.setQuantity(quantity);
            entry.setResolvedPrice(resolved.unitPrice());
            entry.setPriceOrigin(resolved.priceSource() != null ? resolved.priceSource().name() : "UNKNOWN");
            entry.setPriceTableId(resolved.priceTableId());
            entry.setProductPriceId(resolved.productPriceId());
            repository.save(entry);
        } catch (Exception ex) {
            log.warn("Falha ao registrar PriceResolutionLog (best-effort): {}", ex.getMessage());
        }
    }
}
