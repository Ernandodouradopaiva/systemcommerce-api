package br.com.systemcommerce.carrier.repository;

import br.com.systemcommerce.carrier.entity.CarrierContact;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrierContactRepository extends JpaRepository<CarrierContact, UUID> {

    List<CarrierContact> findByCarrierIdAndActiveTrue(UUID carrierId);
}
