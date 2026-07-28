package br.com.systemcommerce.shared.document;

import br.com.systemcommerce.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro canônico de conversões documento→documento (Prompt 56).
 * Histórico imutável — nunca atualizado ou apagado após criado.
 */
@Service
@RequiredArgsConstructor
public class DocumentConversionService {

    private final DocumentConversionRepository documentConversionRepository;

    public record ItemConversion(
            UUID fromItemId, UUID toItemId, BigDecimal quantitySource, BigDecimal quantityConverted, BigDecimal quantityRemaining) {}

    @Transactional
    public DocumentConversion record(
            UUID organizationId,
            UUID storeId,
            OriginDocumentType fromType,
            UUID fromId,
            String fromNumber,
            OriginDocumentType toType,
            UUID toId,
            String toNumber,
            List<ItemConversion> items,
            String notes) {
        DocumentConversion conversion = new DocumentConversion();
        conversion.setOrganizationId(organizationId);
        conversion.setStoreId(storeId);
        conversion.setFromType(fromType);
        conversion.setFromId(fromId);
        conversion.setFromNumber(fromNumber);
        conversion.setToType(toType);
        conversion.setToId(toId);
        conversion.setToNumber(toNumber);
        conversion.setConvertedAt(Instant.now());
        conversion.setConvertedByUserId(CurrentUser.id().orElse(null));
        conversion.setNotes(notes);
        if (items != null) {
            for (ItemConversion item : items) {
                DocumentConversionItem entity = new DocumentConversionItem();
                entity.setFromItemId(item.fromItemId());
                entity.setToItemId(item.toItemId());
                entity.setQuantitySource(item.quantitySource() != null ? item.quantitySource() : BigDecimal.ZERO);
                entity.setQuantityConverted(
                        item.quantityConverted() != null ? item.quantityConverted() : BigDecimal.ZERO);
                entity.setQuantityRemaining(
                        item.quantityRemaining() != null ? item.quantityRemaining() : BigDecimal.ZERO);
                conversion.addItem(entity);
            }
        }
        return documentConversionRepository.save(conversion);
    }
}
