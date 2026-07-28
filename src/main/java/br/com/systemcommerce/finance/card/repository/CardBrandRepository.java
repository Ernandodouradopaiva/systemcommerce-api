package br.com.systemcommerce.finance.card.repository;

import br.com.systemcommerce.finance.card.entity.CardBrand;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardBrandRepository extends JpaRepository<CardBrand, UUID> {
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    List<CardBrand> findByOrganizationIdOrderByNameAsc(UUID organizationId);
}