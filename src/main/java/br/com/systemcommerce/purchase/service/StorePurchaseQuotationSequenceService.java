package br.com.systemcommerce.purchase.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.purchase.entity.StorePurchaseQuotationSequence;
import br.com.systemcommerce.purchase.repository.StorePurchaseQuotationSequenceRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StorePurchaseQuotationSequenceService {

    private final StorePurchaseQuotationSequenceRepository storePurchaseQuotationSequenceRepository;

    @Transactional
    public String allocateNextQuotationNumber(Store store) {
        StorePurchaseQuotationSequence sequence = storePurchaseQuotationSequenceRepository
                .findByStoreIdForUpdate(store.getId())
                .orElseGet(() -> createInitial(store));
        long next = sequence.incrementAndGet();
        storePurchaseQuotationSequenceRepository.save(sequence);
        return formatNumber(store.getCode(), sequence.getPrefix(), next);
    }

    private StorePurchaseQuotationSequence createInitial(Store store) {
        StorePurchaseQuotationSequence sequence = new StorePurchaseQuotationSequence();
        sequence.setStore(store);
        sequence.setStoreId(store.getId());
        sequence.setLastValue(0L);
        sequence.setPrefix("CC");
        sequence.setUpdatedAt(Instant.now());
        return storePurchaseQuotationSequenceRepository.save(sequence);
    }

    public static String formatNumber(String storeCode, String prefix, long value) {
        String p = prefix != null && !prefix.isBlank() ? prefix : "CC";
        return String.format("%s-%s-%06d", p, storeCode, value);
    }
}
