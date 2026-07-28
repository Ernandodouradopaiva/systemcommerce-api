package br.com.systemcommerce.integration.repository;

import br.com.systemcommerce.integration.entity.ChannelListing;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelListingRepository extends JpaRepository<ChannelListing, UUID> {}
