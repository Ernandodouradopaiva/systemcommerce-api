package br.com.systemcommerce.integration.repository;

import br.com.systemcommerce.integration.entity.MarketplaceAccount;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MarketplaceAccountRepository
        extends JpaRepository<MarketplaceAccount, UUID>, JpaSpecificationExecutor<MarketplaceAccount> {}
