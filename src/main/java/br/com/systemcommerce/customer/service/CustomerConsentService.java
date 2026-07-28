package br.com.systemcommerce.customer.service;

import br.com.systemcommerce.customer.dto.CustomerConsentRequest;
import br.com.systemcommerce.customer.dto.CustomerConsentResponse;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.entity.CustomerConsent;
import br.com.systemcommerce.customer.mapper.CustomerConsentMapper;
import br.com.systemcommerce.customer.repository.CustomerConsentRepository;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Consentimentos LGPD — histórico não apagável; revogação apenas marca revokedAt (nunca remove o registro). */
@Service
@RequiredArgsConstructor
public class CustomerConsentService {

    private final CustomerConsentRepository consentRepository;
    private final CustomerConsentMapper mapper;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    public List<CustomerConsentResponse> list(UUID customerId) {
        customerService.getEntity(customerId);
        return consentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public CustomerConsentResponse create(UUID customerId, CustomerConsentRequest request) {
        Customer customer = customerService.getEntity(customerId);
        CustomerConsent consent = new CustomerConsent();
        consent.setCustomer(customer);
        consent.setType(request.type());
        consent.setGranted(Boolean.TRUE.equals(request.granted()));
        consent.setGrantedAt(Boolean.TRUE.equals(request.granted()) ? Instant.now() : null);
        consent.setNotes(blankToNull(request.notes()));
        return mapper.toResponse(consentRepository.save(consent));
    }

    @Transactional
    public CustomerConsentResponse revoke(UUID customerId, UUID consentId) {
        CustomerConsent consent = consentRepository
                .findByIdAndCustomerId(consentId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Consentimento do cliente", consentId));
        consent.revoke();
        return mapper.toResponse(consentRepository.save(consent));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
