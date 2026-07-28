package br.com.systemcommerce.finance.paymentcatalog.repository;

import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentMethodStoreConfiguration;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodStoreConfigurationRepository extends JpaRepository<PaymentMethodStoreConfiguration, UUID> {
    List<PaymentMethodStoreConfiguration> findByPaymentMethodId(UUID paymentMethodId);
    boolean existsByPaymentMethodIdAndStoreId(UUID paymentMethodId, UUID storeId);
}
