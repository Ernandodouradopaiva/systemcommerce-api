package br.com.systemcommerce.carrier.mapper;

import br.com.systemcommerce.carrier.dto.FreightModeResponse;
import br.com.systemcommerce.carrier.dto.FreightQuotationResponse;
import br.com.systemcommerce.carrier.dto.FreightRegionResponse;
import br.com.systemcommerce.carrier.dto.FreightTableResponse;
import br.com.systemcommerce.carrier.entity.FreightMode;
import br.com.systemcommerce.carrier.entity.FreightQuotation;
import br.com.systemcommerce.carrier.entity.FreightRegion;
import br.com.systemcommerce.carrier.entity.FreightTable;
import org.springframework.stereotype.Component;

@Component
public class FreightMapper {

    public FreightModeResponse toResponse(FreightMode mode) {
        return new FreightModeResponse(
                mode.getId(),
                mode.getOrganization() != null ? mode.getOrganization().getId() : null,
                mode.getCode(),
                mode.getName(),
                mode.getModeType(),
                mode.getStatus(),
                mode.isUsable());
    }

    public FreightTableResponse toResponse(FreightTable table) {
        return new FreightTableResponse(
                table.getId(),
                table.getOrganization() != null ? table.getOrganization().getId() : null,
                table.getCarrier() != null ? table.getCarrier().getId() : null,
                table.getCarrier() != null ? table.getCarrier().getLegalName() : null,
                table.getFreightMode() != null ? table.getFreightMode().getId() : null,
                table.getFreightMode() != null ? table.getFreightMode().getName() : null,
                table.getName(),
                table.getStatus(),
                table.getValidFrom(),
                table.getValidUntil(),
                table.getRegions().stream().map(this::toRegionResponse).toList());
    }

    public FreightRegionResponse toRegionResponse(FreightRegion region) {
        return new FreightRegionResponse(
                region.getId(),
                region.getRegionCode(),
                region.getRegionName(),
                region.getZipFrom(),
                region.getZipTo(),
                region.getMinWeight(),
                region.getMaxWeight(),
                region.getMinVolume(),
                region.getMaxVolume(),
                region.getMinOrderAmount(),
                region.getFreightAmount(),
                region.getLeadTimeDays());
    }

    public FreightQuotationResponse toResponse(FreightQuotation quotation) {
        return new FreightQuotationResponse(
                quotation.getId(),
                quotation.getOrganizationId(),
                quotation.getStoreId(),
                quotation.getCarrier() != null ? quotation.getCarrier().getId() : null,
                quotation.getCarrier() != null ? quotation.getCarrier().getLegalName() : null,
                quotation.getFreightMode() != null ? quotation.getFreightMode().getId() : null,
                quotation.getFreightMode() != null ? quotation.getFreightMode().getName() : null,
                quotation.getSalesOrderId(),
                quotation.getQuoteId(),
                quotation.getZipCode(),
                quotation.getWeight(),
                quotation.getVolume(),
                quotation.getOrderAmount(),
                quotation.getCalculatedAmount(),
                Boolean.TRUE.equals(quotation.getManualOverride()),
                quotation.getOverrideAmount(),
                quotation.getSource(),
                quotation.getCalculatedAt(),
                quotation.getCalculatedBy(),
                quotation.getNotes());
    }
}
