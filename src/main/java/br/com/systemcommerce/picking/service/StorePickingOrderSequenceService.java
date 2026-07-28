package br.com.systemcommerce.picking.service;

import br.com.systemcommerce.picking.entity.StorePickingOrderSequence;
import br.com.systemcommerce.picking.repository.StorePickingOrderSequenceRepository;
import br.com.systemcommerce.pos.store.entity.Store;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StorePickingOrderSequenceService {

    private final StorePickingOrderSequenceRepository repository;

    @Transactional
    public String allocateNextPickingNumber(Store store) {
        StorePickingOrderSequence sequence =
                repository.findByStoreIdForUpdate(store.getId()).orElseGet(() -> createInitial(store));
        long next = sequence.incrementAndGet();
        repository.save(sequence);
        return String.format("%s-%s-%06d", sequence.getPrefix(), store.getCode(), next);
    }

    private StorePickingOrderSequence createInitial(Store store) {
        StorePickingOrderSequence sequence = new StorePickingOrderSequence();
        sequence.setStore(store);
        sequence.setStoreId(store.getId());
        sequence.setLastValue(0L);
        sequence.setPrefix("SP");
        sequence.setUpdatedAt(Instant.now());
        return repository.save(sequence);
    }
}
