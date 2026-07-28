package br.com.systemcommerce.product.repository;

import br.com.systemcommerce.product.entity.Category;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    Optional<Category> findByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "parent")
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "parent")
    Page<Category> findAll(Specification<Category> spec, Pageable pageable);

    @Query(
            """
            SELECT COUNT(c) > 0 FROM Category c
            WHERE c.parent.id = :parentId
            """)
    boolean hasChildren(@Param("parentId") UUID parentId);
}
