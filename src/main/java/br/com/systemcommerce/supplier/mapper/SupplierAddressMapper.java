package br.com.systemcommerce.supplier.mapper;

import br.com.systemcommerce.supplier.dto.SupplierAddressRequest;
import br.com.systemcommerce.supplier.dto.SupplierAddressResponse;
import br.com.systemcommerce.supplier.entity.SupplierAddress;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierAddressMapper {

    public SupplierAddressResponse toResponse(SupplierAddress entity) {
        return new SupplierAddressResponse(
                entity.getId(),
                entity.getSupplier().getId(),
                entity.getType(),
                entity.getZipCode(),
                entity.getStreet(),
                entity.getNumber(),
                entity.getComplement(),
                entity.getDistrict(),
                entity.getCity(),
                entity.getState(),
                entity.getPrimary(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public void apply(SupplierAddress entity, SupplierAddressRequest request) {
        entity.setType(request.type());
        entity.setZipCode(blankToNull(request.zipCode()));
        entity.setStreet(blankToNull(request.street()));
        entity.setNumber(blankToNull(request.number()));
        entity.setComplement(blankToNull(request.complement()));
        entity.setDistrict(blankToNull(request.district()));
        entity.setCity(blankToNull(request.city()));
        String uf = blankToNull(request.state());
        entity.setState(uf == null ? null : uf.toUpperCase());
        entity.setPrimary(Boolean.TRUE.equals(request.primary()));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
