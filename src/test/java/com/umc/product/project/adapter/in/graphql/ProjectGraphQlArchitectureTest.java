package com.umc.product.project.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectGraphQlArchitectureTest {

    private static final Path GRAPHQL_SOURCE_ROOT =
        Path.of("src/main/java/com/umc/product/project/adapter/in/graphql");

    @Test
    @DisplayName("Project GraphQL adapter는 REST web adapter DTO에 의존하지 않는다")
    void graphql_adapter_does_not_import_web_adapter() throws IOException {
        List<String> violations = findJavaFiles()
            .flatMap(ProjectGraphQlArchitectureTest::findWebAdapterImportViolations)
            .toList();

        assertThat(violations).isEmpty();
    }

    private static Stream<Path> findJavaFiles() throws IOException {
        return Files.walk(GRAPHQL_SOURCE_ROOT)
            .filter(path -> path.toString().endsWith(".java"));
    }

    private static Stream<String> findWebAdapterImportViolations(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            return IntStream.range(0, lines.size())
                .filter(index -> lines.get(index).startsWith("import "))
                .filter(index -> lines.get(index).contains(".adapter.in.web."))
                .mapToObj(index -> "%s:%d %s".formatted(path, index + 1, lines.get(index).trim()));
        } catch (IOException e) {
            throw new IllegalStateException("Project GraphQL adapter import 정책 테스트 파일 읽기 실패: " + path, e);
        }
    }
}
