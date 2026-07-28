package br.com.systemcommerce.finance.billing.adapter;

import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BillingProviderRegistry {
    private final Map<String, BillingProviderAdapter> byCode;

    public BillingProviderRegistry(List<BillingProviderAdapter> adapters) {
        this.byCode = adapters.stream()
                .collect(Collectors.toMap(a -> a.providerCode().toUpperCase(), Function.identity()));
    }

    public BillingProviderAdapter resolve(String providerCode) {
        String code = providerCode == null || providerCode.isBlank()
                ? StubBillingProviderAdapter.CODE
                : providerCode.trim().toUpperCase();
        BillingProviderAdapter adapter = byCode.get(code);
        if (adapter == null) {
            throw new BusinessRuleException("Provedor de cobrança não suportado: " + code);
        }
        return adapter;
    }
}
