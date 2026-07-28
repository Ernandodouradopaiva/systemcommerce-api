package br.com.systemcommerce.fiscal.numbering.repository;

import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberGap;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberGap.GapStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalNumberGapRepository extends JpaRepository<FiscalNumberGap, UUID> {

    List<FiscalNumberGap> findBySequenceIdAndStatusOrderByFromNumberAsc(UUID sequenceId, GapStatus status);
}
