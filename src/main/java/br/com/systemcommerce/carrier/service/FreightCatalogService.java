package br.com.systemcommerce.carrier.service;

import br.com.systemcommerce.carrier.dto.FreightModeRequest;
import br.com.systemcommerce.carrier.dto.FreightModeResponse;
import br.com.systemcommerce.carrier.dto.FreightRegionRequest;
import br.com.systemcommerce.carrier.dto.FreightTableCreateRequest;
import br.com.systemcommerce.carrier.dto.FreightTableResponse;
import br.com.systemcommerce.carrier.entity.Carrier;
import br.com.systemcommerce.carrier.entity.FreightMode;
import br.com.systemcommerce.carrier.entity.FreightRegion;
import br.com.systemcommerce.carrier.entity.FreightTable;
import br.com.systemcommerce.carrier.mapper.FreightMapper;
import br.com.systemcommerce.carrier.repository.FreightModeRepository;
import br.com.systemcommerce.carrier.repository.FreightTableRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Modalidades e tabelas de frete (Prompt 73). */
@Service
@RequiredArgsConstructor
public class FreightCatalogService {

    private final FreightModeRepository freightModeRepository;
    private final FreightTableRepository freightTableRepository;
    private final CarrierService carrierService;
    private final OrganizationService organizationService;
    private final FreightMapper freightMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<FreightModeResponse> listModes(UUID organizationId, Pageable pageable) {
        return freightModeRepository
                .findAll((root, query, cb) -> organizationId != null
                        ? cb.equal(root.get("organization").get("id"), organizationId)
                        : cb.conjunction(), pageable)
                .map(freightMapper::toResponse);
    }

    @Transactional
    public FreightModeResponse createMode(FreightModeRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        String code = MoneyAndQuantityUtils.requireText(request.code(), "Código");
        if (freightModeRepository.existsByOrganizationIdAndCodeIgnoreCase(organization.getId(), code)) {
            throw new ConflictException("Código da modalidade de frete já está em uso");
        }
        FreightMode mode = new FreightMode();
        mode.setOrganization(organization);
        mode.setCode(code);
        mode.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        mode.setModeType(request.modeType());
        mode.setStatus(FreightMode.FreightModeStatus.ACTIVE);
        FreightMode saved = freightModeRepository.save(mode);
        domainAuditService.record(
                "LOGISTICS", "FreightMode", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Modalidade de frete criada");
        return freightMapper.toResponse(saved);
    }

    @Transactional
    public FreightModeResponse inactivateMode(UUID id) {
        FreightMode mode = freightModeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modalidade de frete", id));
        mode.setStatus(FreightMode.FreightModeStatus.INACTIVE);
        mode.setActive(false);
        FreightMode saved = freightModeRepository.save(mode);
        domainAuditService.record(
                "LOGISTICS", "FreightMode", id, AuditLog.AuditAction.DEACTIVATE, null, null, "Modalidade de frete inativada");
        return freightMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<FreightTableResponse> listTables(UUID organizationId, Pageable pageable) {
        return freightTableRepository
                .findAll((root, query, cb) -> organizationId != null
                        ? cb.equal(root.get("organization").get("id"), organizationId)
                        : cb.conjunction(), pageable)
                .map(freightMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public FreightTableResponse getTableById(UUID id) {
        return freightMapper.toResponse(getTableEntity(id));
    }

    @Transactional
    public FreightTableResponse createTable(FreightTableCreateRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        FreightTable table = new FreightTable();
        table.setOrganization(organization);
        if (request.carrierId() != null) {
            table.setCarrier(carrierService.requireUsable(request.carrierId()));
        }
        if (request.freightModeId() != null) {
            table.setFreightMode(freightModeRepository
                    .findById(request.freightModeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Modalidade de frete", request.freightModeId())));
        }
        table.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        table.setValidFrom(request.validFrom());
        table.setValidUntil(request.validUntil());
        table.setStatus(FreightTable.FreightTableStatus.ACTIVE);

        if (request.regions() == null || request.regions().isEmpty()) {
            throw new BusinessRuleException("Tabela de frete deve conter ao menos uma região");
        }
        for (FreightRegionRequest regionRequest : request.regions()) {
            FreightRegion region = new FreightRegion();
            region.setFreightTable(table);
            region.setRegionCode(MoneyAndQuantityUtils.requireText(regionRequest.regionCode(), "Código da região"));
            region.setRegionName(MoneyAndQuantityUtils.blankToNull(regionRequest.regionName()));
            region.setZipFrom(MoneyAndQuantityUtils.blankToNull(regionRequest.zipFrom()));
            region.setZipTo(MoneyAndQuantityUtils.blankToNull(regionRequest.zipTo()));
            region.setMinWeight(regionRequest.minWeight());
            region.setMaxWeight(regionRequest.maxWeight());
            region.setMinVolume(regionRequest.minVolume());
            region.setMaxVolume(regionRequest.maxVolume());
            region.setMinOrderAmount(regionRequest.minOrderAmount());
            region.setFreightAmount(MoneyAndQuantityUtils.money(regionRequest.freightAmount()));
            region.setLeadTimeDays(regionRequest.leadTimeDays());
            table.getRegions().add(region);
        }

        FreightTable saved = freightTableRepository.save(table);
        domainAuditService.record(
                "LOGISTICS",
                "FreightTable",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Tabela de frete criada");
        return freightMapper.toResponse(getTableEntity(saved.getId()));
    }

    @Transactional
    public FreightTableResponse inactivateTable(UUID id) {
        FreightTable table = getTableEntity(id);
        table.setStatus(FreightTable.FreightTableStatus.INACTIVE);
        table.setActive(false);
        FreightTable saved = freightTableRepository.save(table);
        domainAuditService.record(
                "LOGISTICS", "FreightTable", id, AuditLog.AuditAction.DEACTIVATE, null, snapshot(saved), "Tabela de frete inativada");
        return freightMapper.toResponse(getTableEntity(id));
    }

    @Transactional(readOnly = true)
    public FreightTable getTableEntity(UUID id) {
        return freightTableRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tabela de frete", id));
    }

    private Map<String, Object> snapshot(FreightTable table) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", table.getName());
        map.put("status", table.getStatus());
        map.put("carrierId", table.getCarrier() != null ? table.getCarrier().getId() : null);
        map.put("regionsCount", table.getRegions().size());
        return map;
    }
}
