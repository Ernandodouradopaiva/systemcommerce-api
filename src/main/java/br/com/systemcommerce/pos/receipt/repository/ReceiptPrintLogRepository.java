package br.com.systemcommerce.pos.receipt.repository;

import br.com.systemcommerce.pos.receipt.entity.ReceiptPrintLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ReceiptPrintLogRepository
        extends JpaRepository<ReceiptPrintLog, UUID>, JpaSpecificationExecutor<ReceiptPrintLog> {

    @Query(value = "SELECT nextval('receipt_print_sequence')", nativeQuery = true)
    Long nextSequence();
}
