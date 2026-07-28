package br.com.systemcommerce.purchase.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.purchase.entity.StorePurchaseOrderSequence;
import br.com.systemcommerce.purchase.repository.StorePurchaseOrderSequenceRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StorePurchaseOrderSequenceService {

    private final StorePurchaseOrderSequenceRepository storePurchaseOrderSequenceRepository;

    @Transactional
    public String allocateNextOrderNumber(Store store) {
        StorePurchaseOrderSequence sequence = storePurchaseOrderSequenceRepository
                .findByStoreIdForUpdate(store.getId())
                .orElseGet(() -> createInitial(store));
        long next = sequence.incrementAndGet();
        storePurchaseOrderSequenceRepository.save(sequence);
        return formatNumber(store.getCode(), sequence.getPrefix(), next);
    }

    private StorePurchaseOrderSequence createInitial(Store store) {
        StorePurchaseOrderSequence sequence = new StorePurchaseOrderSequence();
        sequence.setStore(store);
        sequence.setStoreId(store.getId());
        sequence.setLastValue(0L);
        sequence.setPrefix("C");
        sequence.setUpdatedAt(Instant.now());
        return storePurchaseOrderSequenceRepository.save(sequence);
    }

    public static String formatNumber(String storeCode, String prefix, long value) {
        String p = prefix != null && !prefix.isBlank() ? prefix : "C";
        return String.format("%s-%s-%06d", p, storeCode, value);
    }
}
