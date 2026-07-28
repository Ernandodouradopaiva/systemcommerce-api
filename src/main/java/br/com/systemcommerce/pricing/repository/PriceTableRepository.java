package br.com.systemcommerce.pricing.repository;



import br.com.systemcommerce.pricing.entity.PriceTable;

import java.util.Optional;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;



public interface PriceTableRepository extends JpaRepository<PriceTable, UUID>, JpaSpecificationExecutor<PriceTable> {



    boolean existsByCodeIgnoreCase(String code);



    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);



    @EntityGraph(attributePaths = {"stores", "storeGroup", "storeGroup.members", "storeGroup.members.store"})

    @Query("SELECT t FROM PriceTable t WHERE t.id = :id")

    Optional<PriceTable> findDetailedById(@Param("id") UUID id);

}


