package br.com.systemcommerce.shared.document;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentConversionRepository extends JpaRepository<DocumentConversion, UUID> {

    List<DocumentConversion> findByFromTypeAndFromIdOrderByConvertedAtAsc(OriginDocumentType fromType, UUID fromId);

    List<DocumentConversion> findByToTypeAndToIdOrderByConvertedAtAsc(OriginDocumentType toType, UUID toId);
}
