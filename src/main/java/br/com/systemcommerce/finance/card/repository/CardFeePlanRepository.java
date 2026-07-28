package br.com.systemcommerce.finance.card.repository;

import br.com.systemcommerce.finance.card.entity.CardFeePlan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardFeePlanRepository extends JpaRepository<CardFeePlan, UUID> {
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    List<CardFeePlan> findByOrganizationIdAndStatusOrderByNameAsc(UUID organizationId, CardFeePlan.Status status);
}