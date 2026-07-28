package br.com.systemcommerce.fiscal.event.cce;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fiscal.cce")
public class CceBlockedFieldsConfig {

    private List<String> extraBlockedKeywords = List.of();

    public List<String> getExtraBlockedKeywords() {
        return extraBlockedKeywords;
    }

    public void setExtraBlockedKeywords(List<String> extraBlockedKeywords) {
        this.extraBlockedKeywords = extraBlockedKeywords;
    }
}
