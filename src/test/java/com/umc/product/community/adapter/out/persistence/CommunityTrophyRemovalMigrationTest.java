package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@DisplayName("커뮤니티 Trophy 제거 Flyway 마이그레이션")
class CommunityTrophyRemovalMigrationTest {

    private static final String PRE_DROP_VERSION = "2026.07.14.01.00";
    private static final long SENTINEL_ID = 9_000_001L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres")
    );

    @Test
    @DisplayName("fresh 설치에서 Trophy 테이블과 시퀀스를 제거한다")
    void freshInstallRemovesTrophyTableAndSequence() throws SQLException {
        Database database = createDatabase();

        try {
            migrate(database);

            try (Connection connection = database.openConnection()) {
                assertTrophyArtifactsAbsent(connection);
            }
        } finally {
            database.close();
        }
    }

    @Test
    @DisplayName("제거 전 스키마 업그레이드에서 Trophy 테이블과 sentinel 행을 제거한다")
    void upgradeFromPreDropSchemaRemovesTrophyTableSequenceAndSentinelRow() throws SQLException {
        Database database = createDatabase();

        try {
            migrateToPreDropVersion(database);

            try (Connection connection = database.openConnection()) {
                assertThat(relationExists(connection, "trophy")).isTrue();
                assertThat(relationExists(connection, "trophy_id_seq")).isTrue();
                assertThat(countInboundForeignKeys(connection))
                    .as("public.trophy inbound foreign keys")
                    .isZero();
                insertSentinel(connection);
                assertThat(countSentinelRows(connection)).isEqualTo(1);
            }

            MigrateResult migrationResult = migrate(database);
            assertThat(migrationResult.migrationsExecuted)
                .as("pre-drop to latest migrations executed")
                .isPositive();

            try (Connection connection = database.openConnection()) {
                assertTrophyArtifactsAbsent(connection);
            }
        } finally {
            database.close();
        }
    }

    private static MigrateResult migrate(Database database) {
        return Flyway.configure()
            .dataSource(database.jdbcUrl(), database.username(), database.password())
            .locations("classpath:db/migration")
            .load()
            .migrate();
    }

    private static void migrateToPreDropVersion(Database database) {
        Flyway.configure()
            .dataSource(database.jdbcUrl(), database.username(), database.password())
            .locations("classpath:db/migration")
            .target(PRE_DROP_VERSION)
            .load()
            .migrate();
    }

    private static Database createDatabase() throws SQLException {
        String databaseName = "migration_" + UUID.randomUUID().toString().replace("-", "");
        try (
            Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
            );
            Statement statement = connection.createStatement()
        ) {
            statement.execute("CREATE DATABASE " + databaseName);
        }
        String containerJdbcUrl = POSTGRES.getJdbcUrl();
        String jdbcUrl = containerJdbcUrl.substring(0, containerJdbcUrl.lastIndexOf('/') + 1) + databaseName;
        return new Database(jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword(), databaseName);
    }

    private static void insertSentinel(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO public.trophy "
                + "(id, challenger_id, week, title, content, url, created_at, updated_at) "
                + "VALUES (?, 1, 1, 'migration sentinel', 'migration sentinel', 'https://example.com', now(), now())"
        )) {
            statement.setLong(1, SENTINEL_ID);
            statement.executeUpdate();
        }
    }

    private static long countSentinelRows(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT COUNT(*) FROM public.trophy WHERE id = ?"
        )) {
            statement.setLong(1, SENTINEL_ID);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static long countInboundForeignKeys(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT COUNT(*) "
                + "FROM pg_constraint constraint_definition "
                + "JOIN pg_class referenced_table "
                + "ON referenced_table.oid = constraint_definition.confrelid "
                + "JOIN pg_namespace referenced_schema "
                + "ON referenced_schema.oid = referenced_table.relnamespace "
                + "WHERE constraint_definition.contype = 'f' "
                + "AND referenced_schema.nspname = 'public' "
                + "AND referenced_table.relname = 'trophy'"
        ); ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static void assertTrophyArtifactsAbsent(Connection connection) throws SQLException {
        assertThat(relationExists(connection, "trophy")).as("public.trophy").isFalse();
        assertThat(relationExists(connection, "trophy_id_seq")).as("public.trophy_id_seq").isFalse();
    }

    private static boolean relationExists(Connection connection, String relationName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT to_regclass(?) IS NOT NULL"
        )) {
            statement.setString(1, "public." + relationName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    private record Database(String jdbcUrl, String username, String password, String databaseName) {

        private Connection openConnection() throws SQLException {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }

        private void close() throws SQLException {
            try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
            ); Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + databaseName + " WITH (FORCE)");
            }
        }
    }
}
