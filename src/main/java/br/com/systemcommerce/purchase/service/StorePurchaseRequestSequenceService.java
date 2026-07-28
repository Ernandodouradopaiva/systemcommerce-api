package br.com.systemcommerce.purchase.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.purchase.entity.StorePurchaseRequestSequence;
import br.com.systemcommerce.purchase.repository.StorePurchaseRequestSequenceRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StorePurchaseRequestSequenceService {

    private final StorePurchaseRequestSequenceRepository storePurchaseRequestSequenceRepository;

    @Transactional
    public String allocateNextRequestNumber(Store store) {
        StorePurchaseRequestSequence sequence = storePurchaseRequestSequenceRepository
                .findByStoreIdForUpdate(store.getId())
                .orElseGet(() -> createInitial(store));
        long next = sequence.incrementAndGet();
        storePurchaseRequestSequenceRepository.save(sequence);
        return formatNumber(store.getCode(), sequence.getPrefix(), next);
    }

    private StorePurchaseRequestSequence createInitial(Store store) {
        StorePurchaseRequestSequence sequence = new StorePurchaseRequestSequence();
        sequence.setStore(store);
        sequence.setStoreId(store.getId());
        sequence.setLastValue(0L);
        sequence.setPrefix("SC");
        sequence.setUpdatedAt(Instant.now());
        return storePurchaseRequestSequenceRepository.save(sequence);
    }

    public static String formatNumber(String storeCode, String prefix, long value) {
        String p = prefix != null && !prefix.isBlank() ? prefix : "SC";
        return String.format("%s-%s-%06d", p, storeCode, value);
    }
}
