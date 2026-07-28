package br.com.systemcommerce.finance.card.repository;

import br.com.systemcommerce.finance.card.entity.CardTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, UUID> {
    Optional<CardTransaction> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);
    boolean existsBySaleIdAndPaymentId(UUID saleId, UUID paymentId);

    @Query("select t from CardTransaction t join fetch t.acquirer left join fetch t.schedules where t.id = :id")
    Optional<CardTransaction> findDetailedById(@Param("id") UUID id);
}