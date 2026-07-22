package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SharedGraphQlArchitectureTest {

    private static final Path GRAPHQL_SCHEMA_ROOT = Path.of("src/main/resources/graphql");
    private static final Pattern DECLARATION_PATTERN = Pattern.compile(
        "(?m)^\\s*(scalar|enum|interface|type)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b"
    );
    private static final Map<String, String> SHARED_DECLARATION_OWNERS = Map.ofEntries(
        Map.entry("scalar Long", "common.graphqls"),
        Map.entry("scalar Instant", "common.graphqls"),
        Map.entry("enum ChallengerPart", "common.graphqls"),
        Map.entry("enum ChallengerTrack", "common.graphqls"),
        Map.entry("interface Form", "form.graphqls"),
        Map.entry("interface FormSection", "form.graphqls"),
        Map.entry("type FormQuestion", "form.graphqls"),
        Map.entry("type FormOption", "form.graphqls"),
        Map.entry("enum FormStatus", "form.graphqls"),
        Map.entry("enum FormResponseStatus", "form.graphqls"),
        Map.entry("enum QuestionType", "form.graphqls"),
        Map.entry("type Member", "member.graphqls"),
        Map.entry("type Gisu", "organization.graphqls"),
        Map.entry("type Chapter", "organization.graphqls"),
        Map.entry("type School", "organization.graphqls")
    );

    @Test
    @DisplayName("공유 GraphQL 선언은 지정된 SDL 파일 하나만 소유한다")
    void shared_graphql_declarations_have_exactly_one_owner() throws IOException {
        Map<String, List<String>> declarationOwners = findDeclarationOwners();

        assertOwners(declarationOwners, SHARED_DECLARATION_OWNERS);
        assertThat(declarationOwners.entrySet().stream()
            .filter(entry -> entry.getValue().contains("project.graphqls"))
            .map(Map.Entry::getKey)
            .filter(SHARED_DECLARATION_OWNERS::containsKey)
            .toList())
            .isEmpty();
    }

    private static void assertOwners(Map<String, List<String>> actualOwners, Map<String, String> expectedOwners) {
        expectedOwners.forEach((declaration, expectedOwner) -> assertThat(actualOwners.get(declaration))
            .as("GraphQL 선언 '%s' 소유 파일", declaration)
            .containsExactly(expectedOwner));
    }

    private static Map<String, List<String>> findDeclarationOwners() throws IOException {
        Map<String, List<String>> owners = new TreeMap<>();
        for (Path schemaFile : schemaFiles()) {
            String source = Files.readString(schemaFile, StandardCharsets.UTF_8);
            Matcher matcher = DECLARATION_PATTERN.matcher(source);
            while (matcher.find()) {
                String declaration = "%s %s".formatted(matcher.group(1), matcher.group(2));
                owners.computeIfAbsent(declaration, ignored -> new java.util.ArrayList<>())
                    .add(schemaFile.getFileName().toString());
            }
        }
        return owners;
    }

    private static List<Path> schemaFiles() throws IOException {
        try (Stream<Path> schemaFiles = Files.list(GRAPHQL_SCHEMA_ROOT)) {
            return schemaFiles
                .filter(path -> path.getFileName().toString().endsWith(".graphqls"))
                .sorted()
                .toList();
        }
    }
}
