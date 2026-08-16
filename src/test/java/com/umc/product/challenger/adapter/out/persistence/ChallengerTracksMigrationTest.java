package com.umc.product.challenger.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DisplayName("Challenger tracks Flyway 마이그레이션")
class ChallengerTracksMigrationTest {

    private static final String MIGRATION_PATH =
        "db/migration/V2026.07.12.00.01__add_challenger_tracks.sql";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @BeforeEach
    void setUpChallengerSchema() throws Exception {
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS public.challenger");
            statement.execute("""
                CREATE TABLE public.challenger (
                    id BIGSERIAL PRIMARY KEY,
                    member_id BIGINT NOT NULL,
                    gisu_id BIGINT NOT NULL,
                    part VARCHAR(255) NOT NULL,
                    status VARCHAR(255) NOT NULL,
                    CONSTRAINT uk_challenger_member_id_gisu_id UNIQUE (member_id, gisu_id)
                )
                """);
            statement.executeUpdate("""
                INSERT INTO public.challenger (member_id, gisu_id, part, status)
                VALUES
                    (1, 9, 'SPRINGBOOT', 'ACTIVE'),
                    (2, 9, 'ADMIN', 'ACTIVE')
                """);
        }
    }

    @Test
    @DisplayName("tracks를 빈 배열로 추가하고 기존 part와 member-gisu 유일성을 보존한다")
    void tracks를_빈_배열로_추가하고_기존_part와_member_gisu_유일성을_보존한다() throws Exception {
        executeMigration();

        try (Connection connection = POSTGRES.createConnection("")) {
            assertMigratedRows(connection);
            assertSchemaConstraints(connection);
        }
    }

    private void executeMigration() throws Exception {
        String sql = new ClassPathResource(MIGRATION_PATH).getContentAsString(StandardCharsets.UTF_8);
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void assertMigratedRows(Connection connection) throws SQLException {
        try (
            var statement = connection.createStatement();
            ResultSet rows = statement.executeQuery("""
                SELECT member_id, part, tracks
                FROM public.challenger
                ORDER BY member_id
                """)
        ) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong("member_id")).isEqualTo(1L);
            assertThat(rows.getString("part")).isEqualTo("SPRINGBOOT");
            assertThat((String[]) rows.getArray("tracks").getArray()).isEmpty();

            assertThat(rows.next()).isTrue();
            assertThat(rows.getString("part")).isEqualTo("ADMIN");
            assertThat((String[]) rows.getArray("tracks").getArray()).isEmpty();
            assertThat(rows.next()).isFalse();
        }
    }

    private void assertSchemaConstraints(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            try (ResultSet column = statement.executeQuery("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'challenger'
                  AND column_name = 'tracks'
                """)) {
                assertThat(column.next()).isTrue();
                assertThat(column.getString("is_nullable")).isEqualTo("NO");
            }

            try (ResultSet legacyColumn = statement.executeQuery("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'challenger'
                  AND column_name = 'track'
                """)) {
                assertThat(legacyColumn.next()).isTrue();
                assertThat(legacyColumn.getInt(1)).isZero();
            }

            try (ResultSet partColumn = statement.executeQuery("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'challenger'
                  AND column_name = 'part'
                """)) {
                assertThat(partColumn.next()).isTrue();
                assertThat(partColumn.getString("is_nullable")).isEqualTo("YES");
            }

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO public.challenger (member_id, gisu_id, part, status, tracks)
                VALUES (1, 9, 'PLAN', 'ACTIVE', ARRAY['PLAN']::TEXT[])
                """))
                .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                UPDATE public.challenger
                SET tracks = ARRAY['UNKNOWN']::TEXT[]
                WHERE member_id = 2
                """))
                .isInstanceOf(SQLException.class);

            assertThat(statement.executeUpdate("""
                INSERT INTO public.challenger (member_id, gisu_id, part, status, tracks)
                VALUES (4, 9, NULL, 'ACTIVE', ARRAY[]::TEXT[])
                """))
                .isEqualTo(1);

            assertThat(statement.executeUpdate("""
                INSERT INTO public.challenger (member_id, gisu_id, part, status, tracks)
                VALUES (5, 9, NULL, 'ACTIVE', ARRAY['INFRA_PLUS', 'WEB_PRODUCT_ENGINEER']::TEXT[])
                """))
                .isEqualTo(1);

            try (ResultSet tracks = statement.executeQuery("""
                SELECT tracks FROM public.challenger WHERE member_id = 5
                """)) {
                assertThat(tracks.next()).isTrue();
                assertThat((String[]) tracks.getArray("tracks").getArray())
                    .containsExactly("INFRA_PLUS", "WEB_PRODUCT_ENGINEER");
            }

            assertThatThrownBy(() -> statement.executeUpdate("""
                UPDATE public.challenger SET tracks = NULL WHERE member_id = 2
                """))
                .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                UPDATE public.challenger
                SET tracks = ARRAY['PLAN', NULL]::TEXT[]
                WHERE member_id = 2
                """))
                .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                UPDATE public.challenger
                SET tracks = ARRAY['DESIGN', 'DESIGN']::TEXT[]
                WHERE member_id = 2
                """))
                .isInstanceOf(SQLException.class);
        }
    }
}
