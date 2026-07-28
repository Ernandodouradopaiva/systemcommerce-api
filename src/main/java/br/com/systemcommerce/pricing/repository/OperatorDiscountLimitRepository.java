package br.com.systemcommerce.pricing.repository;



import br.com.systemcommerce.pricing.entity.OperatorDiscountLimit;

import java.util.List;

import java.util.Optional;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;



public interface OperatorDiscountLimitRepository extends JpaRepository<OperatorDiscountLimit, UUID> {



    @EntityGraph(attributePaths = "role")

    Optional<OperatorDiscountLimit> findByRoleId(UUID roleId);



    boolean existsByRoleId(UUID roleId);



    @EntityGraph(attributePaths = "role")

    @Query(

            """

            SELECT l FROM OperatorDiscountLimit l

            WHERE l.active = TRUE AND l.role.id IN :roleIds

            """)

    List<OperatorDiscountLimit> findActiveByRoleIds(@Param("roleIds") List<UUID> roleIds);



    @EntityGraph(attributePaths = "role")

    List<OperatorDiscountLimit> findAllByActiveTrue();

}


