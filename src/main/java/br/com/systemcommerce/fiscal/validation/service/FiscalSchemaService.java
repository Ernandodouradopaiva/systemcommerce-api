package br.com.systemcommerce.fiscal.validation.service;

import br.com.systemcommerce.fiscal.validation.dto.FiscalSchemaCreateRequest;
import br.com.systemcommerce.fiscal.validation.dto.FiscalSchemaResponse;
import br.com.systemcommerce.fiscal.validation.entity.FiscalSchemaVersion;
import br.com.systemcommerce.fiscal.validation.entity.FiscalSchemaVersion.SchemaStatus;
import br.com.systemcommerce.fiscal.validation.entity.SchemaUpdateHistory;
import br.com.systemcommerce.fiscal.validation.repository.FiscalSchemaRepository;
import br.com.systemcommerce.fiscal.validation.repository.SchemaUpdateHistoryRepository;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FiscalSchemaService {

    private final FiscalSchemaRepository schemaRepository;
    private final SchemaUpdateHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public List<FiscalSchemaResponse> listByModel(String model) {
        return schemaRepository.findByModelAndActiveTrueOrderByValidFromDesc(model).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FiscalSchemaVersion resolveActiveSchema(String model, LocalDate onDate) {
        LocalDate date = onDate != null ? onDate : LocalDate.now();
        return schemaRepository
                .findActiveForModel(model, SchemaStatus.ACTIVE, date)
                .orElse(null);
    }

    @Transactional
    public FiscalSchemaResponse create(FiscalSchemaCreateRequest request) {
        FiscalSchemaVersion schema = new FiscalSchemaVersion();
        schema.setModel(request.model());
        schema.setLayoutVersion(request.layoutVersion());
        schema.setSchemaNamespace(request.schemaNamespace());
        schema.setXsdResourcePath(request.xsdResourcePath());
        schema.setXsdContent(request.xsdContent());
        schema.setValidFrom(request.validFrom());
        schema.setValidUntil(request.validUntil());
        schema.setStatus(request.status() != null ? request.status() : SchemaStatus.DRAFT);
        FiscalSchemaVersion saved = schemaRepository.save(schema);
        return toResponse(saved);
    }

    @Transactional
    public FiscalSchemaResponse importSchema(UUID schemaId, String source, String notes) {
        FiscalSchemaVersion schema = schemaRepository
                .findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema fiscal", schemaId));
        SchemaUpdateHistory history = new SchemaUpdateHistory();
        history.setSchemaVersion(schema);
        history.setSource(source);
        history.setNotes(notes);
        CurrentUser.id().ifPresent(history::setImportedBy);
        historyRepository.save(history);
        if (StringUtils.hasText(source) && !StringUtils.hasText(schema.getXsdResourcePath())) {
            schema.setXsdResourcePath(source);
        }
        schema.setStatus(SchemaStatus.ACTIVE);
        return toResponse(schemaRepository.save(schema));
    }

    private FiscalSchemaResponse toResponse(FiscalSchemaVersion s) {
        return new FiscalSchemaResponse(
                s.getId(),
                s.getModel(),
                s.getLayoutVersion(),
                s.getSchemaNamespace(),
                s.getXsdResourcePath(),
                s.getStatus(),
                s.getValidFrom(),
                s.getValidUntil());
    }
}
