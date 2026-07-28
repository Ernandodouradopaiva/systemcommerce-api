package br.com.systemcommerce.shipment.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shipment.entity.StoreShipmentSequence;
import br.com.systemcommerce.shipment.repository.StoreShipmentSequenceRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreShipmentSequenceService {

    private final StoreShipmentSequenceRepository repository;

    @Transactional
    public String allocateNextShipmentNumber(Store store) {
        StoreShipmentSequence sequence =
                repository.findByStoreIdForUpdate(store.getId()).orElseGet(() -> createInitial(store));
        long next = sequence.incrementAndGet();
        repository.save(sequence);
        return String.format("%s-%s-%06d", sequence.getPrefix(), store.getCode(), next);
    }

    private StoreShipmentSequence createInitial(Store store) {
        StoreShipmentSequence sequence = new StoreShipmentSequence();
        sequence.setStore(store);
        sequence.setStoreId(store.getId());
        sequence.setLastValue(0L);
        sequence.setPrefix("XP");
        sequence.setUpdatedAt(Instant.now());
        return repository.save(sequence);
    }
}
