package br.com.systemcommerce.fiscal.establishment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentHistoryRepository;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalNumberingSeriesRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiscalEstablishmentServiceTest {

    @Mock
    private FiscalEstablishmentRepository establishmentRepository;

    @Mock
    private FiscalEstablishmentHistoryRepository historyRepository;

    @Mock
    private FiscalNumberingSeriesRepository numberingSeriesRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private StoreService storeService;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FiscalEstablishmentService establishmentService;

    @Test
    void shouldRejectInvalidCnpj() {
        assertThatThrownBy(() -> establishmentService.validateCnpj("123"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("14 dígitos");
    }

    @Test
    void shouldRejectInvalidUf() {
        assertThatThrownBy(() -> establishmentService.validateUf("SPX"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("2 letras");
    }

    @Test
    void shouldRejectInvalidIbgeCityCode() {
        assertThatThrownBy(() -> establishmentService.validateIbgeCityCode("12345"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("7 dígitos");
    }
}
