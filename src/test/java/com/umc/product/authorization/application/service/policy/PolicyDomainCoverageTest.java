package com.umc.product.authorization.application.service.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

class PolicyDomainCoverageTest {

    private static final Path DOMAIN_ROOT = Path.of("src/main/java/com/umc/product");
    private static final Path COVERAGE = Path.of("src/main/resources/policies/domain-coverage.json");

    @Test
    @DisplayName("모든 production 최상위 도메인은 policy 관리 또는 명시적 exemption으로 분류된다")
    void classifiesEveryProductionDomain() throws Exception {
        CoverageDocument document = strictMapper().readValue(Files.readAllBytes(COVERAGE), CoverageDocument.class);
        Set<String> productionDomains;
        try (var paths = Files.list(DOMAIN_ROOT)) {
            productionDomains = paths
                .filter(Files::isDirectory)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toUnmodifiableSet());
        }

        assertThat(document.schemaVersion()).isEqualTo("1.0");
        assertThat(document.domains()).extracting(DomainCoverage::namespace)
            .doesNotHaveDuplicates()
            .containsExactlyInAnyOrderElementsOf(productionDomains);
        assertThat(document.domains()).allSatisfy(domain -> {
            assertThat(domain.reason()).isNotBlank();
            if (domain.classification() == Classification.POLICY_MANAGED) {
                assertThat(domain.migrationStatus()).isNotEqualTo(MigrationStatus.EXEMPT);
            } else {
                assertThat(domain.migrationStatus()).isEqualTo(MigrationStatus.EXEMPT);
            }
        });
    }

    @Test
    @DisplayName("전환이 연결된 도메인은 SHADOW이고 나머지는 전환 대기 상태다")
    void recordsCurrentMigrationState() throws Exception {
        CoverageDocument document = strictMapper().readValue(Files.readAllBytes(COVERAGE), CoverageDocument.class);
        Set<String> shadowNamespaces = Set.of(
            "project",
            "audit",
            "authorization",
            "blog",
            "certificate",
            "chat",
            "maintenance",
            "notification",
            "storage",
            "term");

        assertThat(document.domains())
            .filteredOn(domain -> shadowNamespaces.contains(domain.namespace()))
            .allSatisfy(domain -> assertThat(domain.migrationStatus()).isEqualTo(MigrationStatus.SHADOW));
        assertThat(document.domains())
            .filteredOn(domain -> domain.classification() == Classification.POLICY_MANAGED)
            .filteredOn(domain -> !shadowNamespaces.contains(domain.namespace()))
            .allSatisfy(domain -> assertThat(domain.migrationStatus()).isEqualTo(MigrationStatus.PLANNED));
    }

    private JsonMapper strictMapper() {
        return JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    }

    private record CoverageDocument(String schemaVersion, List<DomainCoverage> domains) {
    }

    private record DomainCoverage(
        String namespace,
        Classification classification,
        MigrationStatus migrationStatus,
        String reason
    ) {
    }

    private enum Classification {
        POLICY_MANAGED,
        AUTHENTICATION_BOUNDARY,
        INTERNAL_ONLY,
        NO_ACTOR_AUTHORIZATION,
        INFRASTRUCTURE
    }

    private enum MigrationStatus {
        PLANNED,
        SHADOW,
        ENFORCED,
        EXEMPT
    }
}
