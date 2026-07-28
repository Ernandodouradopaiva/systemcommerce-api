package br.com.systemcommerce.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.customer.dto.CustomerBlockRequest;
import br.com.systemcommerce.customer.dto.CustomerCreateRequest;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.mapper.CustomerMapper;
import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.customer.repository.CustomerStatusHistoryRepository;
import br.com.systemcommerce.customerstore.service.CustomerStoreRelationshipService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private CustomerStoreRelationshipService customerStoreRelationshipService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private CustomerStatusHistoryRepository statusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void shouldRejectDuplicateDocumentOnCreate() {
        when(customerRepository.existsByDocument("52998224725")).thenReturn(true);

        CustomerCreateRequest request = new CustomerCreateRequest(
                Customer.CustomerType.PF,
                "Teste",
                null,
                "52998224725",
                null,
                "a@b.com",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> customerService.create(request)).isInstanceOf(ConflictException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldSoftDeleteWhenCustomerHasSales() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setType(Customer.CustomerType.PF);
        customer.setName("Com venda");
        customer.setDocument("39053344705");
        customer.markActive();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(saleRepository.hasSalesForCustomer(id)).thenReturn(true);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        customerService.delete(id);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Customer.CustomerStatus.INACTIVE);
        verify(customerRepository, never()).delete(any(Customer.class));
    }

    @Test
    void shouldHardDeleteWhenCustomerHasNoSales() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setType(Customer.CustomerType.PF);
        customer.setName("Sem venda");
        customer.setDocument("39053344705");
        customer.markActive();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(saleRepository.hasSalesForCustomer(id)).thenReturn(false);

        customerService.delete(id);

        verify(customerRepository).delete(customer);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldBlockInactiveCustomerForNewSale() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.markInactive();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.requireUsableForSale(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void shouldAllowActiveCustomerForSale() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.markActive();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        assertThat(customerService.requireUsableForSale(id)).isSameAs(customer);
    }

    @Test
    void shouldBlockCustomerAndPreventNewOrder() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.markActive();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        customerService.block(id, new CustomerBlockRequest("Inadimplência confirmada"));

        assertThat(customer.getStatus()).isEqualTo(Customer.CustomerStatus.BLOCKED);
        assertThat(customer.getBlockedReason()).isEqualTo("Inadimplência confirmada");

        assertThatThrownBy(() -> customerService.assertCanCreateOrder(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bloqueado");
    }

    @Test
    void shouldAllowQuoteForBlockedCustomerWhenFlagEnabled() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.markActive();
        customer.setAllowQuoteWhenBlocked(true);
        customer.markBlocked("Limite de crédito excedido");

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        assertThat(customerService.assertCanCreateQuote(id)).isSameAs(customer);
        assertThatThrownBy(() -> customerService.assertCanCreateOrder(id))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldBlockQuoteForBlockedCustomerWhenFlagDisabled() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.markActive();
        customer.setAllowQuoteWhenBlocked(false);
        customer.markBlocked("Bloqueio total");

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.assertCanCreateQuote(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bloqueado");
    }

    @Test
    void shouldUnblockCustomerBackToActive() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.markActive();
        customer.markBlocked("Motivo qualquer");

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        customerService.unblock(id);

        assertThat(customer.getStatus()).isEqualTo(Customer.CustomerStatus.ACTIVE);
        assertThat(customer.getBlockedAt()).isNull();
        assertThat(customerService.assertCanCreateOrder(id)).isSameAs(customer);
    }

    @Test
    void shouldRejectUnblockWhenCustomerNotBlocked() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.markActive();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.unblock(id)).isInstanceOf(BusinessRuleException.class);
    }
}
