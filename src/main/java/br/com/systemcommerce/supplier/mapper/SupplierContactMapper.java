package br.com.systemcommerce.supplier.mapper;

import br.com.systemcommerce.supplier.dto.SupplierContactRequest;
import br.com.systemcommerce.supplier.dto.SupplierContactResponse;
import br.com.systemcommerce.supplier.entity.SupplierContact;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierContactMapper {

    public SupplierContactResponse toResponse(SupplierContact entity) {
        return new SupplierContactResponse(
                entity.getId(),
                entity.getSupplier().getId(),
                entity.getType(),
                entity.getName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getRole(),
                entity.getPrimary(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public void apply(SupplierContact entity, SupplierContactRequest request) {
        entity.setType(request.type());
        entity.setName(request.name().trim());
        entity.setPhone(blankToNull(request.phone()));
        entity.setEmail(blankToNull(request.email()));
        entity.setRole(blankToNull(request.role()));
        entity.setPrimary(Boolean.TRUE.equals(request.primary()));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
