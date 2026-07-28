package br.com.systemcommerce.customer.service;

import br.com.systemcommerce.customer.dto.CustomerAddressRequest;
import br.com.systemcommerce.customer.dto.CustomerAddressResponse;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.entity.CustomerAddress;
import br.com.systemcommerce.customer.mapper.CustomerAddressMapper;
import br.com.systemcommerce.customer.repository.CustomerAddressRepository;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final CustomerAddressMapper mapper;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> list(UUID customerId) {
        customerService.getEntity(customerId);
        return addressRepository.findByCustomerIdOrderByCreatedAtAsc(customerId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public CustomerAddressResponse create(UUID customerId, CustomerAddressRequest request) {
        Customer customer = customerService.getEntity(customerId);
        CustomerAddress address = new CustomerAddress();
        address.setCustomer(customer);
        mapper.apply(address, request);
        return mapper.toResponse(addressRepository.save(address));
    }

    @Transactional
    public CustomerAddressResponse update(UUID customerId, UUID addressId, CustomerAddressRequest request) {
        CustomerAddress address = requireAddress(customerId, addressId);
        mapper.apply(address, request);
        return mapper.toResponse(addressRepository.save(address));
    }

    @Transactional
    public void delete(UUID customerId, UUID addressId) {
        CustomerAddress address = requireAddress(customerId, addressId);
        address.setActive(false);
        addressRepository.save(address);
    }

    private CustomerAddress requireAddress(UUID customerId, UUID addressId) {
        return addressRepository
                .findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço do cliente", addressId));
    }
}
