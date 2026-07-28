package br.com.systemcommerce.integration.repository;

import br.com.systemcommerce.integration.entity.ChannelOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChannelOrderRepository
        extends JpaRepository<ChannelOrder, UUID>, JpaSpecificationExecutor<ChannelOrder> {

    Optional<ChannelOrder> findByMarketplaceAccountIdAndExternalOrderId(
            UUID marketplaceAccountId, String externalOrderId);

    Optional<ChannelOrder> findByMarketplaceAccountIdAndIdempotencyKey(
            UUID marketplaceAccountId, String idempotencyKey);
}
