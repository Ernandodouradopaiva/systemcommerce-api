package br.com.systemcommerce.sale.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.sale.dto.StoreSaleSequenceResponse;
import br.com.systemcommerce.sale.entity.StoreSaleSequence;
import br.com.systemcommerce.sale.repository.StoreSaleSequenceRepository;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreSaleSequenceService {

    private final StoreSaleSequenceRepository storeSaleSequenceRepository;

    @Transactional
    public String allocateNextSaleNumber(Store store) {
        storeSaleSequenceRepository.insertInitialIfAbsent(store.getId());
        StoreSaleSequence sequence = storeSaleSequenceRepository
                .findByStoreIdForUpdate(store.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Sequência de vendas não encontrada para a loja " + store.getId()));
        long next = sequence.incrementAndGet();
        storeSaleSequenceRepository.save(sequence);
        return formatSaleNumber(store, sequence.getPrefix(), next);
    }

    @Transactional(readOnly = true)
    public StoreSaleSequenceResponse getSequence(UUID storeId, String storeCode) {
        StoreSaleSequence sequence = storeSaleSequenceRepository
                .findByStoreId(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sequência de vendas da loja", storeId));
        String prefix = sequence.getPrefix() != null ? sequence.getPrefix() : "V";
        long last = sequence.getLastValue() != null ? sequence.getLastValue() : 0L;
        return new StoreSaleSequenceResponse(
                storeId,
                storeCode,
                prefix,
                last,
                formatSaleNumber(storeCode, prefix, last + 1));
    }

    public static String formatSaleNumber(Store store, String prefix, long value) {
        return formatSaleNumber(store.getCode(), prefix, value);
    }

    public static String formatSaleNumber(String storeCode, String prefix, long value) {
        String p = prefix != null && !prefix.isBlank() ? prefix : "V";
        return String.format("%s-%s-%06d", p, storeCode, value);
    }
}
