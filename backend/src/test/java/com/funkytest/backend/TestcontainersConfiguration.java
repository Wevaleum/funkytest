package com.funkytest.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts a throwaway Postgres for tests that need one, so {@code ./mvnw test}
 * never depends on {@code docker compose up} having been run. {@code @ServiceConnection}
 * overrides the spring.datasource.* values from application.yml with the
 * container's own, so no test ever touches the local development database.
 *
 * <p>The container needs no init script: Flyway creates the schema and runs
 * db/migration against it, exactly as it does in development.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/** Same image as docker/postgres/Dockerfile builds from. */
	private static final String POSTGRES_IMAGE = "postgres:18-alpine";

	@Bean
	@ServiceConnection
	public PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(POSTGRES_IMAGE);
	}

}
