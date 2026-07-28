package br.com.systemcommerce.finance.card.repository;

import br.com.systemcommerce.finance.card.entity.CardSettlement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardSettlementRepository extends JpaRepository<CardSettlement, UUID> {
    Optional<CardSettlement> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);
}