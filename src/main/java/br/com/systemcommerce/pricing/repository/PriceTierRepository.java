package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.PriceTier;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceTierRepository extends JpaRepository<PriceTier, UUID> {

    List<PriceTier> findByProductPrice_IdOrderByMinQuantityAsc(UUID productPriceId);

    List<PriceTier> findByProductPrice_IdAndActiveTrueOrderByMinQuantityAsc(UUID productPriceId);
}
