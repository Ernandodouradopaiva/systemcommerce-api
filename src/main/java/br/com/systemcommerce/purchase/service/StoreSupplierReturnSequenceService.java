package br.com.systemcommerce.purchase.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.purchase.entity.StoreSupplierReturnSequence;
import br.com.systemcommerce.purchase.repository.StoreSupplierReturnSequenceRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreSupplierReturnSequenceService {

    private final StoreSupplierReturnSequenceRepository storeSupplierReturnSequenceRepository;

    @Transactional
    public String allocateNextReturnNumber(Store store) {
        StoreSupplierReturnSequence sequence = storeSupplierReturnSequenceRepository
                .findByStoreIdForUpdate(store.getId())
                .orElseGet(() -> createInitial(store));
        long next = sequence.incrementAndGet();
        storeSupplierReturnSequenceRepository.save(sequence);
        return formatNumber(store.getCode(), sequence.getPrefix(), next);
    }

    private StoreSupplierReturnSequence createInitial(Store store) {
        StoreSupplierReturnSequence sequence = new StoreSupplierReturnSequence();
        sequence.setStore(store);
        sequence.setStoreId(store.getId());
        sequence.setLastValue(0L);
        sequence.setPrefix("DF");
        sequence.setUpdatedAt(Instant.now());
        return storeSupplierReturnSequenceRepository.save(sequence);
    }

    public static String formatNumber(String storeCode, String prefix, long value) {
        String p = prefix != null && !prefix.isBlank() ? prefix : "DF";
        return String.format("%s-%s-%06d", p, storeCode, value);
    }
}
