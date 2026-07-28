package br.com.systemcommerce.supplier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.supplier.dto.SupplierBankAccountRequest;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.entity.SupplierBankAccount;
import br.com.systemcommerce.supplier.mapper.SupplierBankAccountMapper;
import br.com.systemcommerce.supplier.repository.SupplierBankAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cobre a regra de negócio de dados bancários no nível de serviço.
 * A restrição de acesso (SUPPLIER_BANK_DATA_READ/MANAGE) é aplicada via @PreAuthorize no controller.
 */
@ExtendWith(MockitoExtension.class)
class SupplierBankAccountServiceTest {

    @Mock
    private SupplierBankAccountRepository bankAccountRepository;

    @Spy
    private SupplierBankAccountMapper bankAccountMapper = new SupplierBankAccountMapper();

    @Mock
    private SupplierService supplierService;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private SupplierBankAccountService bankAccountService;

    @Test
    void shouldCreateBankAccountLinkedToSupplier() {
        UUID supplierId = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        when(supplierService.getEntity(supplierId)).thenReturn(supplier);
        when(bankAccountRepository.save(any(SupplierBankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierBankAccountRequest request = new SupplierBankAccountRequest(
                "001", "1234", "56789-0", SupplierBankAccount.BankAccountType.CHECKING, null, "Fornecedor SA", true);

        bankAccountService.create(supplierId, request);

        ArgumentCaptor<SupplierBankAccount> captor = ArgumentCaptor.forClass(SupplierBankAccount.class);
        org.mockito.Mockito.verify(bankAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getSupplier()).isSameAs(supplier);
        assertThat(captor.getValue().getBankCode()).isEqualTo("001");
        assertThat(captor.getValue().getActive()).isTrue();
    }

    @Test
    void shouldNotExposeAccountNumberInAuditDetails() {
        UUID supplierId = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        when(supplierService.getEntity(supplierId)).thenReturn(supplier);
        when(bankAccountRepository.save(any(SupplierBankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierBankAccountRequest request = new SupplierBankAccountRequest(
                "001", "1234", "999999-9", SupplierBankAccount.BankAccountType.SAVINGS, "chave-pix", "Fornecedor SA", true);

        bankAccountService.create(supplierId, request);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(domainAuditService)
                .record(
                        org.mockito.ArgumentMatchers.eq("SUPPLIER"),
                        org.mockito.ArgumentMatchers.eq("SupplierBankAccount"),
                        any(),
                        any(),
                        any(),
                        any(),
                        detailsCaptor.capture());
        assertThat(detailsCaptor.getValue()).doesNotContain("999999-9").doesNotContain("chave-pix");
    }
}
