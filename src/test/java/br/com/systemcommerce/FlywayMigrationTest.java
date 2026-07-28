package br.com.systemcommerce;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {

    private static final String ADMIN_PASSWORD = "Admin@IntegrationTest1";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_migration_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void adminPassword(DynamicPropertyRegistry registry) {
        registry.add("app.seed.admin.password", () -> ADMIN_PASSWORD);
        registry.add("app.seed.admin.enabled", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateAllDomainTablesViaFlyway() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """,
                String.class);

        assertThat(tables)
                .contains(
                        "users",
                        "roles",
                        "permissions",
                        "user_roles",
                        "role_permissions",
                        "customers",
                        "categories",
                        "products",
                        "inventory",
                        "stock_movements",
                        "inventory_adjustment_reasons",
                        "sales",
                        "sale_items",
                        "payments",
                        "payment_status_history",
                        "sale_status_history",
                        "audit_logs",
                        "refresh_tokens",
                        "password_reset_tokens",
                        "flyway_schema_history");
    }

    @Test
    void shouldApplySeedsForPermissionsRolesCategoriesProductsCustomersAndInventory() {
        Integer permissions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class);
        Integer adminRoles =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles WHERE code = 'ADMIN'", Integer.class);
        Integer allRoles = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class);
        Integer rolePermissions =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM role_permissions", Integer.class);
        Integer adminRolePermissions = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id
                WHERE r.code = 'ADMIN'
                """,
                Integer.class);
        Integer categories = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class);
        Integer products = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        Integer customers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class);
        Integer inventory = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory", Integer.class);
        Integer movements = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_movements", Integer.class);
        Integer adjustmentReasons =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_adjustment_reasons", Integer.class);

        assertThat(permissions).isGreaterThanOrEqualTo(27);
        assertThat(adminRoles).isEqualTo(1);
        assertThat(allRoles).isGreaterThanOrEqualTo(4);
        assertThat(adminRolePermissions).isEqualTo(permissions);
        assertThat(rolePermissions).isGreaterThan(adminRolePermissions);
        assertThat(categories).isGreaterThanOrEqualTo(3);
        assertThat(products).isEqualTo(4);
        assertThat(customers).isGreaterThanOrEqualTo(3);
        assertThat(inventory).isGreaterThanOrEqualTo(4);
        assertThat(movements).isGreaterThanOrEqualTo(4);
        assertThat(adjustmentReasons).isGreaterThanOrEqualTo(5);

        Integer namedRoles = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM roles
                WHERE code IN ('ADMIN', 'MANAGER', 'SELLER', 'STOCK_KEEPER')
                """,
                Integer.class);
        assertThat(namedRoles).isEqualTo(4);
    }

    @Test
    void shouldSeedAdminUserWithBcryptPasswordOnly() {
        User admin = userRepository
                .findByEmailIgnoreCase("admin@systemcommerce.local")
                .orElseThrow();

        assertThat(admin.getPasswordHash()).isNotBlank();
        assertThat(admin.getPasswordHash()).isNotEqualTo(ADMIN_PASSWORD);
        assertThat(admin.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash())).isTrue();
        assertThat(admin.getLogin()).isEqualTo("admin");
        assertThat(admin.getStatus()).isEqualTo(User.UserStatus.ACTIVE);

        Integer adminRoles = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_roles ur
                JOIN users u ON u.id = ur.user_id
                JOIN roles r ON r.id = ur.role_id
                WHERE LOWER(u.email) = LOWER(?)
                  AND r.code = 'ADMIN'
                """,
                Integer.class,
                "admin@systemcommerce.local");

        assertThat(adminRoles).isEqualTo(1);
    }

    @Test
    void shouldHavePrimaryForeignUniqueAndCheckConstraints() {
        Integer primaryKeys = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public' AND constraint_type = 'PRIMARY KEY'
                """,
                Integer.class);
        Integer foreignKeys = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public' AND constraint_type = 'FOREIGN KEY'
                """,
                Integer.class);
        Integer uniqueConstraints = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public' AND constraint_type = 'UNIQUE'
                """,
                Integer.class);
        Integer checkConstraints = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public' AND constraint_type = 'CHECK'
                """,
                Integer.class);

        assertThat(primaryKeys).isGreaterThanOrEqualTo(15);
        assertThat(foreignKeys).isGreaterThanOrEqualTo(10);
        assertThat(uniqueConstraints).isGreaterThanOrEqualTo(5);
        assertThat(checkConstraints).isGreaterThanOrEqualTo(10);
    }

    @Test
    void shouldCreateIndexesForFrequentQueries() {
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname IN (
                    'idx_users_email',
                    'idx_products_sku',
                    'idx_customers_document',
                    'idx_sales_status',
                    'idx_stock_movements_product_id',
                    'idx_inventory_product_id'
                  )
                """);

        assertThat(indexes).hasSize(6);
    }

    @Test
    void flywayHistoryShouldContainSchemaAndSeedMigrations() {
        Integer versioned = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE AND version IS NOT NULL
                """,
                Integer.class);

        assertThat(versioned).isGreaterThanOrEqualTo(20);
    }
}
