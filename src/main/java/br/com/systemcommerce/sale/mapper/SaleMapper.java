package br.com.systemcommerce.sale.mapper;



import br.com.systemcommerce.sale.dto.SaleItemResponse;

import br.com.systemcommerce.sale.dto.SaleResponse;

import br.com.systemcommerce.sale.dto.SaleSellerHistoryResponse;

import br.com.systemcommerce.sale.dto.SaleStatusHistoryResponse;

import br.com.systemcommerce.sale.entity.Sale;

import br.com.systemcommerce.sale.entity.SaleItem;

import br.com.systemcommerce.sale.entity.SaleSellerHistory;

import br.com.systemcommerce.sale.entity.SaleStatusHistory;

import java.math.BigDecimal;

import java.util.List;

import java.util.Map;

import java.util.UUID;

import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;



@Component

public class SaleMapper {



    public SaleResponse toResponse(Sale sale) {

        return toResponse(sale, sale.getItems(), Map.of());

    }



    public SaleResponse toResponse(Sale sale, List<SaleItem> items) {

        return toResponse(sale, items, Map.of());

    }



    public SaleResponse toResponse(Sale sale, List<SaleItem> items, Map<UUID, BigDecimal> availableStockByProduct) {

        boolean editable = sale.isEditable();

        boolean posDraft = sale.isPos() && sale.isDraft();

        String sellerCode = resolveSellerCode(sale);

        String sellerName = resolveSellerName(sale);

        return new SaleResponse(

                sale.getId(),

                sale.getSaleNumber(),

                sale.getOrganization() != null ? sale.getOrganization().getId() : null,

                sale.getCustomer() != null ? sale.getCustomer().getId() : null,

                sale.getCustomer() != null ? sale.getCustomer().getName() : null,

                sale.getSeller().getId(),

                sale.getSeller().getName(),

                sale.getSellerProfile() != null ? sale.getSellerProfile().getId() : null,

                sellerCode,

                sellerName,

                sale.getSellerCodeSnapshot(),

                sale.getSellerNameSnapshot(),

                sale.getPriceTable() != null ? sale.getPriceTable().getId() : null,

                sale.getSaleDate(),

                sale.getStatus(),

                sale.getChannel() != null ? sale.getChannel() : Sale.SaleChannel.ADMIN,

                sale.getStore() != null ? sale.getStore().getId() : null,

                sale.getStore() != null ? sale.getStore().getCode() : null,

                sale.getTerminal() != null ? sale.getTerminal().getId() : null,

                sale.getTerminal() != null ? sale.getTerminal().getCode() : null,

                sale.getCashSession() != null ? sale.getCashSession().getId() : null,

                sale.getWarehouse() != null ? sale.getWarehouse().getId() : null,

                sale.getWarehouse() != null ? sale.getWarehouse().getCode() : null,

                sale.getSubtotal(),

                sale.getDiscountAmount(),

                sale.getSurchargeAmount(),

                sale.getFreightAmount(),

                sale.getTotalAmount(),

                sale.getNotes(),

                items == null

                        ? List.of()

                        : items.stream()

                                .map(item -> toItemResponse(

                                        item, availableStockByProduct.get(item.getProduct().getId())))

                                .toList(),

                editable,

                editable,

                !sale.isCancelled(),

                sale.canReceivePayment(),

                posDraft,

                sale.getVersion(),

                sale.getCreatedAt(),

                sale.getUpdatedAt());

    }



    public SaleSellerHistoryResponse toSellerHistoryResponse(SaleSellerHistory history) {

        return new SaleSellerHistoryResponse(

                history.getId(),

                history.getPreviousSellerProfile() != null

                        ? history.getPreviousSellerProfile().getId()

                        : null,

                history.getNewSellerProfile() != null ? history.getNewSellerProfile().getId() : null,

                history.getPreviousSellerCode(),

                history.getNewSellerCode(),

                history.getPreviousSellerName(),

                history.getNewSellerName(),

                history.getChangedBy() != null ? history.getChangedBy().getId() : null,

                history.getChangedBy() != null ? history.getChangedBy().getName() : null,

                history.getReason(),

                history.getCreatedAt());

    }



    public SaleItemResponse toItemResponse(SaleItem item) {

        return toItemResponse(item, null);

    }



    public SaleItemResponse toItemResponse(SaleItem item, BigDecimal availableStock) {

        return new SaleItemResponse(

                item.getId(),

                item.getProduct().getId(),

                item.getProduct().getSku(),

                item.getProduct().getBarcode(),

                item.getProduct().getName(),

                item.getDescription(),

                item.getQuantity(),

                item.getUnitPrice(),

                item.getDiscountAmount(),

                item.getLineSubtotal(),

                item.getLineTotal(),

                availableStock,

                item.getPriceSource() != null ? item.getPriceSource() : SaleItem.PriceSource.CATALOG,

                item.getPriceTable() != null ? item.getPriceTable().getId() : null,

                item.getProductPrice() != null ? item.getProductPrice().getId() : null,

                item.getDiscountAuthorizedBy() != null ? item.getDiscountAuthorizedBy().getId() : null);

    }



    public SaleStatusHistoryResponse toHistoryResponse(SaleStatusHistory history) {

        return new SaleStatusHistoryResponse(

                history.getId(),

                history.getFromStatus(),

                history.getToStatus(),

                history.getReason(),

                history.getChangedAt(),

                history.getChangedBy() != null ? history.getChangedBy().getId() : null,

                history.getChangedBy() != null ? history.getChangedBy().getName() : null);

    }



    private static String resolveSellerCode(Sale sale) {

        if (StringUtils.hasText(sale.getSellerCodeSnapshot())) {

            return sale.getSellerCodeSnapshot();

        }

        return sale.getSellerProfile() != null ? sale.getSellerProfile().getSellerCode() : null;

    }



    private static String resolveSellerName(Sale sale) {

        if (StringUtils.hasText(sale.getSellerNameSnapshot())) {

            return sale.getSellerNameSnapshot();

        }

        if (sale.getSellerProfile() != null && sale.getSellerProfile().getEmployee() != null) {

            return sale.getSellerProfile().getEmployee().getName();

        }

        return null;

    }

}


