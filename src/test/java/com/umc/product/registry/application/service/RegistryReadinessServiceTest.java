package com.umc.product.registry.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.registry.application.port.out.RegistryNamespaceCoveragePort;
import com.umc.product.registry.application.port.out.RegistryStateQueryPort;
import com.umc.product.registry.domain.OwnershipEnforcementMode;
import com.umc.product.registry.domain.RegistryCutoverState;
import com.umc.product.registry.domain.RegistryName;
import com.umc.product.registry.domain.RegistryNamespaceCoverage;
import com.umc.product.registry.domain.RegistryStatus;

@DisplayName("RegistryReadinessService")
class RegistryReadinessServiceTest {

    @Test
    @DisplayName("세 canonical DB row와 exact-one namespace coverage가 모두 준비되면 strict cutover가 열린다")
    void 세_registry와_namespace_coverage가_모두_ready면_strict_cutover가_열린다() {
        // given
        RecordingStateQuery stateQuery = states(RegistryStatus.READY);
        RegistryReadinessService sut = service(
            stateQuery,
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

        // when & then
        assertThat(sut.areAllRegistriesReady()).isTrue();
        assertThat(sut.ownershipMode(RegistryName.FORM_OWNERSHIP))
            .isEqualTo(OwnershipEnforcementMode.STRICT);
        assertThat(stateQuery.requestedNames).contains(
            "file-usage",
            "form-ownership",
            "chat-room-ownership"
        );
    }

    @Test
    @DisplayName("persisted unknown namespace와 source evaluator 누락은 ownership readiness를 막는다")
    void unknown_persisted_namespace와_source_policy_누락은_ready를_막는다() {
        // given
        RegistryReadinessService sut = service(
            states(RegistryStatus.READY),
            true,
            coverage(
                RegistryName.FORM_OWNERSHIP,
                List.of("unknown.persisted"),
                List.of("source.declared", "form.standalone"),
                List.of("form.standalone")
            ),
            coverage(
                RegistryName.CHAT_OWNERSHIP,
                List.of("chat.standalone"),
                List.of("chat.standalone"),
                List.of("chat.standalone")
            )
        );

        // when & then
        assertThat(sut.isReady(RegistryName.FORM_OWNERSHIP)).isFalse();
        assertThat(sut.invalidNamespaces(RegistryName.FORM_OWNERSHIP))
            .containsExactly("source.declared", "unknown.persisted");
        assertThat(sut.ownershipMode(RegistryName.FORM_OWNERSHIP))
            .isEqualTo(OwnershipEnforcementMode.BLOCKED);
    }

    @Test
    @DisplayName("같은 namespace evaluator가 두 개면 DB READY여도 fail closed한다")
    void duplicate_evaluator는_DB_READY여도_fail_closed한다() {
        // given
        RegistryReadinessService sut = service(
            states(RegistryStatus.READY),
            true,
            coverage(
                RegistryName.FORM_OWNERSHIP,
                List.of("form.standalone"),
                List.of("form.standalone"),
                List.of("form.standalone", "form.standalone")
            ),
            coverage(
                RegistryName.CHAT_OWNERSHIP,
                List.of("chat.standalone"),
                List.of("chat.standalone"),
                List.of("chat.standalone")
            )
        );

        // when & then
        assertThat(sut.invalidNamespaces(RegistryName.FORM_OWNERSHIP))
            .containsExactly("form.standalone");
        assertThat(sut.isReady(RegistryName.FORM_OWNERSHIP)).isFalse();
    }

    @Test
    @DisplayName("property false는 audit이고 property true만으로는 DB DISABLED를 우회하지 못한다")
    void property_only_enable은_DB_DISABLED를_우회하지_못한다() {
        // given
        RegistryNamespaceCoveragePort form = coverage(
            RegistryName.FORM_OWNERSHIP,
            List.of("form.standalone"),
            List.of("form.standalone"),
            List.of("form.standalone")
        );
        RegistryNamespaceCoveragePort chat = coverage(
            RegistryName.CHAT_OWNERSHIP,
            List.of("chat.standalone"),
            List.of("chat.standalone"),
            List.of("chat.standalone")
        );

        // when & then
        assertThat(service(states(RegistryStatus.DISABLED), false, form, chat)
            .ownershipMode(RegistryName.FORM_OWNERSHIP))
            .isEqualTo(OwnershipEnforcementMode.AUDIT);
        assertThat(service(states(RegistryStatus.DISABLED), true, form, chat)
            .ownershipMode(RegistryName.FORM_OWNERSHIP))
            .isEqualTo(OwnershipEnforcementMode.BLOCKED);
    }

    @Test
    @DisplayName("coverage contribution이 없거나 조회가 실패하면 DB READY도 fail closed한다")
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

        RegistryReadinessService missing = service(states(RegistryStatus.READY), true);
        RegistryReadinessService failedQuery = service(
            states(RegistryStatus.READY),
            true,
            coverage(
                RegistryName.FORM_OWNERSHIP,
                List.of("form.standalone"),
                List.of("form.standalone"),
                List.of("form.standalone")
            ),
            failed
        );
        RegistryReadinessService emptyCoverage = service(
            states(RegistryStatus.READY),
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
    @DisplayName("DB state 조회 예외는 property true에서도 BLOCKED로 닫힌다")
    void state_query_exception은_fail_closed한다() {
        RegistryStateQueryPort failedState = registryName -> {
            throw new IllegalStateException("database unavailable");
        };
        RegistryReadinessService sut = new RegistryReadinessService(
            failedState,
            List.of(coverage(
                RegistryName.FORM_OWNERSHIP,
                List.of("form.standalone"),
                List.of("form.standalone"),
                List.of("form.standalone")
            )),
            new EngineOwnershipProperties(true)
        );

        assertThat(sut.isReady(RegistryName.FORM_OWNERSHIP)).isFalse();
        assertThat(sut.ownershipMode(RegistryName.FORM_OWNERSHIP))
            .isEqualTo(OwnershipEnforcementMode.BLOCKED);
    }

    private RegistryReadinessService service(
        RecordingStateQuery stateQuery,
        boolean enforcementEnabled,
        RegistryNamespaceCoveragePort... coveragePorts
    ) {
        return new RegistryReadinessService(
            stateQuery,
            List.of(coveragePorts),
            new EngineOwnershipProperties(enforcementEnabled)
        );
    }

    private static RecordingStateQuery states(RegistryStatus status) {
        Map<RegistryName, RegistryStatus> statuses = new EnumMap<>(RegistryName.class);
        for (RegistryName registryName : RegistryName.values()) {
            statuses.put(registryName, status);
        }
        return new RecordingStateQuery(statuses);
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

    private static final class RecordingStateQuery implements RegistryStateQueryPort {

        private final Map<RegistryName, RegistryStatus> statuses;
        private final List<String> requestedNames = new ArrayList<>();

        private RecordingStateQuery(Map<RegistryName, RegistryStatus> statuses) {
            this.statuses = statuses;
        }

        @Override
        public RegistryCutoverState loadState(String registryName) {
            requestedNames.add(registryName);
            RegistryName logicalName = RegistryName.fromCanonicalName(registryName);
            return new RegistryCutoverState(
                registryName,
                statuses.getOrDefault(logicalName, RegistryStatus.DISABLED),
                null,
                ""
            );
        }
    }
}
