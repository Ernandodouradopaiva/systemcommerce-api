package br.com.systemcommerce.integration.repository;

import br.com.systemcommerce.integration.entity.ChannelProduct;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelProductRepository extends JpaRepository<ChannelProduct, UUID> {

    Optional<ChannelProduct> findByMarketplaceAccountIdAndExternalProductId(
            UUID marketplaceAccountId, String externalProductId);

    Optional<ChannelProduct> findByMarketplaceAccountIdAndExternalSku(
            UUID marketplaceAccountId, String externalSku);
}
