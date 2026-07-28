package br.com.systemcommerce.fiscal.validation.repository;

import br.com.systemcommerce.fiscal.validation.entity.SchemaUpdateHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemaUpdateHistoryRepository extends JpaRepository<SchemaUpdateHistory, UUID> {

    List<SchemaUpdateHistory> findBySchemaVersionIdOrderByImportedAtDesc(UUID schemaVersionId);
}
