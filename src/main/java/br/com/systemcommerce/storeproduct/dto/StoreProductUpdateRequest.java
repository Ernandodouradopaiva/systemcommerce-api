package br.com.systemcommerce.storeproduct.dto;

import br.com.systemcommerce.storeproduct.entity.StoreProduct;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StoreProductUpdateRequest(
        StoreProduct.StoreProductStatus status,
        Boolean allowsSale,
        Boolean allowsPosSale,
        Boolean allowsErpSale,
        @Size(max = 60) String localInternalCode,
        @Size(max = 60) String localBarcode,
        @DecimalMin(value = "0.00", inclusive = true) BigDecimal localDefaultPrice,
        @DecimalMin(value = "0.000", inclusive = true) BigDecimal localMinStock,
        @DecimalMin(value = "0.000", inclusive = true) BigDecimal localMaxStock,
        Boolean allowNegativeStock,
        @Size(max = 120) String physicalLocation,
        @Size(max = 40) String aisle,
        @Size(max = 40) String shelf,
        @Size(max = 80) String displayPosition,
        LocalDate commercializationStart,
        LocalDate commercializationEnd,
        @Size(max = 500) String blockReason) {}
