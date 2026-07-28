package br.com.systemcommerce.supplier.mapper;

import br.com.systemcommerce.supplier.dto.SupplierDocumentRequest;
import br.com.systemcommerce.supplier.dto.SupplierDocumentResponse;
import br.com.systemcommerce.supplier.entity.SupplierDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierDocumentMapper {

    public SupplierDocumentResponse toResponse(SupplierDocument entity) {
        return new SupplierDocumentResponse(
                entity.getId(),
                entity.getSupplier().getId(),
                entity.getName(),
                entity.getType(),
                entity.getFileRef(),
                entity.getUploadedAt(),
                entity.getActive());
    }

    public void apply(SupplierDocument entity, SupplierDocumentRequest request) {
        entity.setName(request.name().trim());
        entity.setType(blankToNull(request.type()));
        entity.setFileRef(blankToNull(request.fileRef()));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
