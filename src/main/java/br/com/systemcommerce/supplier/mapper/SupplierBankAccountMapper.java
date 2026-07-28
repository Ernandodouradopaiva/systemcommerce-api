package br.com.systemcommerce.supplier.mapper;

import br.com.systemcommerce.supplier.dto.SupplierBankAccountRequest;
import br.com.systemcommerce.supplier.dto.SupplierBankAccountResponse;
import br.com.systemcommerce.supplier.entity.SupplierBankAccount;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierBankAccountMapper {

    public SupplierBankAccountResponse toResponse(SupplierBankAccount entity) {
        return new SupplierBankAccountResponse(
                entity.getId(),
                entity.getSupplier().getId(),
                entity.getBankCode(),
                entity.getAgency(),
                entity.getAccount(),
                entity.getAccountType(),
                entity.getPixKey(),
                entity.getHolderName(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public void apply(SupplierBankAccount entity, SupplierBankAccountRequest request) {
        entity.setBankCode(blankToNull(request.bankCode()));
        entity.setAgency(blankToNull(request.agency()));
        entity.setAccount(blankToNull(request.account()));
        entity.setAccountType(request.accountType());
        entity.setPixKey(blankToNull(request.pixKey()));
        entity.setHolderName(blankToNull(request.holderName()));
        entity.setActive(request.active() == null || request.active());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
