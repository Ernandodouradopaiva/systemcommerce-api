package br.com.systemcommerce.customer.mapper;

import br.com.systemcommerce.customer.dto.CustomerCreateRequest;
import br.com.systemcommerce.customer.dto.CustomerResponse;
import br.com.systemcommerce.customer.dto.CustomerUpdateRequest;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.validation.BrazilianDocumentUtils;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getType(),
                customer.getName(),
                customer.getTradeName(),
                customer.getDocument(),
                customer.getStateRegistration(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getMobile(),
                customer.getBirthDate(),
                customer.getNotes(),
                customer.getStatus(),
                customer.getActive(),
                customer.getZipCode(),
                customer.getStreet(),
                customer.getNumber(),
                customer.getComplement(),
                customer.getDistrict(),
                customer.getCity(),
                customer.getState(),
                customer.getClassification(),
                customer.getRegistrationOrigin(),
                customer.getCommercialNotes(),
                customer.getCreditLimit(),
                customer.getDelinquencyIndicator(),
                customer.getBlockedAt(),
                customer.getBlockedReason(),
                customer.getAllowQuoteWhenBlocked(),
                customer.getMunicipalRegistration(),
                customer.isUsableForSale(),
                customer.isUsableForQuote(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }

    public void applyCreate(Customer customer, CustomerCreateRequest request, String normalizedDocument) {
        customer.setType(request.type());
        customer.setName(trim(request.name()));
        customer.setTradeName(blankToNull(request.tradeName()));
        customer.setDocument(normalizedDocument);
        customer.setStateRegistration(blankToNull(request.stateRegistration()));
        customer.setEmail(normalizeEmail(request.email()));
        customer.setPhone(blankToNull(request.phone()));
        customer.setMobile(blankToNull(request.mobile()));
        customer.setBirthDate(request.birthDate());
        customer.setNotes(blankToNull(request.notes()));
        applyAddress(customer, request.zipCode(), request.street(), request.number(), request.complement(), request.district(), request.city(), request.state());
        applyCommercial(
                customer,
                request.classification(),
                request.registrationOrigin(),
                request.commercialNotes(),
                request.municipalRegistration(),
                request.allowQuoteWhenBlocked());
        customer.markActive();
    }

    public void applyUpdate(Customer customer, CustomerUpdateRequest request, String normalizedDocument) {
        customer.setType(request.type());
        customer.setName(trim(request.name()));
        customer.setTradeName(blankToNull(request.tradeName()));
        customer.setDocument(normalizedDocument);
        customer.setStateRegistration(blankToNull(request.stateRegistration()));
        customer.setEmail(normalizeEmail(request.email()));
        customer.setPhone(blankToNull(request.phone()));
        customer.setMobile(blankToNull(request.mobile()));
        customer.setBirthDate(request.birthDate());
        customer.setNotes(blankToNull(request.notes()));
        applyAddress(customer, request.zipCode(), request.street(), request.number(), request.complement(), request.district(), request.city(), request.state());
        applyCommercial(
                customer,
                request.classification(),
                request.registrationOrigin(),
                request.commercialNotes(),
                request.municipalRegistration(),
                request.allowQuoteWhenBlocked());
    }

    private void applyCommercial(
            Customer customer,
            Customer.CustomerClassification classification,
            Customer.RegistrationOrigin registrationOrigin,
            String commercialNotes,
            String municipalRegistration,
            Boolean allowQuoteWhenBlocked) {
        customer.setClassification(classification);
        customer.setRegistrationOrigin(registrationOrigin != null ? registrationOrigin : Customer.RegistrationOrigin.ERP);
        customer.setCommercialNotes(blankToNull(commercialNotes));
        customer.setMunicipalRegistration(blankToNull(municipalRegistration));
        customer.setAllowQuoteWhenBlocked(allowQuoteWhenBlocked != null ? allowQuoteWhenBlocked : Boolean.TRUE);
        if (customer.getCreditLimit() == null) {
            customer.setCreditLimit(BigDecimal.ZERO);
        }
        if (customer.getDelinquencyIndicator() == null) {
            customer.setDelinquencyIndicator(Boolean.FALSE);
        }
    }

    private void applyAddress(
            Customer customer,
            String zipCode,
            String street,
            String number,
            String complement,
            String district,
            String city,
            String state) {
        customer.setZipCode(BrazilianDocumentUtils.digitsOnly(blankToNull(zipCode)));
        customer.setStreet(blankToNull(street));
        customer.setNumber(blankToNull(number));
        customer.setComplement(blankToNull(complement));
        customer.setDistrict(blankToNull(district));
        customer.setCity(blankToNull(city));
        String uf = blankToNull(state);
        customer.setState(uf == null ? null : uf.toUpperCase());
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
