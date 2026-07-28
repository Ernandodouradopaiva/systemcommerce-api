package br.com.systemcommerce.customer.mapper;

import br.com.systemcommerce.customer.dto.CustomerContactRequest;
import br.com.systemcommerce.customer.dto.CustomerContactResponse;
import br.com.systemcommerce.customer.entity.CustomerContact;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CustomerContactMapper {

    public CustomerContactResponse toResponse(CustomerContact contact) {
        return new CustomerContactResponse(
                contact.getId(),
                contact.getCustomer().getId(),
                contact.getType(),
                contact.getName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getMobile(),
                contact.getRoleDescription(),
                contact.getIsDefault(),
                contact.getNotes(),
                contact.getActive(),
                contact.getCreatedAt(),
                contact.getUpdatedAt());
    }

    public void apply(CustomerContact contact, CustomerContactRequest request) {
        contact.setType(request.type());
        contact.setName(blankToNull(request.name()));
        String email = blankToNull(request.email());
        contact.setEmail(email == null ? null : email.toLowerCase());
        contact.setPhone(blankToNull(request.phone()));
        contact.setMobile(blankToNull(request.mobile()));
        contact.setRoleDescription(blankToNull(request.roleDescription()));
        contact.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        contact.setNotes(blankToNull(request.notes()));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
