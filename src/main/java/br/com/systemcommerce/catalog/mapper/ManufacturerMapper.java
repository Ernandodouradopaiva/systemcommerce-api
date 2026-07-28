package br.com.systemcommerce.catalog.mapper;

import br.com.systemcommerce.catalog.dto.ManufacturerCreateRequest;
import br.com.systemcommerce.catalog.dto.ManufacturerResponse;
import br.com.systemcommerce.catalog.dto.ManufacturerUpdateRequest;
import br.com.systemcommerce.catalog.entity.Manufacturer;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;

@Component
public class ManufacturerMapper {

    public ManufacturerResponse toResponse(Manufacturer manufacturer) {
        return new ManufacturerResponse(
                manufacturer.getId(),
                manufacturer.getOrganization() != null ? manufacturer.getOrganization().getId() : null,
                manufacturer.getCode(),
                manufacturer.getName(),
                manufacturer.getDescription(),
                manufacturer.getCountryCode(),
                manufacturer.getWebsite(),
                manufacturer.getLogoUrl(),
                manufacturer.getStatus(),
                manufacturer.getActive(),
                manufacturer.getCreatedAt(),
                manufacturer.getUpdatedAt());
    }

    public void applyCreate(Manufacturer manufacturer, ManufacturerCreateRequest request, Organization organization) {
        manufacturer.setOrganization(organization);
        apply(
                manufacturer,
                request.code(),
                request.name(),
                request.description(),
                request.countryCode(),
                request.website(),
                request.logoUrl());
        manufacturer.markActive();
    }

    public void applyUpdate(Manufacturer manufacturer, ManufacturerUpdateRequest request) {
        apply(
                manufacturer,
                request.code(),
                request.name(),
                request.description(),
                request.countryCode(),
                request.website(),
                request.logoUrl());
    }

    private void apply(
            Manufacturer manufacturer,
            String code,
            String name,
            String description,
            String countryCode,
            String website,
            String logoUrl) {
        manufacturer.setCode(MoneyAndQuantityUtils.requireText(code, "Código").toUpperCase());
        manufacturer.setName(MoneyAndQuantityUtils.requireText(name, "Nome"));
        manufacturer.setDescription(MoneyAndQuantityUtils.blankToNull(description));
        manufacturer.setCountryCode(MoneyAndQuantityUtils.blankToNull(countryCode));
        manufacturer.setWebsite(MoneyAndQuantityUtils.blankToNull(website));
        manufacturer.setLogoUrl(MoneyAndQuantityUtils.blankToNull(logoUrl));
    }
}
