package br.com.systemcommerce.serial.mapper;

import br.com.systemcommerce.serial.dto.ProductSerialNumberResponse;
import br.com.systemcommerce.serial.dto.SerialNumberStatusHistoryResponse;
import br.com.systemcommerce.serial.entity.ProductSerialNumber;
import br.com.systemcommerce.serial.entity.SerialNumberStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class ProductSerialNumberMapper {

    public ProductSerialNumberResponse toResponse(ProductSerialNumber serial) {
        return new ProductSerialNumberResponse(
                serial.getId(),
                serial.getOrganization().getId(),
                serial.getProduct().getId(),
                serial.getProduct().getSku(),
                serial.getSerialNumber(),
                serial.getProductBatch() != null ? serial.getProductBatch().getId() : null,
                serial.getWarehouse() != null ? serial.getWarehouse().getId() : null,
                serial.getStatus(),
                serial.getNotes(),
                serial.getActive());
    }

    public SerialNumberStatusHistoryResponse toHistoryResponse(SerialNumberStatusHistory history) {
        return new SerialNumberStatusHistoryResponse(
                history.getFromStatus(), history.getToStatus(), history.getNotes(), history.getChangedAt());
    }
}
