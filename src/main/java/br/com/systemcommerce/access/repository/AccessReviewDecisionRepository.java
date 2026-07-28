package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.AccessReviewDecision;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessReviewDecisionRepository extends JpaRepository<AccessReviewDecision, UUID> {}
