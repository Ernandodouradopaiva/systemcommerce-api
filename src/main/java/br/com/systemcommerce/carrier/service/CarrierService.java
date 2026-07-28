package br.com.systemcommerce.carrier.service;

import br.com.systemcommerce.carrier.dto.CarrierContactRequest;
import br.com.systemcommerce.carrier.dto.CarrierCreateRequest;
import br.com.systemcommerce.carrier.dto.CarrierResponse;
import br.com.systemcommerce.carrier.dto.CarrierUpdateRequest;
import br.com.systemcommerce.carrier.entity.Carrier;
import br.com.systemcommerce.carrier.entity.CarrierContact;
import br.com.systemcommerce.carrier.mapper.CarrierMapper;
import br.com.systemcommerce.carrier.repository.CarrierRepository;
import br.com.systemcommerce.carrier.specification.CarrierSpecifications;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transportadoras + contatos (Prompt 73). Inativa não pode ser selecionada em novas expedições/pedidos. */
@Service
@RequiredArgsConstructor
public class CarrierService {

    private final CarrierRepository carrierRepository;
    private final CarrierMapper carrierMapper;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<CarrierResponse> list(UUID organizationId, Carrier.CarrierStatus status, String search, Pageable pageable) {
        return carrierRepository
                .findAll(CarrierSpecifications.withFilters(organizationId, status, search), pageable)
                .map(carrierMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CarrierResponse getById(UUID id) {
        return carrierMapper.toResponse(getEntity(id));
    }

    @Transactional
    public CarrierResponse create(CarrierCreateRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        assertUniqueCode(organization.getId(), request.code(), null);
        assertUniqueDocument(organization.getId(), request.document(), null);

        Carrier carrier = new Carrier();
        carrier.setOrganization(organization);
        carrier.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        carrier.setLegalName(MoneyAndQuantityUtils.requireText(request.legalName(), "Razão social"));
        carrier.setTradeName(MoneyAndQuantityUtils.blankToNull(request.tradeName()));
        carrier.setDocument(normalizeDocument(request.document()));
        carrier.setStateRegistration(MoneyAndQuantityUtils.blankToNull(request.stateRegistration()));
        carrier.setAnttRntrc(MoneyAndQuantityUtils.blankToNull(request.anttRntrc()));
        carrier.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        carrier.setStatus(Carrier.CarrierStatus.ACTIVE);
        replaceContacts(carrier, request.contacts());

        Carrier saved = carrierRepository.save(carrier);
        domainAuditService.record(
                "LOGISTICS",
                "Carrier",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Transportadora criada");
        return carrierMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public CarrierResponse update(UUID id, CarrierUpdateRequest request) {
        Carrier carrier = getEntity(id);
        Map<String, Object> before = snapshot(carrier);
        assertUniqueCode(carrier.getOrganization().getId(), request.code(), id);
        assertUniqueDocument(carrier.getOrganization().getId(), request.document(), id);

        carrier.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        carrier.setLegalName(MoneyAndQuantityUtils.requireText(request.legalName(), "Razão social"));
        carrier.setTradeName(MoneyAndQuantityUtils.blankToNull(request.tradeName()));
        carrier.setDocument(normalizeDocument(request.document()));
        carrier.setStateRegistration(MoneyAndQuantityUtils.blankToNull(request.stateRegistration()));
        carrier.setAnttRntrc(MoneyAndQuantityUtils.blankToNull(request.anttRntrc()));
        carrier.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        replaceContacts(carrier, request.contacts());

        Carrier saved = carrierRepository.save(carrier);
        domainAuditService.record(
                "LOGISTICS",
                "Carrier",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Transportadora atualizada");
        return carrierMapper.toResponse(getEntity(id));
    }

    @Transactional
    public CarrierResponse activate(UUID id) {
        Carrier carrier = getEntity(id);
        Map<String, Object> before = snapshot(carrier);
        carrier.markActive();
        Carrier saved = carrierRepository.save(carrier);
        domainAuditService.record(
                "LOGISTICS", "Carrier", id, AuditLog.AuditAction.ACTIVATE, before, snapshot(saved), "Transportadora ativada");
        return carrierMapper.toResponse(getEntity(id));
    }

    @Transactional
    public CarrierResponse inactivate(UUID id) {
        Carrier carrier = getEntity(id);
        Map<String, Object> before = snapshot(carrier);
        carrier.markInactive();
        Carrier saved = carrierRepository.save(carrier);
        domainAuditService.record(
                "LOGISTICS",
                "Carrier",
                id,
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(saved),
                "Transportadora inativada — não pode mais ser selecionada");
        return carrierMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Carrier requireUsable(UUID id) {
        Carrier carrier = getEntity(id);
        if (!carrier.isUsable()) {
            throw new BusinessRuleException("Transportadora inativa não pode ser selecionada");
        }
        return carrier;
    }

    @Transactional(readOnly = true)
    public Carrier getEntity(UUID id) {
        return carrierRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transportadora", id));
    }

    private void replaceContacts(Carrier carrier, List<CarrierContactRequest> requests) {
        carrier.getContacts().clear();
        if (requests == null) {
            return;
        }
        for (CarrierContactRequest request : requests) {
            CarrierContact contact = new CarrierContact();
            contact.setCarrier(carrier);
            contact.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome do contato"));
            contact.setPhone(MoneyAndQuantityUtils.blankToNull(request.phone()));
            contact.setEmail(MoneyAndQuantityUtils.blankToNull(request.email()));
            contact.setRoleLabel(MoneyAndQuantityUtils.blankToNull(request.roleLabel()));
            contact.setPrimaryContact(Boolean.TRUE.equals(request.primaryContact()));
            carrier.getContacts().add(contact);
        }
    }

    private void assertUniqueCode(UUID organizationId, String code, UUID id) {
        String normalized = MoneyAndQuantityUtils.requireText(code, "Código");
        boolean exists = id == null
                ? carrierRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, normalized)
                : carrierRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(organizationId, normalized, id);
        if (exists) {
            throw new ConflictException("Código da transportadora já está em uso");
        }
    }

    private void assertUniqueDocument(UUID organizationId, String document, UUID id) {
        String normalized = normalizeDocument(document);
        boolean exists = id == null
                ? carrierRepository.existsByOrganizationIdAndDocument(organizationId, normalized)
                : carrierRepository.existsByOrganizationIdAndDocumentAndIdNot(organizationId, normalized, id);
        if (exists) {
            throw new ConflictException("Documento da transportadora já está em uso");
        }
    }

    private String normalizeDocument(String document) {
        return MoneyAndQuantityUtils.requireText(document, "Documento");
    }

    private Map<String, Object> snapshot(Carrier carrier) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", carrier.getCode());
        map.put("legalName", carrier.getLegalName());
        map.put("document", carrier.getDocument());
        map.put("status", carrier.getStatus());
        map.put("active", carrier.getActive());
        return map;
    }
}
