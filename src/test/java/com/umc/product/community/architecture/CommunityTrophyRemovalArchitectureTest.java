package com.umc.product.community.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.certificate.domain.Certificate;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.maintenance.domain.MaintenanceDomain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@DisplayName("커뮤니티 Trophy 런타임 제거 아키텍처 계약")
class CommunityTrophyRemovalArchitectureTest {

    private static final Path COMMUNITY_SOURCE_ROOT = Path.of(
        "src/main/java/com/umc/product/community"
    );

    @Test
    @DisplayName("커뮤니티 Java 런타임에는 Trophy 전용 파일이 남아 있지 않다")
    void trophy_runtime_files_are_absent() throws IOException {
        // Given: 커뮤니티 Java 런타임 소스 경로
        // When: 파일명과 소스 내용을 기준으로 Trophy 전용 흔적을 찾는다
        try (var paths = Files.walk(COMMUNITY_SOURCE_ROOT)) {
            var javaFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .toList();

            // Then: 파일명과 파일 내용 어디에도 Trophy 전용 흔적이 없어야 한다
            assertThat(javaFiles.stream()
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).contains("trophy"))
                .toList())
                .as("커뮤니티 Trophy 전용 Java 파일")
                .isEmpty();
            assertThat(javaFiles.stream()
                .filter(path -> containsIgnoreCase(path, "trophy"))
                .toList())
                .as("커뮤니티 Java 소스의 Trophy 토큰")
                .isEmpty();
        }
    }

    @Test
    @DisplayName("Trophy 오류 코드는 제거하되 나머지 COMMUNITY 번호는 유지한다")
    void trophy_error_codes_are_removed_without_renumbering_remaining_codes() {
        // Given: 제거 후에도 외부 계약으로 남아 있는 CommunityErrorCode
        Map<String, String> codes = Map.ofEntries(
            Map.entry("POST_NOT_FOUND", "COMMUNITY-0001"),
            Map.entry("COMMENT_NOT_FOUND", "COMMUNITY-0002"),
            Map.entry("INVALID_POST_TITLE", "COMMUNITY-0004"),
            Map.entry("INVALID_COMMENT_CONTENT", "COMMUNITY-0010"),
            Map.entry("REPORT_ALREADY_EXISTS", "COMMUNITY-0016"),
            Map.entry("INVALID_ID", "COMMUNITY-0030"),
            Map.entry("POST_UPDATE_INVALID_CALL", "COMMUNITY-0032")
        );

        // When: 오류 코드 enum을 읽는다
        Map<String, String> actualCodes = java.util.Arrays.stream(CommunityErrorCode.values())
            .collect(java.util.stream.Collectors.toMap(
                Enum::name,
                CommunityErrorCode::getCode
            ));

        // Then: Trophy 상수와 제거 대상 오류 코드는 없고 기존 상수의 번호는 그대로다
        assertThat(actualCodes.keySet())
            .noneMatch(name -> name.toLowerCase(Locale.ROOT).contains("trophy"));
        assertThat(actualCodes.values())
            .doesNotContain(
                "COMMUNITY-0003",
                "COMMUNITY-0012",
                "COMMUNITY-0013",
                "COMMUNITY-0014",
                "COMMUNITY-0015"
            );
        assertThat(actualCodes).containsAllEntriesOf(codes);
    }

    @Test
    @DisplayName("Trophy URI는 COMMUNITY 점검 allow-path로 더 이상 분류되지 않는다")
    void trophy_uri_is_not_a_maintenance_domain_path() {
        // Given: Trophy API였던 URI와 보존해야 하는 게시글 URI
        // When: URI를 점검 도메인으로 분류한다
        // Then: Trophy URI는 미분류이고 게시글 URI는 COMMUNITY로 유지된다
        assertThat(MaintenanceDomain.fromUri("/api/v1/trophies/42")).isEmpty();
        assertThat(MaintenanceDomain.fromUri("/api/v1/posts/42"))
            .contains(MaintenanceDomain.COMMUNITY);
    }

    @Test
    @DisplayName("WeeklyBestWorkbook과 Certificate 런타임 타입은 보존된다")
    void weekly_best_workbook_and_certificate_runtime_are_preserved() {
        // Given/When: Trophy 런타임 제거와 무관한 기존 JPA 타입의 실계약을 읽는다
        // Then: 후속 Workbook/Certificate 구현의 엔티티와 테이블 경계는 살아 있어야 한다
        assertThat(WeeklyBestWorkbook.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(WeeklyBestWorkbook.class.getAnnotation(Table.class).name())
            .isEqualTo("weekly_best_workbook");
        assertThat(Certificate.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Certificate.class.getAnnotation(Table.class).name())
            .isEqualTo("certificate");
    }

    private static boolean containsIgnoreCase(Path path, String token) {
        try {
            return Files.readString(path).toLowerCase(Locale.ROOT)
                .contains(token.toLowerCase(Locale.ROOT));
        } catch (IOException exception) {
            throw new IllegalStateException("커뮤니티 Java 소스를 읽을 수 없습니다: " + path, exception);
        }
    }
}
