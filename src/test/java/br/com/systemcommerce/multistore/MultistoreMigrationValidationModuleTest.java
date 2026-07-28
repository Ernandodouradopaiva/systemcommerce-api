package br.com.systemcommerce.multistore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Validação pós-Flyway do relatório V173 ({@code multistore_migration_report}).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MultistoreMigrationValidationModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_multistore_migration_validation_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allV173MigrationChecksShouldBeOk() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT check_name, check_status, detail
                FROM multistore_migration_report
                WHERE migration_version = 'V173'
                ORDER BY check_name
                """);

        assertThat(rows).isNotEmpty();
        assertThat(rows)
                .as("todas as checagens V173 devem estar OK: %s", rows)
                .allSatisfy(row -> assertThat(row.get("check_status")).isEqualTo("OK"));
    }

    @Test
    void noSalesWithoutStoreIdAndNoInventoryStoreMismatch() {
        Integer salesWithoutStore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales WHERE store_id IS NULL", Integer.class);
        assertThat(salesWithoutStore).isZero();

        Integer inventoryMismatch = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM inventory i
                JOIN warehouses w ON w.id = i.warehouse_id
                WHERE i.store_id IS DISTINCT FROM w.store_id
                """,
                Integer.class);
        assertThat(inventoryMismatch).isZero();

        Map<String, Object> salesCheck = jdbcTemplate.queryForMap(
                """
                SELECT check_status, detail
                FROM multistore_migration_report
                WHERE migration_version = 'V173' AND check_name = 'sales_without_store'
                """);
        assertThat(salesCheck.get("check_status")).isEqualTo("OK");

        Map<String, Object> inventoryCheck = jdbcTemplate.queryForMap(
                """
                SELECT check_status, detail
                FROM multistore_migration_report
                WHERE migration_version = 'V173' AND check_name = 'inventory_store_mismatch'
                """);
        assertThat(inventoryCheck.get("check_status")).isEqualTo("OK");
    }
}
