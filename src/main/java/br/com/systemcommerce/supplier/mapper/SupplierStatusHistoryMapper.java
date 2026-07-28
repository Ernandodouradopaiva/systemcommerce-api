package br.com.systemcommerce.supplier.mapper;

import br.com.systemcommerce.supplier.dto.SupplierStatusHistoryResponse;
import br.com.systemcommerce.supplier.entity.SupplierStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class SupplierStatusHistoryMapper {

    public SupplierStatusHistoryResponse toResponse(SupplierStatusHistory entity) {
        return new SupplierStatusHistoryResponse(
                entity.getId(),
                entity.getSupplier().getId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getNotes(),
                entity.getChangedAt(),
                entity.getChangedBy() != null ? entity.getChangedBy().getId() : null,
                entity.getChangedBy() != null ? entity.getChangedBy().getName() : null);
    }
}
