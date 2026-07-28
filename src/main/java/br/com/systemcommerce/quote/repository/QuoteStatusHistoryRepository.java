package br.com.systemcommerce.quote.repository;

import br.com.systemcommerce.quote.entity.QuoteStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteStatusHistoryRepository extends JpaRepository<QuoteStatusHistory, UUID> {

    List<QuoteStatusHistory> findByQuoteIdOrderByChangedAtAsc(UUID quoteId);
}
