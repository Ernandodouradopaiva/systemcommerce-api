package br.com.systemcommerce.commission.repository;

import br.com.systemcommerce.commission.entity.CommissionAdjustment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionAdjustmentRepository extends JpaRepository<CommissionAdjustment, UUID> {

    List<CommissionAdjustment> findByCalculationId(UUID calculationId);
}
