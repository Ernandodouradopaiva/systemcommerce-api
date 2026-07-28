package br.com.systemcommerce.batch.mapper;

import br.com.systemcommerce.batch.dto.ProductBatchResponse;
import br.com.systemcommerce.batch.entity.ProductBatch;
import org.springframework.stereotype.Component;

@Component
public class ProductBatchMapper {

    public ProductBatchResponse toResponse(ProductBatch batch) {
        return new ProductBatchResponse(
                batch.getId(),
                batch.getOrganization().getId(),
                batch.getProduct().getId(),
                batch.getProduct().getSku(),
                batch.getProduct().getName(),
                batch.getBatchCode(),
                batch.getManufacturedAt(),
                batch.getExpiresAt(),
                batch.getReceivedAt(),
                batch.getStatus(),
                batch.getNotes(),
                batch.getActive());
    }
}
