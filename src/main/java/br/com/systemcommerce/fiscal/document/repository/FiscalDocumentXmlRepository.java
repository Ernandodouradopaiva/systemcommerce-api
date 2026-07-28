package br.com.systemcommerce.fiscal.document.repository;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentXml;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalDocumentXmlRepository extends JpaRepository<FiscalDocumentXml, UUID> {

    List<FiscalDocumentXml> findByDocumentId(UUID documentId);
}
