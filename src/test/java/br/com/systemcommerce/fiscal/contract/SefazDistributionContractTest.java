package br.com.systemcommerce.fiscal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.systemcommerce.fiscal.transmission.dto.NsuDistributionResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Contrato mínimo do stub de distribuição (Prompt 149). */
class SefazDistributionContractTest {

    @Test
    void emptyDistributionUsesCstat137Shape() {
        NsuDistributionResult r =
                new NsuDistributionResult(true, "137", "Nenhum documento", 10, 5L, 5L, List.of());
        assertThat(r.success()).isTrue();
        assertThat(r.cstat()).isEqualTo("137");
        assertThat(r.documents()).isEmpty();
    }
}
