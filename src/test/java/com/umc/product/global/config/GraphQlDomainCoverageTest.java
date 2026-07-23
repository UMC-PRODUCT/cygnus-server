package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GraphQlDomainCoverageTest {

    private static final Path JAVA_DOMAIN_ROOT = Path.of("src/main/java/com/umc/product");
    private static final Path GRAPHQL_ROOT = Path.of("src/main/resources/graphql");

    private static final Set<String> GRAPHQL_DOMAINS = Set.of(
        "analytics",
        "audit",
        "authentication",
        "authorization",
        "blog",
        "certificate",
        "challenger",
        "chat",
        "community",
        "curriculum",
        "documentation",
        "feedback",
        "form",
        "maintenance",
        "member",
        "notice",
        "notification",
        "organization",
        "project",
        "recruiting",
        "schedule",
        "storage",
        "term"
    );

    private static final Set<String> NON_API_OR_INTERNAL_DOMAINS = Set.of(
        "common",
        "figma",
        "global",
        "llm",
        "survey",
        "test"
    );

    private static final Map<String, String> REPRESENTATIVE_ROOT_FIELDS = Map.ofEntries(
        Map.entry("analytics", "adminAnalytics"),
        Map.entry("audit", "auditLogs"),
        Map.entry("authentication", "myOAuthConnections"),
        Map.entry("authorization", "resourcePermissions"),
        Map.entry("blog", "blogContents"),
        Map.entry("certificate", "myCertificates"),
        Map.entry("challenger", "challengers"),
        Map.entry("chat", "chatRooms"),
        Map.entry("community", "communityPosts"),
        Map.entry("curriculum", "curriculum"),
        Map.entry("documentation", "errorCodeCatalog"),
        Map.entry("feedback", "feedbackTemplate"),
        Map.entry("form", "form"),
        Map.entry("maintenance", "maintenanceStatus"),
        Map.entry("member", "memberSearch"),
        Map.entry("notice", "notices"),
        Map.entry("notification", "registerFcmInstallation"),
        Map.entry("organization", "gisuOrganizations"),
        Map.entry("project", "projects"),
        Map.entry("recruiting", "recruitingSeasons"),
        Map.entry("schedule", "mySchedules"),
        Map.entry("storage", "prepareFileUpload"),
        Map.entry("term", "terms")
    );

    @Test
    @DisplayName("모든 top-level 도메인은 GraphQL 공개 계약 또는 명시적 제외 분류를 가진다")
    void everyTopLevelDomainHasGraphQlContractOrExplicitExclusion() throws IOException {
        Set<String> classifiedDomains = new TreeSet<>(GRAPHQL_DOMAINS);
        classifiedDomains.addAll(NON_API_OR_INTERNAL_DOMAINS);

        assertThat(javaDomains()).containsExactlyElementsOf(classifiedDomains);
    }

    @Test
    @DisplayName("GraphQL 공개 도메인은 IDL, 관계 README, Controller를 함께 제공한다")
    void everyGraphQlDomainHasIdlReadmeAndController() throws IOException {
        for (String domain : GRAPHQL_DOMAINS) {
            Path schemaDirectory = GRAPHQL_ROOT.resolve(domain);
            Path controllerDirectory = JAVA_DOMAIN_ROOT.resolve(domain).resolve("adapter/in/graphql");

            assertThat(schemaDirectory.resolve("README.md"))
                .as("%s GraphQL 관계 README", domain)
                .isRegularFile();
            assertThat(graphQlFiles(schemaDirectory))
                .as("%s GraphQL IDL", domain)
                .isNotEmpty();
            assertThat(javaFiles(controllerDirectory))
                .as("%s GraphQL Controller", domain)
                .anyMatch(path -> read(path).contains("@Controller"));
        }
    }

    @Test
    @DisplayName("내부 전용 도메인은 우연히 public GraphQL IDL을 노출하지 않는다")
    void internalDomainsDoNotExposeGraphQlIdlAccidentally() {
        for (String domain : NON_API_OR_INTERNAL_DOMAINS) {
            assertThat(GRAPHQL_ROOT.resolve(domain))
                .as("%s 내부 도메인 GraphQL IDL", domain)
                .doesNotExist();
        }
    }

    @Test
    @DisplayName("각 GraphQL 도메인은 client가 진입할 대표 root field를 제공한다")
    void everyGraphQlDomainHasRepresentativeRootField() throws IOException {
        assertThat(REPRESENTATIVE_ROOT_FIELDS.keySet()).containsExactlyInAnyOrderElementsOf(GRAPHQL_DOMAINS);

        for (Map.Entry<String, String> entry : REPRESENTATIVE_ROOT_FIELDS.entrySet()) {
            String requestSource = Files.readString(
                GRAPHQL_ROOT.resolve(entry.getKey()).resolve("request.graphqls")
            );
            assertThat(requestSource)
                .as("%s 대표 root field", entry.getKey())
                .containsPattern("\\b" + entry.getValue() + "\\b");
        }
    }

    private static Set<String> javaDomains() throws IOException {
        try (Stream<Path> paths = Files.list(JAVA_DOMAIN_ROOT)) {
            return paths.filter(Files::isDirectory)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static Set<Path> graphQlFiles(Path directory) throws IOException {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".graphqls"))
                .collect(Collectors.toSet());
        }
    }

    private static Set<Path> javaFiles(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .collect(Collectors.toSet());
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("GraphQL Controller를 읽을 수 없습니다: " + path, exception);
        }
    }
}
