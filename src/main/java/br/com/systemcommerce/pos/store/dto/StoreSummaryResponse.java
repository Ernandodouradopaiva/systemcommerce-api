package br.com.systemcommerce.pos.store.dto;

import br.com.systemcommerce.pos.store.entity.Store;
import java.util.UUID;

public record StoreSummaryResponse(
        UUID id,
        String code,
        String name,
        String tradeName,
        Store.StoreStatus status,
        boolean headquarters,
        boolean allowsSales,
        boolean allowsPos,
        long warehouseCount,
        long terminalCount,
        long openCashSessionCount) {}
