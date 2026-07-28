package br.com.systemcommerce.finance.card.repository;

import br.com.systemcommerce.finance.card.entity.CardChargeback;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardChargebackRepository extends JpaRepository<CardChargeback, UUID> {
    Optional<CardChargeback> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);
}