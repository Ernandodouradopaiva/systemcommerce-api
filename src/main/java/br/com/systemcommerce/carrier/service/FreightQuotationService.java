package br.com.systemcommerce.carrier.service;

import br.com.systemcommerce.carrier.dto.FreightQuotationRequest;
import br.com.systemcommerce.carrier.dto.FreightQuotationResponse;
import br.com.systemcommerce.carrier.entity.Carrier;
import br.com.systemcommerce.carrier.entity.FreightMode;
import br.com.systemcommerce.carrier.entity.FreightQuotation;
import br.com.systemcommerce.carrier.entity.FreightRegion;
import br.com.systemcommerce.carrier.entity.FreightTable;
import br.com.systemcommerce.carrier.mapper.FreightMapper;
import br.com.systemcommerce.carrier.repository.FreightQuotationRepository;
import br.com.systemcommerce.carrier.repository.FreightTableRepository;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.AccessDeniedBusinessException;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cálculo de frete (Prompt 73): busca tabela usável por transportadora/modalidade, seleciona a
 * região compatível por CEP/peso/valor e retorna o menor valor elegível. Permite override manual,
 * restrito a usuários com permissão {@code CARRIER_MANAGE}.
 */
@Service
@RequiredArgsConstructor
public class FreightQuotationService {

    public static final String MANUAL_OVERRIDE_AUTHORITY = "CARRIER_MANAGE";

    private final FreightTableRepository freightTableRepository;
    private final FreightQuotationRepository freightQuotationRepository;
    private final FreightMapper freightMapper;
    private final DomainAuditService domainAuditService;

    @Transactional
    public FreightQuotationResponse calculate(FreightQuotationRequest request) {
        FreightQuotation quotation = new FreightQuotation();
        quotation.setOrganizationId(request.organizationId());
        quotation.setStoreId(request.storeId());
        quotation.setSalesOrderId(request.salesOrderId());
        quotation.setQuoteId(request.quoteId());
        quotation.setZipCode(request.zipCode());
        quotation.setWeight(request.weight());
        quotation.setVolume(request.volume());
        quotation.setOrderAmount(request.orderAmount());
        quotation.setNotes(request.notes());
        quotation.setCalculatedBy(br.com.systemcommerce.shared.security.CurrentUser.id().orElse(null));

        if (request.manualOverrideAmount() != null) {
            if (!SecurityAuthorities.hasAuthority(MANUAL_OVERRIDE_AUTHORITY)) {
                throw new AccessDeniedBusinessException(
                        "Usuário não autorizado a informar valor de frete manual (override)");
            }
            quotation.setManualOverride(Boolean.TRUE);
            quotation.setOverrideAmount(request.manualOverrideAmount());
            quotation.setCalculatedAmount(request.manualOverrideAmount());
            quotation.setSource(FreightQuotation.Source.MANUAL);
            quotation.setCarrier(resolveCarrier(request));
            quotation.setFreightMode(resolveFreightMode(request));
        } else {
            MatchResult match = findBestMatch(request);
            quotation.setCalculatedAmount(match.region().getFreightAmount());
            quotation.setSource(FreightQuotation.Source.TABLE);
            quotation.setCarrier(match.table().getCarrier());
            quotation.setFreightMode(match.table().getFreightMode());
        }

        FreightQuotation saved = freightQuotationRepository.save(quotation);
        domainAuditService.record(
                "LOGISTICS",
                "FreightQuotation",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Cotação de frete calculada");
        return freightMapper.toResponse(saved);
    }

    private Carrier resolveCarrier(FreightQuotationRequest request) {
        if (request.carrierId() == null) {
            return null;
        }
        return freightTableRepository.findUsableTables(request.organizationId()).stream()
                .map(FreightTable::getCarrier)
                .filter(c -> c != null && c.getId().equals(request.carrierId()))
                .findFirst()
                .orElse(null);
    }

    private FreightMode resolveFreightMode(FreightQuotationRequest request) {
        if (request.freightModeId() == null) {
            return null;
        }
        return freightTableRepository.findUsableTables(request.organizationId()).stream()
                .map(FreightTable::getFreightMode)
                .filter(m -> m != null && m.getId().equals(request.freightModeId()))
                .findFirst()
                .orElse(null);
    }

    private MatchResult findBestMatch(FreightQuotationRequest request) {
        LocalDate today = LocalDate.now();
        List<FreightTable> tables = freightTableRepository.findUsableTables(request.organizationId()).stream()
                .filter(FreightTable::isUsable)
                .filter(t -> t.isValidOn(today))
                .filter(t -> request.carrierId() == null
                        || (t.getCarrier() != null && t.getCarrier().getId().equals(request.carrierId())))
                .filter(t -> request.freightModeId() == null
                        || (t.getFreightMode() != null && t.getFreightMode().getId().equals(request.freightModeId())))
                .filter(t -> request.carrierId() == null
                        || t.getCarrier() == null
                        || t.getCarrier().isUsable())
                .toList();

        Optional<MatchResult> best = tables.stream()
                .flatMap(table -> table.getRegions().stream()
                        .filter(region -> region.matchesZip(request.zipCode()))
                        .filter(region -> region.matchesWeight(request.weight()))
                        .filter(region -> region.matchesOrderAmount(request.orderAmount()))
                        .map(region -> new MatchResult(table, region)))
                .min(Comparator.comparing(m -> m.region().getFreightAmount()));

        return best.orElseThrow(() -> new BusinessRuleException(
                "Nenhuma tabela de frete elegível encontrada para os parâmetros informados (CEP/peso/valor)"));
    }

    private Map<String, Object> snapshot(FreightQuotation quotation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("calculatedAmount", quotation.getCalculatedAmount());
        map.put("source", quotation.getSource());
        map.put("manualOverride", quotation.getManualOverride());
        map.put("carrierId", quotation.getCarrier() != null ? quotation.getCarrier().getId() : null);
        return map;
    }

    private record MatchResult(FreightTable table, FreightRegion region) {}
}
