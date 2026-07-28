package br.com.systemcommerce.carrier.mapper;

import br.com.systemcommerce.carrier.dto.CarrierContactResponse;
import br.com.systemcommerce.carrier.dto.CarrierResponse;
import br.com.systemcommerce.carrier.entity.Carrier;
import br.com.systemcommerce.carrier.entity.CarrierContact;
import org.springframework.stereotype.Component;

@Component
public class CarrierMapper {

    public CarrierResponse toResponse(Carrier carrier) {
        return new CarrierResponse(
                carrier.getId(),
                carrier.getOrganization() != null ? carrier.getOrganization().getId() : null,
                carrier.getCode(),
                carrier.getLegalName(),
                carrier.getTradeName(),
                carrier.getDocument(),
                carrier.getStateRegistration(),
                carrier.getAnttRntrc(),
                carrier.getStatus(),
                carrier.isUsable(),
                carrier.getNotes(),
                carrier.getContacts().stream().map(this::toContactResponse).toList(),
                carrier.getVersion(),
                carrier.getCreatedAt(),
                carrier.getUpdatedAt());
    }

    public CarrierContactResponse toContactResponse(CarrierContact contact) {
        return new CarrierContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getPhone(),
                contact.getEmail(),
                contact.getRoleLabel(),
                Boolean.TRUE.equals(contact.getPrimaryContact()));
    }
}
