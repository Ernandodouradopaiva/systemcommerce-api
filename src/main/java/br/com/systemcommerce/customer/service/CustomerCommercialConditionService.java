package br.com.systemcommerce.customer.service;

import br.com.systemcommerce.customer.dto.CustomerCommercialConditionRequest;
import br.com.systemcommerce.customer.dto.CustomerCommercialConditionResponse;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.entity.CustomerCommercialCondition;
import br.com.systemcommerce.customer.mapper.CustomerCommercialConditionMapper;
import br.com.systemcommerce.customer.repository.CustomerCommercialConditionRepository;
import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.pricing.repository.PriceTableRepository;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Condição comercial em nível de organização — não duplica por loja (uma condição por cliente). */
@Service
@RequiredArgsConstructor
public class CustomerCommercialConditionService {

    private final CustomerCommercialConditionRepository conditionRepository;
    private final CustomerCommercialConditionMapper mapper;
    private final CustomerService customerService;
    private final PriceTableRepository priceTableRepository;

    @Transactional(readOnly = true)
    public Optional<CustomerCommercialConditionResponse> get(UUID customerId) {
        customerService.getEntity(customerId);
        return conditionRepository.findByCustomerId(customerId).map(mapper::toResponse);
    }

    @Transactional
    public CustomerCommercialConditionResponse upsert(UUID customerId, CustomerCommercialConditionRequest request) {
        Customer customer = customerService.getEntity(customerId);
        CustomerCommercialCondition condition = conditionRepository
                .findByCustomerId(customerId)
                .orElseGet(() -> {
                    CustomerCommercialCondition created = new CustomerCommercialCondition();
                    created.setCustomer(customer);
                    return created;
                });

        condition.setPaymentTermDays(request.paymentTermDays());
        condition.setPaymentCondition(blankToNull(request.paymentCondition()));
        condition.setNotes(blankToNull(request.notes()));
        condition.setPriceTable(resolvePriceTable(request.priceTableId()));

        return mapper.toResponse(conditionRepository.save(condition));
    }

    private PriceTable resolvePriceTable(UUID priceTableId) {
        if (priceTableId == null) {
            return null;
        }
        return priceTableRepository
                .findById(priceTableId)
                .orElseThrow(() -> new ResourceNotFoundException("Tabela de preço", priceTableId));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
