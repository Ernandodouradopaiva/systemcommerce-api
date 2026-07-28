package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.PromotionBenefit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionBenefitRepository extends JpaRepository<PromotionBenefit, UUID> {

    List<PromotionBenefit> findByPromotionIdAndActiveTrue(UUID promotionId);
}
