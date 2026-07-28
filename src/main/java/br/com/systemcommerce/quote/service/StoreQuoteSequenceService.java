package br.com.systemcommerce.quote.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.quote.entity.StoreQuoteSequence;
import br.com.systemcommerce.quote.repository.StoreQuoteSequenceRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreQuoteSequenceService {

    private final StoreQuoteSequenceRepository storeQuoteSequenceRepository;

    @Transactional
    public String allocateNextQuoteNumber(Store store) {
        StoreQuoteSequence sequence = storeQuoteSequenceRepository
                .findByStoreIdForUpdate(store.getId())
                .orElseGet(() -> createInitial(store));
        long next = sequence.incrementAndGet();
        storeQuoteSequenceRepository.save(sequence);
        return formatNumber(store.getCode(), sequence.getPrefix(), next);
    }

    private StoreQuoteSequence createInitial(Store store) {
        StoreQuoteSequence sequence = new StoreQuoteSequence();
        sequence.setStore(store);
        sequence.setStoreId(store.getId());
        sequence.setLastValue(0L);
        sequence.setPrefix("O");
        sequence.setUpdatedAt(Instant.now());
        return storeQuoteSequenceRepository.save(sequence);
    }

    public static String formatNumber(String storeCode, String prefix, long value) {
        String p = prefix != null && !prefix.isBlank() ? prefix : "O";
        return String.format("%s-%s-%06d", p, storeCode, value);
    }
}
