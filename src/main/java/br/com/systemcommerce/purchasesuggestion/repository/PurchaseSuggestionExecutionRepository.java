package br.com.systemcommerce.purchasesuggestion.repository;

import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionExecution;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseSuggestionExecutionRepository extends JpaRepository<PurchaseSuggestionExecution, UUID> {}
