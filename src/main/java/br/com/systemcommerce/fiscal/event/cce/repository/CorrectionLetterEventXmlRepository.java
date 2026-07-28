package br.com.systemcommerce.fiscal.event.cce.repository;

import br.com.systemcommerce.fiscal.event.cce.entity.CorrectionLetterEventXml;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectionLetterEventXmlRepository extends JpaRepository<CorrectionLetterEventXml, UUID> {

    List<CorrectionLetterEventXml> findByLetterIdOrderByStoredAtDesc(UUID letterId);
}
