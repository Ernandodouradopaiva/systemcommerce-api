package br.com.systemcommerce.finance.policy.repository;

import br.com.systemcommerce.finance.policy.entity.SettlementAdjustment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementAdjustmentRepository extends JpaRepository<SettlementAdjustment, UUID> {

    List<SettlementAdjustment> findBySettlementTypeAndSettlementId(
            SettlementAdjustment.SettlementType settlementType, UUID settlementId);
}
