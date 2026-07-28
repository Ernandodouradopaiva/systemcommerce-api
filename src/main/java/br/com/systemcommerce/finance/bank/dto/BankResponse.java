package br.com.systemcommerce.finance.bank.dto;

import br.com.systemcommerce.finance.bank.entity.Bank;
import java.util.UUID;

public record BankResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String shortName,
        String countryCode,
        Bank.BankStatus status,
        boolean usable,
        Long version) {}
