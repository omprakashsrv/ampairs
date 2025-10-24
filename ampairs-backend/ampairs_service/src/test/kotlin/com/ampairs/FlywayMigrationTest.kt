package com.ampairs

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.Database
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlywayMigrationTest {

    companion object {
        @Container
        private val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")
            .withDatabaseName("ampairs_flyway")
            .withUsername("root")
            .withPassword("root")
    }

    private lateinit var flyway: Flyway
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeAll
    fun setUp() {
        val dataSource = DriverManagerDataSource().apply {
            setDriverClassName(mysql.driverClassName)
            url = "${mysql.jdbcUrl}?allowPublicKeyRetrieval=true&useSSL=false"
            username = mysql.username
            password = mysql.password
        }

        flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/mysql", "classpath:db/migration")
            .baselineOnMigrate(true)
            .cleanDisabled(false)
            .load()

        flyway.clean()
        flyway.migrate()

        jdbcTemplate = JdbcTemplate(dataSource)
    }

    @Test
    @DisplayName("All migrations execute without pending scripts")
    fun shouldSuccessfullyExecuteAllMigrations() {
        val appliedVersions = flyway.info().applied().mapNotNull { it.version?.canonical }

        // Module-scoped migrations V1.0.x and workspace retail modules V2.0.0
        assertThat(appliedVersions)
            .contains(
                "1.0.0",   // order module
                "1.0.1",   // auth module
                "1.0.2",   // core module
                "1.0.3",   // event module
                "1.0.4",   // notification module
                "1.0.5",   // workspace module
                "1.0.6",   // customer module
                "1.0.7",   // form module
                "1.0.8",   // product module
                "1.0.9",   // tax module
                "1.0.10",  // unit module
                "1.0.11",  // invoice module
                "1.0.12",  // business module
                "1.0.13",  // auth app_user table
                "1.0.14",  // business custom_attributes
                "2.0.0"    // workspace retail modules
            )
        assertThat(flyway.info().pending()).isEmpty()
    }

    @Test
    fun shouldCreateExpectedTables() {
        val tables = jdbcTemplate.queryForList(
            """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = ?
            """.trimIndent(),
            String::class.java,
            mysql.databaseName
        )

        assertThat(tables)
            .contains(
                "file",
                "unit",
                "unit_conversion",
                "customer",
                "customer_images",
                "product",
                "inventory",
                "customer_order",
                "invoice",
                "businesses",
                "attribute_definition",
                "field_config",
                "device_session",
                "login_session",
                "auth_token",
                "app_user",
                "workspaces",
                "workspace_members",
                "workspace_invitations",
                "workspace_teams",
                "workspace_modules",
                "workspace_settings",
                "workspace_activities",
                "master_modules",
                "notification_queue"
            )
    }

    @Test
    fun shouldDefineForeignKeysForUnitConversion() {
        val deleteRules = jdbcTemplate.queryForList(
            """
            SELECT rc.constraint_name, rc.delete_rule
            FROM information_schema.referential_constraints rc
            WHERE rc.constraint_schema = ?
              AND rc.table_name = 'unit_conversion'
            """.trimIndent(),
            Map::class.java,
            mysql.databaseName
        )

        assertThat(deleteRules)
            .anySatisfy { row ->
                assertThat(row["CONSTRAINT_NAME"]).isEqualTo("fk_unit_conversion_base")
                assertThat(row["DELETE_RULE"]).isEqualTo("CASCADE")
            }
            .anySatisfy { row ->
                assertThat(row["CONSTRAINT_NAME"]).isEqualTo("fk_unit_conversion_derived")
                assertThat(row["DELETE_RULE"]).isEqualTo("CASCADE")
            }
    }

    @Test
    fun shouldRegisterJsonColumns() {
        val jsonColumns = jdbcTemplate.queryForList(
            """
            SELECT table_name, column_name
            FROM information_schema.columns
            WHERE table_schema = ?
              AND data_type = 'json'
            """.trimIndent(),
            Map::class.java,
            mysql.databaseName
        )

        assertThat(jsonColumns.map { "${it["TABLE_NAME"]}.${it["COLUMN_NAME"]}" })
            .contains(
                "customer.billing_address",
                "customer.shipping_address",
                "customer.attributes",
                "businesses.custom_attributes",
                "order_item.tax_info",
                "order_item.attributes",
                "invoice.tax_info",
                "attribute_definition.validation_params"
            )
    }

    @Test
    fun shouldUseTimestampForAuditColumns() {
        val timestampColumns = jdbcTemplate.queryForList(
            """
            SELECT table_name, column_name
            FROM information_schema.columns
            WHERE table_schema = ?
              AND column_name IN ('created_at', 'updated_at')
              AND data_type = 'timestamp'
            """.trimIndent(),
            Map::class.java,
            mysql.databaseName
        ).map { "${it["TABLE_NAME"]}.${it["COLUMN_NAME"]}" }

        assertThat(timestampColumns)
            .contains(
                "customer.created_at",
                "customer.updated_at",
                "product.created_at",
                "product.updated_at",
                "inventory.created_at",
                "inventory.updated_at",
                "businesses.created_at",
                "businesses.updated_at"
            )
    }

    @Test
    fun shouldIndexForeignKeyColumns() {
        val indexes = jdbcTemplate.queryForList(
            """
            SELECT table_name, column_name, index_name
            FROM information_schema.statistics
            WHERE table_schema = ?
              AND table_name IN ('unit_conversion', 'inventory_unit_conversion')
            """.trimIndent(),
            Map::class.java,
            mysql.databaseName
        )

        val indexMap = indexes.groupBy { it["TABLE_NAME"] to it["COLUMN_NAME"] }
            .mapValues { (_, value) -> value.map { it["INDEX_NAME"] } }

        assertThat(indexMap["unit_conversion" to "base_unit_id"])
            .isNotNull
            .contains("idx_unit_conversion_base")

        assertThat(indexMap["unit_conversion" to "derived_unit_id"])
            .isNotNull
            .contains("idx_unit_conversion_derived")

        assertThat(indexMap["inventory_unit_conversion" to "inventory_id"])
            .isNotNull
            .contains("idx_inventory_unit_conv_inventory")
    }

    @Test
    fun shouldValidateJpaEntitiesAgainstSchema() {
        val vendorAdapter = HibernateJpaVendorAdapter().apply {
            setDatabase(Database.MYSQL)
            setGenerateDdl(false)
            setShowSql(false)
        }

        val factoryBean = LocalContainerEntityManagerFactoryBean().apply {
            dataSource = jdbcTemplate.dataSource
            setPackagesToScan("com.ampairs")
            jpaVendorAdapter = vendorAdapter
            setJpaPropertyMap(
                mapOf(
                    "hibernate.hbm2ddl.auto" to "validate",
                    "hibernate.physical_naming_strategy" to "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy",
                    "hibernate.dialect" to "org.hibernate.dialect.MySQLDialect"
                )
            )
            afterPropertiesSet()
        }

        factoryBean.nativeEntityManagerFactory.close()
    }
}
