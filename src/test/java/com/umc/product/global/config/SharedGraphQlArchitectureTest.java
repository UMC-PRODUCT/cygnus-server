package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        "(?m)^\\s*(scalar|enum|input|interface|type)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b"
    );
    private static final Map<String, String> PROVIDER_DECLARATION_OWNERS = Map.ofEntries(
        Map.entry("scalar Long", "shared/scalars.graphqls"),
        Map.entry("scalar Instant", "shared/scalars.graphqls"),
        Map.entry("input PageInput", "shared/pagination.graphqls"),
        Map.entry("type PageInfo", "shared/pagination.graphqls"),
        Map.entry("enum ChallengerPart", "challenger/output.graphqls"),
        Map.entry("enum ChallengerTrack", "challenger/output.graphqls"),
        Map.entry("enum ChallengerStatus", "challenger/output.graphqls"),
        Map.entry("type Form", "form/output.graphqls"),
        Map.entry("type FormSection", "form/output.graphqls"),
        Map.entry("type FormQuestion", "form/output.graphqls"),
        Map.entry("type FormOption", "form/output.graphqls"),
        Map.entry("enum FormStatus", "form/output.graphqls"),
        Map.entry("enum FormResponseStatus", "form/output.graphqls"),
        Map.entry("enum QuestionType", "form/output.graphqls"),
        Map.entry("type MemberPublic", "member/response.graphqls"),
        Map.entry("type MemberPrivate", "member/response.graphqls"),
        Map.entry("type Gisu", "organization/response.graphqls"),
        Map.entry("type Chapter", "organization/response.graphqls"),
        Map.entry("type School", "organization/response.graphqls")
    );

    @Test
    @DisplayName("provider GraphQL 선언은 지정된 도메인 IDL 하나만 소유한다")
    void providerGraphQlDeclarationsHaveExactlyOneOwner() throws IOException {
        Map<String, List<String>> declarationOwners = findDeclarationOwners();

        PROVIDER_DECLARATION_OWNERS.forEach((declaration, expectedOwner) ->
            assertThat(declarationOwners.get(declaration))
                .as("GraphQL 선언 '%s' 소유 파일", declaration)
                .containsExactly(expectedOwner));
    }

    @Test
    @DisplayName("도메인 request IDL과 response IDL은 선언 역할을 분리한다")
    void domainRequestAndResponseIdlsHaveSeparateRoles() throws IOException {
        for (Path schemaFile : schemaFiles()) {
            String relativePath = relativePath(schemaFile);
            String source = Files.readString(schemaFile, StandardCharsets.UTF_8);

            if (relativePath.endsWith("/request.graphqls")) {
                assertThat(source)
                    .as("request IDL은 concrete output type을 선언하지 않는다: %s", relativePath)
                    .doesNotContainPattern("(?m)^type\\s+(?!Query|Mutation)[A-Za-z_]");
            }
            if (relativePath.endsWith("/response.graphqls") || relativePath.endsWith("/output.graphqls")) {
                assertThat(source)
                    .as("response/output IDL은 input을 선언하지 않는다: %s", relativePath)
                    .doesNotContainPattern("(?m)^input\\s+[A-Za-z_]")
                    .doesNotContain("extend type Query", "extend type Mutation");
            }
        }
    }

    @Test
    @DisplayName("모든 GraphQL 도메인 디렉터리는 관계를 설명하는 README를 제공한다")
    void everyGraphQlDomainDirectoryHasReadme() throws IOException {
        List<String> missingReadmes = new ArrayList<>();

        try (Stream<Path> paths = Files.list(GRAPHQL_SCHEMA_ROOT)) {
            paths.filter(Files::isDirectory)
                .filter(path -> containsGraphQlSchema(path))
                .filter(path -> Files.notExists(path.resolve("README.md")))
                .map(path -> path.getFileName().toString())
                .forEach(missingReadmes::add);
        }

        assertThat(missingReadmes).isEmpty();
    }

    @Test
    @DisplayName("Project와 Recruiting Form projection은 provider Form을 재선언하거나 구현하지 않는다")
    void consumerFormProjectionsRemainDomainOwned() throws IOException {
        String projectSource = Files.readString(
            GRAPHQL_SCHEMA_ROOT.resolve("project/response.graphqls"), StandardCharsets.UTF_8);
        String recruitingSource = Files.readString(
            GRAPHQL_SCHEMA_ROOT.resolve("recruiting/response.graphqls"), StandardCharsets.UTF_8);

        assertThat(projectSource)
            .contains("type ProjectApplicationForm", "type ProjectApplicationFormSection")
            .doesNotContain("implements Form", "type Form {");
        assertThat(recruitingSource)
            .contains("type RecruitingApplicationFormStructure", "type RecruitingFormSection")
            .doesNotContain("implements Form", "type Form {");
    }

    private static Map<String, List<String>> findDeclarationOwners() throws IOException {
        Map<String, List<String>> owners = new TreeMap<>();
        for (Path schemaFile : schemaFiles()) {
            String source = Files.readString(schemaFile, StandardCharsets.UTF_8);
            Matcher matcher = DECLARATION_PATTERN.matcher(source);
            while (matcher.find()) {
                String declaration = "%s %s".formatted(matcher.group(1), matcher.group(2));
                owners.computeIfAbsent(declaration, ignored -> new ArrayList<>())
                    .add(relativePath(schemaFile));
            }
        }
        return owners;
    }

    private static List<Path> schemaFiles() throws IOException {
        try (Stream<Path> schemaFiles = Files.walk(GRAPHQL_SCHEMA_ROOT)) {
            return schemaFiles
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".graphqls"))
                .sorted()
                .toList();
        }
    }

    private static boolean containsGraphQlSchema(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.anyMatch(path -> path.getFileName().toString().endsWith(".graphqls"));
        } catch (IOException exception) {
            throw new IllegalStateException("GraphQL 도메인 디렉터리를 읽을 수 없습니다: " + directory, exception);
        }
    }

    private static String relativePath(Path schemaFile) {
        return GRAPHQL_SCHEMA_ROOT.relativize(schemaFile).toString().replace('\\', '/');
    }
}
