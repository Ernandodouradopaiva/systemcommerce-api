package br.com.systemcommerce.customer.service;

import br.com.systemcommerce.customer.dto.CustomerBlockRequest;
import br.com.systemcommerce.customer.dto.CustomerCreateRequest;
import br.com.systemcommerce.customer.dto.CustomerResponse;
import br.com.systemcommerce.customer.dto.CustomerStatusHistoryResponse;
import br.com.systemcommerce.customer.dto.CustomerUpdateRequest;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.entity.CustomerStatusHistory;
import br.com.systemcommerce.customer.mapper.CustomerMapper;
import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.customer.repository.CustomerStatusHistoryRepository;
import br.com.systemcommerce.customer.specification.CustomerSpecifications;
import br.com.systemcommerce.customerstore.service.CustomerStoreRelationshipService;
import br.com.systemcommerce.customer.validation.BrazilianDocumentUtils;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final CustomerMapper customerMapper;
    private final CustomerStoreRelationshipService customerStoreRelationshipService;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;
    private final CustomerStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(
            String name, String document, Customer.CustomerStatus status, String search, Pageable pageable) {
        return customerRepository
                .findAll(CustomerSpecifications.withFilters(name, document, status, search), pageable)
                .map(customerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return customerMapper.toResponse(getEntity(id));
    }

    @Transactional
    public CustomerResponse create(CustomerCreateRequest request) {
        String document = BrazilianDocumentUtils.normalizeAndValidate(request.type(), request.document());
        BrazilianDocumentUtils.assertValidEmail(request.email());
        BrazilianDocumentUtils.assertUniqueDocument(customerRepository.existsByDocument(document));

        Customer customer = new Customer();
        customerMapper.applyCreate(customer, request, document);
        if (request.storeId() == null) {
            customer.setOrganization(organizationService.requireDefault());
        }
        Customer saved = customerRepository.save(customer);
        customerStoreRelationshipService.ensureOnCustomerCreate(saved, request.storeId());
        saved = customerRepository.save(saved);

        domainAuditService.recordCustomer(
                saved.getId(), AuditLog.AuditAction.CREATE, null, snapshot(saved), "Cliente criado");
        recordStatusHistory(saved, null, saved.getStatus(), "Cadastro criado");
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerUpdateRequest request) {
        Customer customer = getEntity(id);
        Map<String, Object> before = snapshot(customer);

        String document = BrazilianDocumentUtils.normalizeAndValidate(request.type(), request.document());
        BrazilianDocumentUtils.assertValidEmail(request.email());
        BrazilianDocumentUtils.assertUniqueDocument(customerRepository.existsByDocumentAndIdNot(document, id));

        customerMapper.applyUpdate(customer, request, document);
        Customer saved = customerRepository.save(customer);

        domainAuditService.recordCustomer(
                saved.getId(), AuditLog.AuditAction.UPDATE, before, snapshot(saved), "Cliente atualizado");
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse activate(UUID id) {
        Customer customer = getEntity(id);
        Map<String, Object> before = snapshot(customer);
        Customer.CustomerStatus previous = customer.getStatus();
        customer.markActive();
        Customer saved = customerRepository.save(customer);
        domainAuditService.recordCustomer(
                saved.getId(), AuditLog.AuditAction.ACTIVATE, before, snapshot(saved), "Cliente ativado");
        recordStatusHistory(saved, previous, saved.getStatus(), "Cliente ativado");
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse deactivate(UUID id) {
        Customer customer = getEntity(id);
        Map<String, Object> before = snapshot(customer);
        Customer.CustomerStatus previous = customer.getStatus();
        customer.markInactive();
        Customer saved = customerRepository.save(customer);
        domainAuditService.recordCustomer(
                saved.getId(), AuditLog.AuditAction.DEACTIVATE, before, snapshot(saved), "Cliente inativado");
        recordStatusHistory(saved, previous, saved.getStatus(), "Cliente inativado");
        return customerMapper.toResponse(saved);
    }

    /** Cliente BLOCKED não gera novo pedido/venda; orçamento segue {@code allowQuoteWhenBlocked}. */
    @Transactional
    public CustomerResponse block(UUID id, CustomerBlockRequest request) {
        Customer customer = getEntity(id);
        Map<String, Object> before = snapshot(customer);
        Customer.CustomerStatus previous = customer.getStatus();
        customer.markBlocked(request.reason());
        Customer saved = customerRepository.save(customer);
        domainAuditService.recordCustomer(
                saved.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                before,
                snapshot(saved),
                "Cliente bloqueado: " + request.reason());
        recordStatusHistory(saved, previous, saved.getStatus(), request.reason());
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse unblock(UUID id) {
        Customer customer = getEntity(id);
        if (customer.getStatus() != Customer.CustomerStatus.BLOCKED) {
            throw new BusinessRuleException("Cliente não está bloqueado");
        }
        Map<String, Object> before = snapshot(customer);
        Customer.CustomerStatus previous = customer.getStatus();
        customer.markActive();
        Customer saved = customerRepository.save(customer);
        domainAuditService.recordCustomer(
                saved.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                before,
                snapshot(saved),
                "Cliente desbloqueado");
        recordStatusHistory(saved, previous, saved.getStatus(), "Cliente desbloqueado");
        return customerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomerStatusHistoryResponse> statusHistory(UUID customerId, Pageable pageable) {
        getEntity(customerId);
        return statusHistoryRepository
                .findByCustomerIdOrderByChangedAtDesc(customerId, pageable)
                .map(this::toStatusHistoryResponse);
    }

    /**
     * Exclusão lógica (inativação) quando houver vínculo com venda; sem vínculo remove fisicamente.
     */
    @Transactional
    public void delete(UUID id) {
        Customer customer = getEntity(id);
        Map<String, Object> before = snapshot(customer);
        boolean hasSales = saleRepository.hasSalesForCustomer(id);

        if (hasSales) {
            Customer.CustomerStatus previous = customer.getStatus();
            customer.markInactive();
            customerRepository.save(customer);
            domainAuditService.recordCustomer(
                    id,
                    AuditLog.AuditAction.DELETE,
                    before,
                    snapshot(customer),
                    "Exclusão lógica: cliente possui vínculo com venda(s)");
            recordStatusHistory(customer, previous, customer.getStatus(), "Exclusão lógica (vínculo com venda)");
            return;
        }

        customerRepository.delete(customer);
        domainAuditService.recordCustomer(
                id, AuditLog.AuditAction.DELETE, before, null, "Cliente removido fisicamente (sem vendas)");
    }

    /** Cliente inativo/bloqueado não pode ser utilizado em uma nova venda (uso genérico/legado). */
    @Transactional(readOnly = true)
    public Customer requireUsableForSale(UUID customerId) {
        Customer customer = getEntity(customerId);
        if (!customer.isUsableForSale()) {
            throw new BusinessRuleException(unusableMessage(customer, "venda"));
        }
        return customer;
    }

    /** Pedido de venda/venda exige cliente ACTIVE — BLOCKED nunca gera novo pedido, independente da flag. */
    @Transactional(readOnly = true)
    public Customer assertCanCreateOrder(UUID customerId) {
        Customer customer = getEntity(customerId);
        if (!customer.isUsableForSale()) {
            throw new BusinessRuleException(unusableMessage(customer, "pedido/venda"));
        }
        return customer;
    }

    /** Orçamento permite cliente BLOCKED quando {@code allowQuoteWhenBlocked} = true; INACTIVE nunca permite. */
    @Transactional(readOnly = true)
    public Customer assertCanCreateQuote(UUID customerId) {
        Customer customer = getEntity(customerId);
        if (!customer.isUsableForQuote()) {
            throw new BusinessRuleException(unusableMessage(customer, "orçamento"));
        }
        return customer;
    }

    private String unusableMessage(Customer customer, String operation) {
        if (customer.getStatus() == Customer.CustomerStatus.BLOCKED) {
            return "Cliente bloqueado não pode ser utilizado em uma nova " + operation
                    + (customer.getBlockedReason() != null ? " (motivo: " + customer.getBlockedReason() + ")" : "");
        }
        return "Cliente inativo não pode ser utilizado em uma nova " + operation;
    }

    Customer getEntity(UUID id) {
        return customerRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }

    private void recordStatusHistory(
            Customer customer, Customer.CustomerStatus previous, Customer.CustomerStatus current, String reason) {
        CustomerStatusHistory history = new CustomerStatusHistory();
        history.setCustomer(customer);
        history.setPreviousStatus(previous);
        history.setNewStatus(current);
        history.setReason(reason);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private CustomerStatusHistoryResponse toStatusHistoryResponse(CustomerStatusHistory history) {
        return new CustomerStatusHistoryResponse(
                history.getId(),
                history.getCustomer().getId(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getReason(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null,
                history.getChangedBy() != null ? history.getChangedBy().getLogin() : null,
                history.getChangedAt());
    }

    private Map<String, Object> snapshot(Customer customer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", customer.getType());
        map.put("name", customer.getName());
        map.put("tradeName", customer.getTradeName());
        map.put("document", customer.getDocument());
        map.put("email", customer.getEmail());
        map.put("status", customer.getStatus());
        map.put("active", customer.getActive());
        map.put("city", customer.getCity());
        map.put("state", customer.getState());
        map.put("classification", customer.getClassification());
        map.put("blockedReason", customer.getBlockedReason());
        return map;
    }
}
