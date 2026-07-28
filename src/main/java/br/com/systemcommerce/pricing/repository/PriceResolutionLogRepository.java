package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.PriceResolutionLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceResolutionLogRepository extends JpaRepository<PriceResolutionLog, UUID> {

    List<PriceResolutionLog> findTop50ByProductIdOrderByResolvedAtDesc(UUID productId);
}
