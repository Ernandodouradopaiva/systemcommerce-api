package br.com.systemcommerce.quote.repository;

import br.com.systemcommerce.quote.entity.QuoteRevision;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRevisionRepository extends JpaRepository<QuoteRevision, UUID> {

    List<QuoteRevision> findByQuote_IdOrderByRevisionNumberDesc(UUID quoteId);

    long countByQuote_Id(UUID quoteId);
}
