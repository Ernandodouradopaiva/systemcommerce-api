package br.com.systemcommerce.customer.mapper;

import br.com.systemcommerce.customer.dto.CustomerAddressRequest;
import br.com.systemcommerce.customer.dto.CustomerAddressResponse;
import br.com.systemcommerce.customer.entity.CustomerAddress;
import br.com.systemcommerce.customer.validation.BrazilianDocumentUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CustomerAddressMapper {

    public CustomerAddressResponse toResponse(CustomerAddress address) {
        return new CustomerAddressResponse(
                address.getId(),
                address.getCustomer().getId(),
                address.getType(),
                address.getZipCode(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getDistrict(),
                address.getCity(),
                address.getState(),
                address.getIsDefault(),
                address.getNotes(),
                address.getActive(),
                address.getCreatedAt(),
                address.getUpdatedAt());
    }

    public void apply(CustomerAddress address, CustomerAddressRequest request) {
        address.setType(request.type());
        address.setZipCode(BrazilianDocumentUtils.digitsOnly(blankToNull(request.zipCode())));
        address.setStreet(blankToNull(request.street()));
        address.setNumber(blankToNull(request.number()));
        address.setComplement(blankToNull(request.complement()));
        address.setDistrict(blankToNull(request.district()));
        address.setCity(blankToNull(request.city()));
        String uf = blankToNull(request.state());
        address.setState(uf == null ? null : uf.toUpperCase());
        address.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        address.setNotes(blankToNull(request.notes()));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
