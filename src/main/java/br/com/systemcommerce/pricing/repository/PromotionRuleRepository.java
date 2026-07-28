package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.PromotionRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRuleRepository extends JpaRepository<PromotionRule, UUID> {

    List<PromotionRule> findByPromotionIdAndActiveTrueOrderBySortOrderAsc(UUID promotionId);
}
