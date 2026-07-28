package br.com.systemcommerce.salesorder.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.salesorder.entity.StoreSalesOrderSequence;
import br.com.systemcommerce.salesorder.repository.StoreSalesOrderSequenceRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreSalesOrderSequenceService {

    private final StoreSalesOrderSequenceRepository storeSalesOrderSequenceRepository;

    @Transactional
    public String allocateNextOrderNumber(Store store) {
        StoreSalesOrderSequence sequence = storeSalesOrderSequenceRepository
                .findByStoreIdForUpdate(store.getId())
                .orElseGet(() -> createInitial(store));
        long next = sequence.incrementAndGet();
        storeSalesOrderSequenceRepository.save(sequence);
        return formatNumber(store.getCode(), sequence.getPrefix(), next);
    }

    private StoreSalesOrderSequence createInitial(Store store) {
        StoreSalesOrderSequence sequence = new StoreSalesOrderSequence();
        sequence.setStore(store);
        sequence.setStoreId(store.getId());
        sequence.setLastValue(0L);
        sequence.setPrefix("P");
        sequence.setUpdatedAt(Instant.now());
        return storeSalesOrderSequenceRepository.save(sequence);
    }

    public static String formatNumber(String storeCode, String prefix, long value) {
        String p = prefix != null && !prefix.isBlank() ? prefix : "P";
        return String.format("%s-%s-%06d", p, storeCode, value);
    }
}
