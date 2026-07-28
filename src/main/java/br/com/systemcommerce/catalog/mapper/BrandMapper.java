package br.com.systemcommerce.catalog.mapper;

import br.com.systemcommerce.catalog.dto.BrandCreateRequest;
import br.com.systemcommerce.catalog.dto.BrandResponse;
import br.com.systemcommerce.catalog.dto.BrandUpdateRequest;
import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getOrganization() != null ? brand.getOrganization().getId() : null,
                brand.getCode(),
                brand.getName(),
                brand.getDescription(),
                brand.getCountryCode(),
                brand.getWebsite(),
                brand.getLogoUrl(),
                brand.getStatus(),
                brand.getActive(),
                brand.getCreatedAt(),
                brand.getUpdatedAt());
    }

    public void applyCreate(Brand brand, BrandCreateRequest request, Organization organization) {
        brand.setOrganization(organization);
        apply(
                brand,
                request.code(),
                request.name(),
                request.description(),
                request.countryCode(),
                request.website(),
                request.logoUrl());
        brand.markActive();
    }

    public void applyUpdate(Brand brand, BrandUpdateRequest request) {
        apply(
                brand,
                request.code(),
                request.name(),
                request.description(),
                request.countryCode(),
                request.website(),
                request.logoUrl());
    }

    private void apply(
            Brand brand,
            String code,
            String name,
            String description,
            String countryCode,
            String website,
            String logoUrl) {
        brand.setCode(MoneyAndQuantityUtils.requireText(code, "Código").toUpperCase());
        brand.setName(MoneyAndQuantityUtils.requireText(name, "Nome"));
        brand.setDescription(MoneyAndQuantityUtils.blankToNull(description));
        brand.setCountryCode(MoneyAndQuantityUtils.blankToNull(countryCode));
        brand.setWebsite(MoneyAndQuantityUtils.blankToNull(website));
        brand.setLogoUrl(MoneyAndQuantityUtils.blankToNull(logoUrl));
    }
}
