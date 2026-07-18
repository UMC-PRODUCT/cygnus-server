package com.umc.product.registry.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.registry.application.port.out.RegistryNamespaceCoveragePort;
import com.umc.product.registry.domain.OwnershipEnforcementMode;
import com.umc.product.registry.domain.RegistryName;
import com.umc.product.registry.domain.RegistryNamespaceCoverage;

@DisplayName("RegistryReadinessService")
class RegistryReadinessServiceTest {

    @Test
    @DisplayName("Flyway가 완료되고 exact-one namespace coverage가 준비되면 strict enforcement가 열린다")
    void Flyway와_namespace_coverage가_준비되면_strict_enforcement가_열린다() {
        RegistryReadinessService sut = service(
            true,
            coverage(
                RegistryName.FORM_OWNERSHIP,
                List.of("project.application-form", "legacy.form"),
                List.of("project.application-form", "notice.vote", "form.standalone"),
                List.of("project.application-form", "notice.vote", "form.standalone", "legacy.form")
            ),
            coverage(
                RegistryName.CHAT_OWNERSHIP,
                List.of("chat.standalone"),
                List.of("chat.standalone"),
                List.of("chat.standalone")
            )
        );

        assertThat(sut.areAllRegistriesReady()).isTrue();
        assertThat(sut.isReady(RegistryName.STORAGE_USAGE)).isTrue();
        assertThat(sut.ownershipMode(RegistryName.FORM_OWNERSHIP))
            .isEqualTo(OwnershipEnforcementMode.STRICT);
    }

    @Test
    @DisplayName("persisted unknown namespace와 source evaluator 누락은 ownership readiness를 막는다")
    void unknown_persisted_namespace와_source_policy_누락은_ready를_막는다() {
        RegistryReadinessService sut = service(
            true,
            coverage(
                RegistryName.FORM_OWNERSHIP,
                List.of("unknown.persisted"),
                List.of("source.declared", "form.standalone"),
                List.of("form.standalone")
            )
        );

        assertThat(sut.isReady(RegistryName.FORM_OWNERSHIP)).isFalse();
        assertThat(sut.invalidNamespaces(RegistryName.FORM_OWNERSHIP))
            .containsExactly("source.declared", "unknown.persisted");
        assertThat(sut.ownershipMode(RegistryName.FORM_OWNERSHIP))
            .isEqualTo(OwnershipEnforcementMode.BLOCKED);
    }

    @Test
    @DisplayName("같은 namespace evaluator가 두 개면 fail closed한다")
    void duplicate_evaluator는_fail_closed한다() {
        RegistryReadinessService sut = service(
            true,
            coverage(
                RegistryName.FORM_OWNERSHIP,
                List.of("form.standalone"),
                List.of("form.standalone"),
                List.of("form.standalone", "form.standalone")
            )
        );

        assertThat(sut.invalidNamespaces(RegistryName.FORM_OWNERSHIP))
            .containsExactly("form.standalone");
        assertThat(sut.isReady(RegistryName.FORM_OWNERSHIP)).isFalse();
    }

    @Test
    @DisplayName("property false는 audit이고 property true는 valid coverage에서만 strict다")
    void property와_coverage가_enforcement_mode를_결정한다() {
        RegistryNamespaceCoveragePort form = coverage(
            RegistryName.FORM_OWNERSHIP,
            List.of("form.standalone"),
            List.of("form.standalone"),
            List.of("form.standalone")
        );

        assertThat(service(false, form).ownershipMode(RegistryName.FORM_OWNERSHIP))
            .isEqualTo(OwnershipEnforcementMode.AUDIT);
        assertThat(service(true, form).ownershipMode(RegistryName.FORM_OWNERSHIP))
            .isEqualTo(OwnershipEnforcementMode.STRICT);
    }

    @Test
    @DisplayName("coverage contribution이 없거나 조회가 실패하면 fail closed한다")
    void missing_or_failed_coverage는_fail_closed한다() {
        RegistryNamespaceCoveragePort failed = new RegistryNamespaceCoveragePort() {
            @Override
            public RegistryName registryName() {
                return RegistryName.CHAT_OWNERSHIP;
            }

            @Override
            public RegistryNamespaceCoverage loadCoverage() {
                throw new IllegalStateException("coverage unavailable");
            }
        };

        RegistryReadinessService missing = service(true);
        RegistryReadinessService failedQuery = service(true, failed);
        RegistryReadinessService emptyCoverage = service(
            true,
            coverage(RegistryName.FORM_OWNERSHIP, List.of(), List.of(), List.of())
        );

        assertThat(missing.isReady(RegistryName.FORM_OWNERSHIP)).isFalse();
        assertThat(failedQuery.isReady(RegistryName.CHAT_OWNERSHIP)).isFalse();
        assertThat(emptyCoverage.isReady(RegistryName.FORM_OWNERSHIP)).isFalse();
        assertThat(failedQuery.ownershipMode(RegistryName.CHAT_OWNERSHIP))
            .isEqualTo(OwnershipEnforcementMode.BLOCKED);
    }

    @Test
    @DisplayName("Storage cutover certification이 PENDING이면 전체 readiness를 막는다")
    void Storage_cutover가_PENDING이면_ready를_막는다() {
        RegistryReadinessService sut = new RegistryReadinessService(
            List.of(),
            new EngineOwnershipProperties(true),
            () -> false
        );

        assertThat(sut.isReady(RegistryName.STORAGE_USAGE)).isFalse();
        assertThat(sut.invalidNamespaces(RegistryName.STORAGE_USAGE))
            .containsExactly("<cutover-pending>");
        assertThat(sut.areAllRegistriesReady()).isFalse();
    }

    private RegistryReadinessService service(
        boolean enforcementEnabled,
        RegistryNamespaceCoveragePort... coveragePorts
    ) {
        return new RegistryReadinessService(
            List.of(coveragePorts),
            new EngineOwnershipProperties(enforcementEnabled),
            () -> true
        );
    }

    private static RegistryNamespaceCoveragePort coverage(
        RegistryName registryName,
        List<String> persisted,
        List<String> declared,
        List<String> evaluators
    ) {
        return new RegistryNamespaceCoveragePort() {
            @Override
            public RegistryName registryName() {
                return registryName;
            }

            @Override
            public RegistryNamespaceCoverage loadCoverage() {
                return new RegistryNamespaceCoverage(persisted, declared, evaluators);
            }
        };
    }
}
