package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.Coupon;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    /** Busca por código sem restrição de organização, usada pelo motor de promoções ao simular carrinhos. */
    Optional<Coupon> findFirstByCodeIgnoreCase(String code);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    Page<Coupon> findByOrganizationId(UUID organizationId, Pageable pageable);
}
