package br.com.systemcommerce.pos.sale.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.pos.suspended-sales")
public class SuspendedSaleProperties {

    /** TTL da venda suspensa antes de expirar (não recuperável). */
    private Duration ttl = Duration.ofHours(72);

    /** Recuperação limitada à mesma loja. */
    private boolean sameStoreOnly = true;

    /** Tempo máximo do bloqueio de edição sem renovação. */
    private Duration editLockTtl = Duration.ofMinutes(30);
}
