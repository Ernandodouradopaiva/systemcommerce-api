package br.com.systemcommerce.catalog.mapper;

import br.com.systemcommerce.catalog.dto.ProductLineCreateRequest;
import br.com.systemcommerce.catalog.dto.ProductLineResponse;
import br.com.systemcommerce.catalog.dto.ProductLineUpdateRequest;
import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.catalog.entity.ProductLine;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;

@Component
public class ProductLineMapper {

    public ProductLineResponse toResponse(ProductLine line) {
        Brand brand = line.getBrand();
        return new ProductLineResponse(
                line.getId(),
                line.getOrganization() != null ? line.getOrganization().getId() : null,
                brand != null ? brand.getId() : null,
                brand != null ? brand.getName() : null,
                line.getCode(),
                line.getName(),
                line.getDescription(),
                line.getStatus(),
                line.getActive(),
                line.getCreatedAt(),
                line.getUpdatedAt());
    }

    public void applyCreate(
            ProductLine line, ProductLineCreateRequest request, Organization organization, Brand brand) {
        line.setOrganization(organization);
        line.setBrand(brand);
        apply(line, request.code(), request.name(), request.description());
        line.markActive();
    }

    public void applyUpdate(ProductLine line, ProductLineUpdateRequest request, Brand brand) {
        line.setBrand(brand);
        apply(line, request.code(), request.name(), request.description());
    }

    private void apply(ProductLine line, String code, String name, String description) {
        line.setCode(MoneyAndQuantityUtils.requireText(code, "Código").toUpperCase());
        line.setName(MoneyAndQuantityUtils.requireText(name, "Nome"));
        line.setDescription(MoneyAndQuantityUtils.blankToNull(description));
    }
}
