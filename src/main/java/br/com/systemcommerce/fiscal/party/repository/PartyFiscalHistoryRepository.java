package br.com.systemcommerce.fiscal.party.repository;

import br.com.systemcommerce.fiscal.party.PartyType;
import br.com.systemcommerce.fiscal.party.entity.PartyFiscalHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyFiscalHistoryRepository extends JpaRepository<PartyFiscalHistory, UUID> {

    List<PartyFiscalHistory> findByPartyTypeAndPartyIdOrderByChangedAtDesc(PartyType partyType, UUID partyId);
}
