package br.com.systemcommerce.quote.repository;

import br.com.systemcommerce.quote.entity.QuoteAcceptance;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteAcceptanceRepository extends JpaRepository<QuoteAcceptance, UUID> {

    List<QuoteAcceptance> findByQuote_IdOrderByAcceptedAtDesc(UUID quoteId);
}
