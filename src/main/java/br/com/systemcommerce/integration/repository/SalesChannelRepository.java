package br.com.systemcommerce.integration.repository;

import br.com.systemcommerce.integration.entity.SalesChannel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalesChannelRepository
        extends JpaRepository<SalesChannel, UUID>, JpaSpecificationExecutor<SalesChannel> {

    Optional<SalesChannel> findByOrganizationIdAndCode(UUID organizationId, String code);
}
