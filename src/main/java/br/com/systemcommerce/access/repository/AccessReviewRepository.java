package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.AccessReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessReviewRepository extends JpaRepository<AccessReview, UUID> {
    List<AccessReview> findAllByOrderByCreatedAtDesc();

    boolean existsByCode(String code);
}
