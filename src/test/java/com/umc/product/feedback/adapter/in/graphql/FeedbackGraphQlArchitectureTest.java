package com.umc.product.feedback.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedbackGraphQlArchitectureTest {

    private static final Path GRAPHQL_SOURCE_ROOT =
        Path.of("src/main/java/com/umc/product/feedback/adapter/in/graphql");
    private static final Set<String> PROHIBITED_IMPORT_PATTERNS = Set.of(
        ".adapter.in.web.",
        ".project.adapter.in.graphql.",
        ".member.adapter.in.graphql.",
        ".form.adapter.in.graphql."
    );

    @Test
    @DisplayName("Feedback GraphQL adapter는 REST와 타 도메인 GraphQL adapter에 의존하지 않는다")
    void graphql_adapter_does_not_import_prohibited_adapters() throws IOException {
        List<String> violations = findProhibitedImportViolations();

        assertThat(violations).isEmpty();
    }

    private static List<String> findProhibitedImportViolations() throws IOException {
        try (Stream<Path> javaFiles = Files.walk(GRAPHQL_SOURCE_ROOT)) {
            return javaFiles
                .filter(path -> path.toString().endsWith(".java"))
                .flatMap(FeedbackGraphQlArchitectureTest::findProhibitedImportViolations)
                .toList();
        }
    }

    private static Stream<String> findProhibitedImportViolations(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            return IntStream.range(0, lines.size())
                .filter(index -> lines.get(index).trim().startsWith("import "))
                .filter(index -> PROHIBITED_IMPORT_PATTERNS.stream()
                    .anyMatch(lines.get(index)::contains))
                .mapToObj(index -> "%s:%d %s".formatted(path, index + 1, lines.get(index).trim()));
        } catch (IOException e) {
            throw new IllegalStateException("Feedback GraphQL adapter import 정책 테스트 파일 읽기 실패: " + path, e);
        }
    }
}
