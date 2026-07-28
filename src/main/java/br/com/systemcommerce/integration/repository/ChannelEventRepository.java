package br.com.systemcommerce.integration.repository;

import br.com.systemcommerce.integration.entity.ChannelEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelEventRepository extends JpaRepository<ChannelEvent, UUID> {}
