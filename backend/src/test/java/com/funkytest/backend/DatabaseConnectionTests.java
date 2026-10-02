package com.funkytest.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the configured DataSource reaches Postgres and that Flyway brought the
 * schema up to date on startup.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
// Seeding 10k products would dominate the run time and the assertions.
@TestPropertySource(properties = "app.fixtures.enabled=false")
class DatabaseConnectionTests {

	@Autowired
	private JdbcClient jdbcClient;

	@Test
	void connectsToPostgres() {
		String version = jdbcClient.sql("SELECT version()").query(String.class).single();
		assertThat(version).startsWith("PostgreSQL 18");
	}

	@Test
	void flywayCreatedTheApplicationSchema() {
		Long schemas = jdbcClient
			.sql("SELECT count(*) FROM pg_namespace WHERE nspname = 'funkytest'")
			.query(Long.class)
			.single();
		assertThat(schemas).isEqualTo(1);
	}

	@Test
	void flywayAppliedEveryMigration() {
		var applied = jdbcClient
			.sql("SELECT version FROM funkytest.flyway_schema_history WHERE success ORDER BY installed_rank")
			.query(String.class)
			.list();
		assertThat(applied).contains("1");
	}

	@Test
	void migrationInstalledTheExtensions() {
		var extensions = jdbcClient.sql("SELECT extname FROM pg_extension")
			.query(String.class)
			.list();
		assertThat(extensions).contains("pgcrypto", "pg_trgm");
	}

}
