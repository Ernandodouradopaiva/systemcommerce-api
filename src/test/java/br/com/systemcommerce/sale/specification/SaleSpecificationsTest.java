package br.com.systemcommerce.sale.specification;



import static org.assertj.core.api.Assertions.assertThat;



import br.com.systemcommerce.sale.entity.Sale;

import java.time.Instant;

import java.util.List;

import java.util.UUID;

import org.junit.jupiter.api.Test;



class SaleSpecificationsTest {



    @Test

    void withFiltersAlwaysReturnsSpecification() {

        assertThat(SaleSpecifications.withFilters(null, null, null, null, null, null, null, null, null, null))

                .isNotNull();

        assertThat(SaleSpecifications.withFilters(

                        Sale.SaleStatus.CONFIRMED,

                        UUID.randomUUID(),

                        UUID.randomUUID(),

                        "V0001",

                        Instant.parse("2026-01-01T00:00:00Z"),

                        Instant.parse("2026-12-31T00:00:00Z"),

                        "cliente",

                        Sale.SaleChannel.POS,

                        UUID.randomUUID(),

                        List.of(UUID.randomUUID())))

                .isNotNull();

    }

}


