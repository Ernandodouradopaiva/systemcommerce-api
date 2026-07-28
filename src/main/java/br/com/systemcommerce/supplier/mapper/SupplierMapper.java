package br.com.systemcommerce.supplier.mapper;

import br.com.systemcommerce.customer.validation.BrazilianDocumentUtils;
import br.com.systemcommerce.supplier.dto.SupplierCreateRequest;
import br.com.systemcommerce.supplier.dto.SupplierResponse;
import br.com.systemcommerce.supplier.dto.SupplierUpdateRequest;
import br.com.systemcommerce.supplier.entity.Supplier;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierMapper {

    public SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getCode(),
                supplier.getType(),
                supplier.getDocument(),
                supplier.getStateRegistration(),
                supplier.getLegalName(),
                supplier.getTradeName(),
                supplier.getContactName(),
                supplier.getPhone(),
                supplier.getMobile(),
                supplier.getEmail(),
                supplier.getWebsite(),
                supplier.getZipCode(),
                supplier.getStreet(),
                supplier.getNumber(),
                supplier.getComplement(),
                supplier.getDistrict(),
                supplier.getCity(),
                supplier.getState(),
                supplier.getNotes(),
                supplier.getMunicipalRegistration(),
                supplier.getTaxContributorIndicator(),
                supplier.getCategory(),
                supplier.getStatus(),
                supplier.getBlockedAt(),
                supplier.getBlockedReason(),
                supplier.getActive(),
                supplier.getRegisteredAt(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt());
    }

    public void applyCreate(Supplier supplier, SupplierCreateRequest request, String normalizedDocument) {
        supplier.setCode(trim(request.code()));
        supplier.setType(request.type());
        supplier.setDocument(normalizedDocument);
        supplier.setStateRegistration(blankToNull(request.stateRegistration()));
        supplier.setLegalName(trim(request.legalName()));
        supplier.setTradeName(blankToNull(request.tradeName()));
        supplier.setContactName(blankToNull(request.contactName()));
        supplier.setPhone(blankToNull(request.phone()));
        supplier.setMobile(blankToNull(request.mobile()));
        supplier.setEmail(normalizeEmail(request.email()));
        supplier.setWebsite(blankToNull(request.website()));
        supplier.setNotes(blankToNull(request.notes()));
        supplier.setMunicipalRegistration(blankToNull(request.municipalRegistration()));
        supplier.setTaxContributorIndicator(request.taxContributorIndicator());
        supplier.setCategory(blankToNull(request.category()));
        applyAddress(
                supplier,
                request.zipCode(),
                request.street(),
                request.number(),
                request.complement(),
                request.district(),
                request.city(),
                request.state());
        supplier.setRegisteredAt(Instant.now());
        supplier.markActive();
    }

    public void applyUpdate(Supplier supplier, SupplierUpdateRequest request, String normalizedDocument) {
        supplier.setCode(trim(request.code()));
        supplier.setType(request.type());
        supplier.setDocument(normalizedDocument);
        supplier.setStateRegistration(blankToNull(request.stateRegistration()));
        supplier.setLegalName(trim(request.legalName()));
        supplier.setTradeName(blankToNull(request.tradeName()));
        supplier.setContactName(blankToNull(request.contactName()));
        supplier.setPhone(blankToNull(request.phone()));
        supplier.setMobile(blankToNull(request.mobile()));
        supplier.setEmail(normalizeEmail(request.email()));
        supplier.setWebsite(blankToNull(request.website()));
        supplier.setNotes(blankToNull(request.notes()));
        supplier.setMunicipalRegistration(blankToNull(request.municipalRegistration()));
        supplier.setTaxContributorIndicator(request.taxContributorIndicator());
        supplier.setCategory(blankToNull(request.category()));
        applyAddress(
                supplier,
                request.zipCode(),
                request.street(),
                request.number(),
                request.complement(),
                request.district(),
                request.city(),
                request.state());
    }

    private void applyAddress(
            Supplier supplier,
            String zipCode,
            String street,
            String number,
            String complement,
            String district,
            String city,
            String state) {
        supplier.setZipCode(BrazilianDocumentUtils.digitsOnly(blankToNull(zipCode)));
        supplier.setStreet(blankToNull(street));
        supplier.setNumber(blankToNull(number));
        supplier.setComplement(blankToNull(complement));
        supplier.setDistrict(blankToNull(district));
        supplier.setCity(blankToNull(city));
        String uf = blankToNull(state);
        supplier.setState(uf == null ? null : uf.toUpperCase());
    }

    private String normalizeEmail(String email) {
        String value = blankToNull(email);
        return value == null ? null : value.toLowerCase();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
