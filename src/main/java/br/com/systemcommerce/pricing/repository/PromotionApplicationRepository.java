package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.PromotionApplication;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionApplicationRepository extends JpaRepository<PromotionApplication, UUID> {

    List<PromotionApplication> findBySaleId(UUID saleId);

    List<PromotionApplication> findBySalesOrderId(UUID salesOrderId);

    List<PromotionApplication> findByQuoteId(UUID quoteId);
}
