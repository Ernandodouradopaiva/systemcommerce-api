package br.com.systemcommerce.reservation.repository;

import br.com.systemcommerce.reservation.entity.StockReservationStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationStatusHistoryRepository extends JpaRepository<StockReservationStatusHistory, UUID> {

    List<StockReservationStatusHistory> findByStockReservationIdOrderByChangedAtAsc(UUID stockReservationId);
}
