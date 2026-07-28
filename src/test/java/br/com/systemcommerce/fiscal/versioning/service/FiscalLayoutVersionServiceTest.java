package br.com.systemcommerce.fiscal.versioning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.versioning.entity.FiscalLayoutVersion;
import br.com.systemcommerce.fiscal.versioning.entity.FiscalTaxRuleSetVersion;
import br.com.systemcommerce.fiscal.versioning.repository.FiscalLayoutVersionRepository;
import br.com.systemcommerce.fiscal.versioning.repository.FiscalTaxRuleSetVersionRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiscalLayoutVersionServiceTest {

    @Mock
    private FiscalLayoutVersionRepository layoutVersionRepository;

    @Mock
    private FiscalTaxRuleSetVersionRepository ruleSetRepository;

    @InjectMocks
    private FiscalLayoutVersionService service;

    @Test
    void resolvesByDate() {
        FiscalLayoutVersion v = new FiscalLayoutVersion();
        v.setCode("NFe_4.00_NT2025.002");
        when(layoutVersionRepository.findValidOn("55", LocalDate.of(2026, 6, 1))).thenReturn(List.of(v));
        assertThat(service.resolveForEmission("55", LocalDate.of(2026, 6, 1)).getCode())
                .isEqualTo("NFe_4.00_NT2025.002");
    }

    @Test
    void lockedRuleSetCannotMutate() {
        UUID id = UUID.randomUUID();
        FiscalTaxRuleSetVersion rs = new FiscalTaxRuleSetVersion();
        rs.setId(id);
        rs.setLocked(true);
        when(ruleSetRepository.findById(id)).thenReturn(Optional.of(rs));
        assertThatThrownBy(() -> service.updateRules(id, "{}"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bloqueado");
    }
}
