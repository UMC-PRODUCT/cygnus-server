package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecruitingGraphQlArchitectureTest {

    private static final Path GRAPHQL_SOURCE_ROOT =
        Path.of("src/main/java/com/umc/product/recruiting/adapter/in/graphql");

    @Test
    @DisplayName("Recruiting GraphQL adapter 패키지가 존재한다")
    void Recruiting_GraphQL_adapter_패키지가_존재한다() {
        assertThat(GRAPHQL_SOURCE_ROOT)
            .isDirectory();
    }

    @Test
    @DisplayName("Recruiting GraphQL adapter는 REST web adapter DTO에 의존하지 않는다")
    void Recruiting_GraphQL_adapter는_REST_web_adapter_DTO에_의존하지_않는다() throws IOException {
        List<String> violations = findJavaFiles()
            .flatMap(RecruitingGraphQlArchitectureTest::findWebAdapterImportViolations)
            .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Recruiting GraphQL adapter는 domain entity를 노출하지 않는다")
    void Recruiting_GraphQL_adapter는_domain_entity를_노출하지_않는다() throws IOException {
        List<String> violations = findJavaFiles()
            .flatMap(RecruitingGraphQlArchitectureTest::findDomainEntityImportViolations)
            .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Recruiting GraphQL adapter는 outbound port DTO에 의존하지 않는다")
    void Recruiting_GraphQL_adapter는_outbound_port_DTO에_의존하지_않는다() throws IOException {
        List<String> violations = findJavaFiles()
            .flatMap(RecruitingGraphQlArchitectureTest::findOutboundPortImportViolations)
            .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Recruiting GraphQL adapter는 persistence adapter에 의존하지 않는다")
    void Recruiting_GraphQL_adapter는_persistence_adapter에_의존하지_않는다() throws IOException {
        List<String> violations = findJavaFiles()
            .flatMap(RecruitingGraphQlArchitectureTest::findPersistenceAdapterImportViolations)
            .toList();

        assertThat(violations).isEmpty();
    }

    private static Stream<Path> findJavaFiles() throws IOException {
        if (!Files.isDirectory(GRAPHQL_SOURCE_ROOT)) {
            return Stream.empty();
        }
        return Files.walk(GRAPHQL_SOURCE_ROOT)
            .filter(path -> path.toString().endsWith(".java"));
    }

    private static Stream<String> findWebAdapterImportViolations(Path path) {
        return findImportViolations(path, ".adapter.in.web.");
    }

    private static Stream<String> findDomainEntityImportViolations(Path path) {
        return findImportViolations(path, "com.umc.product.recruiting.domain.Recruiting");
    }

    private static Stream<String> findOutboundPortImportViolations(Path path) {
        return findImportViolations(path, "com.umc.product.recruiting.application.port.out.");
    }

    private static Stream<String> findPersistenceAdapterImportViolations(Path path) {
        return Stream.concat(
            findImportViolations(path, ".adapter.out."),
            findImportViolations(path, "JpaRepository")
        );
    }

    private static Stream<String> findImportViolations(Path path, String forbiddenImport) {
        try {
            List<String> lines = Files.readAllLines(path);
            return IntStream.range(0, lines.size())
                .filter(index -> lines.get(index).startsWith("import "))
                .filter(index -> lines.get(index).contains(forbiddenImport))
                .mapToObj(index -> "%s:%d %s".formatted(path, index + 1, lines.get(index).trim()));
        } catch (IOException e) {
            throw new IllegalStateException("Recruiting GraphQL adapter 정책 테스트 파일 읽기 실패: " + path, e);
        }
    }
}
