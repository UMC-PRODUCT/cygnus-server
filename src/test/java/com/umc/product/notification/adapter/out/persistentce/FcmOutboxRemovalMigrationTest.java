package com.umc.product.notification.adapter.out.persistentce;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DisplayName("FCM outbox 제거 Flyway 마이그레이션")
class FcmOutboxRemovalMigrationTest {

    private static final String MIGRATION_PATH =
        "db/migration/V2026.07.23.03.00__drop_fcm_outbox.sql";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @BeforeEach
    void setUpLegacyTable() throws Exception {
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS public.fcm_outbox");
            statement.execute("CREATE TABLE public.fcm_outbox (id BIGSERIAL PRIMARY KEY)");
            statement.executeUpdate("INSERT INTO public.fcm_outbox DEFAULT VALUES");
        }
    }

    @Test
    @DisplayName("legacy FCM outbox 테이블을 삭제하고 재실행해도 성공한다")
    void removesLegacyFcmOutboxTable() throws Exception {
        executeMigration();
        executeMigration();

        try (
            Connection connection = POSTGRES.createConnection("");
            var statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT to_regclass('public.fcm_outbox')")
        ) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isNull();
        }
    }

    private void executeMigration() throws Exception {
        String sql = new ClassPathResource(MIGRATION_PATH).getContentAsString(StandardCharsets.UTF_8);
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
