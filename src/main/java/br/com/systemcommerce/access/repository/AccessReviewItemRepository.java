package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.AccessReviewItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessReviewItemRepository extends JpaRepository<AccessReviewItem, UUID> {
    List<AccessReviewItem> findByReviewId(UUID reviewId);
}
