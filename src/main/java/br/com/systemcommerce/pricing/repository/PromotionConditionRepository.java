package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.PromotionCondition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionConditionRepository extends JpaRepository<PromotionCondition, UUID> {

    List<PromotionCondition> findByPromotionIdAndActiveTrue(UUID promotionId);
}
