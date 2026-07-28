package br.com.systemcommerce.fiscal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "systemcommerce.fiscal")
public class FiscalProperties {

    private Sefaz sefaz = new Sefaz();
    private Signing signing = new Signing();
    private Storage storage = new Storage();

    @Getter
    @Setter
    public static class Sefaz {
        /** Quando true, adapters simulam sucesso em homologação (padrão true). */
        private boolean stub = true;
        private int circuitBreakerFailureThreshold = 3;
        private int circuitBreakerOpenMinutes = 5;
        private int maxNetworkRetries = 2;
    }

    @Getter
    @Setter
    public static class Signing {
        /** Permite TestSignatureProvider quando certificado A1 indisponível (não-produção). */
        private boolean allowTestProvider = true;
    }

    @Getter
    @Setter
    public static class Storage {
        private String backend = "LOCAL";
        private String localBasePath = "./data/fiscal-xml";
        private boolean encryptAtRest = false;
    }
}
