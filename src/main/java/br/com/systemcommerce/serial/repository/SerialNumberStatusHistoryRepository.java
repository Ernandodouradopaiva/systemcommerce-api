package br.com.systemcommerce.serial.repository;

import br.com.systemcommerce.serial.entity.SerialNumberStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SerialNumberStatusHistoryRepository extends JpaRepository<SerialNumberStatusHistory, UUID> {

    List<SerialNumberStatusHistory> findByProductSerialIdOrderByChangedAtAsc(UUID productSerialId);
}
