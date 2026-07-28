package br.com.systemcommerce.customer.service;

import br.com.systemcommerce.customer.dto.CustomerContactRequest;
import br.com.systemcommerce.customer.dto.CustomerContactResponse;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.entity.CustomerContact;
import br.com.systemcommerce.customer.mapper.CustomerContactMapper;
import br.com.systemcommerce.customer.repository.CustomerContactRepository;
import br.com.systemcommerce.customer.validation.BrazilianDocumentUtils;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerContactService {

    private final CustomerContactRepository contactRepository;
    private final CustomerContactMapper mapper;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    public List<CustomerContactResponse> list(UUID customerId) {
        customerService.getEntity(customerId);
        return contactRepository.findByCustomerIdOrderByCreatedAtAsc(customerId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public CustomerContactResponse create(UUID customerId, CustomerContactRequest request) {
        Customer customer = customerService.getEntity(customerId);
        BrazilianDocumentUtils.assertValidEmail(request.email());
        CustomerContact contact = new CustomerContact();
        contact.setCustomer(customer);
        mapper.apply(contact, request);
        return mapper.toResponse(contactRepository.save(contact));
    }

    @Transactional
    public CustomerContactResponse update(UUID customerId, UUID contactId, CustomerContactRequest request) {
        BrazilianDocumentUtils.assertValidEmail(request.email());
        CustomerContact contact = requireContact(customerId, contactId);
        mapper.apply(contact, request);
        return mapper.toResponse(contactRepository.save(contact));
    }

    @Transactional
    public void delete(UUID customerId, UUID contactId) {
        CustomerContact contact = requireContact(customerId, contactId);
        contact.setActive(false);
        contactRepository.save(contact);
    }

    private CustomerContact requireContact(UUID customerId, UUID contactId) {
        return contactRepository
                .findByIdAndCustomerId(contactId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Contato do cliente", contactId));
    }
}
