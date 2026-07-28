package br.com.systemcommerce.pos.store.mapper;

import br.com.systemcommerce.pos.store.dto.StoreCreateRequest;
import br.com.systemcommerce.pos.store.dto.StoreResponse;
import br.com.systemcommerce.pos.store.dto.StoreUpdateRequest;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StoreMapper {

    public StoreResponse toResponse(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getOrganization().getId(),
                store.getOrganization().getCode(),
                store.getCode(),
                store.getName(),
                store.getTradeName(),
                store.getDocument(),
                store.getStateRegistration(),
                store.getMunicipalRegistration(),
                store.getEstablishmentType(),
                store.isHeadquarters(),
                store.getOpeningDate(),
                store.isAllowsSales(),
                store.isAllowsPos(),
                store.getEmail(),
                store.getPhone(),
                store.getZipCode(),
                store.getStreet(),
                store.getNumber(),
                store.getComplement(),
                store.getDistrict(),
                store.getCity(),
                store.getState(),
                store.getTimezone(),
                store.getStatus(),
                store.getActive(),
                store.getCreatedAt(),
                store.getUpdatedAt());
    }

    public void applyCreate(Store store, StoreCreateRequest request) {
        applyFields(
                store,
                request.code(),
                request.name(),
                request.tradeName(),
                request.document(),
                request.stateRegistration(),
                request.municipalRegistration(),
                request.establishmentType(),
                request.headquarters(),
                request.openingDate(),
                request.allowsSales(),
                request.allowsPos(),
                request.email(),
                request.phone(),
                request.zipCode(),
                request.street(),
                request.number(),
                request.complement(),
                request.district(),
                request.city(),
                request.state(),
                request.timezone());
        store.markActive();
    }

    public void applyUpdate(Store store, StoreUpdateRequest request) {
        applyFields(
                store,
                request.code(),
                request.name(),
                request.tradeName(),
                request.document(),
                request.stateRegistration(),
                request.municipalRegistration(),
                request.establishmentType(),
                request.headquarters(),
                request.openingDate(),
                request.allowsSales(),
                request.allowsPos(),
                request.email(),
                request.phone(),
                request.zipCode(),
                request.street(),
                request.number(),
                request.complement(),
                request.district(),
                request.city(),
                request.state(),
                request.timezone());
    }

    private void applyFields(
            Store store,
            String code,
            String name,
            String tradeName,
            String document,
            String stateRegistration,
            String municipalRegistration,
            Store.EstablishmentType establishmentType,
            Boolean headquarters,
            java.time.LocalDate openingDate,
            Boolean allowsSales,
            Boolean allowsPos,
            String email,
            String phone,
            String zipCode,
            String street,
            String number,
            String complement,
            String district,
            String city,
            String state,
            String timezone) {
        store.setCode(MoneyAndQuantityUtils.requireText(code, "Código").toUpperCase());
        store.setName(MoneyAndQuantityUtils.requireText(name, "Razão social"));
        store.setTradeName(MoneyAndQuantityUtils.blankToNull(tradeName));
        store.setDocument(normalizeDocument(document));
        store.setStateRegistration(MoneyAndQuantityUtils.blankToNull(stateRegistration));
        store.setMunicipalRegistration(MoneyAndQuantityUtils.blankToNull(municipalRegistration));
        if (establishmentType != null) {
            store.setEstablishmentType(establishmentType);
        } else if (store.getEstablishmentType() == null) {
            store.setEstablishmentType(Store.EstablishmentType.BRANCH);
        }
        if (headquarters != null) {
            store.setHeadquarters(headquarters);
        }
        store.setOpeningDate(openingDate);
        if (allowsSales != null) {
            store.setAllowsSales(allowsSales);
        } else if (store.getId() == null) {
            store.setAllowsSales(true);
        }
        if (allowsPos != null) {
            store.setAllowsPos(allowsPos);
        } else if (store.getId() == null) {
            store.setAllowsPos(true);
        }
        store.setEmail(MoneyAndQuantityUtils.blankToNull(email));
        store.setPhone(MoneyAndQuantityUtils.blankToNull(phone));
        store.setZipCode(MoneyAndQuantityUtils.blankToNull(zipCode));
        store.setStreet(MoneyAndQuantityUtils.blankToNull(street));
        store.setNumber(MoneyAndQuantityUtils.blankToNull(number));
        store.setComplement(MoneyAndQuantityUtils.blankToNull(complement));
        store.setDistrict(MoneyAndQuantityUtils.blankToNull(district));
        store.setCity(MoneyAndQuantityUtils.blankToNull(city));
        store.setState(StringUtils.hasText(state) ? state.trim().toUpperCase() : null);
        store.setTimezone(StringUtils.hasText(timezone) ? timezone.trim() : "America/Sao_Paulo");
    }

    private static String normalizeDocument(String document) {
        String value = MoneyAndQuantityUtils.blankToNull(document);
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
