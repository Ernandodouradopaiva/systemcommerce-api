package br.com.systemcommerce.customer.dto;

import br.com.systemcommerce.customer.entity.CustomerAddress;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerAddressRequest(
        @NotNull(message = "tipo de endereço é obrigatório") CustomerAddress.AddressType type,
        @Size(max = 10) String zipCode,
        @Size(max = 200) String street,
        @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @Size(max = 100) String district,
        @Size(max = 100) String city,
        @Size(max = 2) String state,
        Boolean isDefault,
        @Size(max = 500) String notes) {}
