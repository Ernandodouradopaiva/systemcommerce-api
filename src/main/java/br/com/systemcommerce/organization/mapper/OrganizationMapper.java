package br.com.systemcommerce.organization.mapper;

import br.com.systemcommerce.organization.dto.OrganizationCreateRequest;
import br.com.systemcommerce.organization.dto.OrganizationResponse;
import br.com.systemcommerce.organization.dto.OrganizationUpdateRequest;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrganizationMapper {

    public OrganizationResponse toResponse(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getCode(),
                org.getLegalName(),
                org.getTradeName(),
                org.getDocument(),
                org.getStateRegistration(),
                org.getMunicipalRegistration(),
                org.getEmail(),
                org.getPhone(),
                org.getWebsite(),
                org.getZipCode(),
                org.getStreet(),
                org.getNumber(),
                org.getComplement(),
                org.getDistrict(),
                org.getCity(),
                org.getState(),
                org.getDefaultTimezone(),
                org.getCurrency(),
                org.getStatus(),
                org.getActive(),
                org.getCreatedAt(),
                org.getUpdatedAt());
    }

    public void applyCreate(Organization org, OrganizationCreateRequest request) {
        applyFields(
                org,
                request.code(),
                request.legalName(),
                request.tradeName(),
                request.document(),
                request.stateRegistration(),
                request.municipalRegistration(),
                request.email(),
                request.phone(),
                request.website(),
                request.zipCode(),
                request.street(),
                request.number(),
                request.complement(),
                request.district(),
                request.city(),
                request.state(),
                request.defaultTimezone(),
                request.currency());
        org.markActive();
    }

    public void applyUpdate(Organization org, OrganizationUpdateRequest request) {
        applyFields(
                org,
                request.code(),
                request.legalName(),
                request.tradeName(),
                request.document(),
                request.stateRegistration(),
                request.municipalRegistration(),
                request.email(),
                request.phone(),
                request.website(),
                request.zipCode(),
                request.street(),
                request.number(),
                request.complement(),
                request.district(),
                request.city(),
                request.state(),
                request.defaultTimezone(),
                request.currency());
    }

    private void applyFields(
            Organization org,
            String code,
            String legalName,
            String tradeName,
            String document,
            String stateRegistration,
            String municipalRegistration,
            String email,
            String phone,
            String website,
            String zipCode,
            String street,
            String number,
            String complement,
            String district,
            String city,
            String state,
            String defaultTimezone,
            String currency) {
        org.setCode(MoneyAndQuantityUtils.requireText(code, "Código").toUpperCase());
        org.setLegalName(MoneyAndQuantityUtils.requireText(legalName, "Razão social"));
        org.setTradeName(MoneyAndQuantityUtils.blankToNull(tradeName));
        org.setDocument(normalizeDocument(document));
        org.setStateRegistration(MoneyAndQuantityUtils.blankToNull(stateRegistration));
        org.setMunicipalRegistration(MoneyAndQuantityUtils.blankToNull(municipalRegistration));
        org.setEmail(MoneyAndQuantityUtils.blankToNull(email));
        org.setPhone(MoneyAndQuantityUtils.blankToNull(phone));
        org.setWebsite(MoneyAndQuantityUtils.blankToNull(website));
        org.setZipCode(MoneyAndQuantityUtils.blankToNull(zipCode));
        org.setStreet(MoneyAndQuantityUtils.blankToNull(street));
        org.setNumber(MoneyAndQuantityUtils.blankToNull(number));
        org.setComplement(MoneyAndQuantityUtils.blankToNull(complement));
        org.setDistrict(MoneyAndQuantityUtils.blankToNull(district));
        org.setCity(MoneyAndQuantityUtils.blankToNull(city));
        org.setState(StringUtils.hasText(state) ? state.trim().toUpperCase() : null);
        org.setDefaultTimezone(
                StringUtils.hasText(defaultTimezone) ? defaultTimezone.trim() : "America/Sao_Paulo");
        org.setCurrency(
                StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "BRL");
    }

    private static String normalizeDocument(String document) {
        String value = MoneyAndQuantityUtils.blankToNull(document);
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
