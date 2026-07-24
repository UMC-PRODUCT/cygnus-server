package com.umc.product.support;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {

    private static final DockerImageName POSTGIS_IMAGE = PostgisTestImageResolver.resolve();

    private static final PostgreSQLContainer<?> POSTGIS_CONTAINER = startPostgisContainer();

    private static final AtomicInteger DATABASE_SEQUENCE = new AtomicInteger();

    @Bean
    JdbcConnectionDetails postgisConnectionDetails() {
        String databaseName = "test_" + DATABASE_SEQUENCE.incrementAndGet();
        createDatabase(databaseName);
        String jdbcUrl = "jdbc:postgresql://%s:%d/%s?loggerLevel=OFF".formatted(
            POSTGIS_CONTAINER.getHost(),
            POSTGIS_CONTAINER.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            databaseName
        );
        return new JdbcConnectionDetails() {
            @Override
            public String getUsername() {
                return POSTGIS_CONTAINER.getUsername();
            }

            @Override
            public String getPassword() {
                return POSTGIS_CONTAINER.getPassword();
            }

            @Override
            public String getJdbcUrl() {
                return jdbcUrl;
            }
        };
    }

    // 각 테스트 데이터베이스에 PostGIS 확장을 생성한다.
    @Bean
    ApplicationRunner init(DataSource ds) {
        return args -> {
            try (var c = ds.getConnection(); var st = c.createStatement()) {
                st.execute("CREATE EXTENSION IF NOT EXISTS postgis");
            }
        };
    }

    private static PostgreSQLContainer<?> startPostgisContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(POSTGIS_IMAGE);
        container.start();
        return container;
    }

    private static void createDatabase(String databaseName) {
        try (
            var connection = DriverManager.getConnection(
                POSTGIS_CONTAINER.getJdbcUrl(),
                POSTGIS_CONTAINER.getUsername(),
                POSTGIS_CONTAINER.getPassword()
            );
            var statement = connection.createStatement()
        ) {
            statement.execute("CREATE DATABASE " + databaseName);
        } catch (SQLException e) {
            throw new IllegalStateException("테스트 데이터베이스를 생성하지 못했습니다: " + databaseName, e);
        }
    }
}
