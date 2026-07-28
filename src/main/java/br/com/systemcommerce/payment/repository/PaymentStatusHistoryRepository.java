package br.com.systemcommerce.payment.repository;

import br.com.systemcommerce.payment.entity.PaymentStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, UUID> {

    List<PaymentStatusHistory> findByPaymentIdOrderByChangedAtAsc(UUID paymentId);
}
