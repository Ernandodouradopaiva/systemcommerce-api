package br.com.systemcommerce.supplier.dto;

import br.com.systemcommerce.supplier.entity.SupplierAddress;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupplierAddressRequest(
        @NotNull(message = "tipo de endereço é obrigatório") SupplierAddress.AddressType type,
        @Size(max = 10) String zipCode,
        @Size(max = 200) String street,
        @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @Size(max = 100) String district,
        @Size(max = 100) String city,
        @Size(max = 2) String state,
        Boolean primary) {}
