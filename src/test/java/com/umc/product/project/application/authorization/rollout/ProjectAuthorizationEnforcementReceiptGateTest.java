package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

class ProjectAuthorizationEnforcementReceiptGateTest {

    private static final Instant NOW = Instant.parse("2026-07-14T12:00:00Z");
    private static final String POLICY_VERSION = "1.1.0";
    private static final String POLICY_FINGERPRINT =
        "7cb5dd0453429ae766788952ae1972c9e24ae2c8f137dfa983a8bbe28a3f5e2e";
    private static final String ARTIFACT_SHA =
        "2dd6403da68c43d3ccd8a864fe50f8d597a513f4c3f7080948ae29ac29205564";

    @Test
    @DisplayName("receipt가 없어도 SHADOW와 LEGACY는 허용한다")
    void receipt가_없어도_shadow와_legacy는_허용한다() {
        ProjectAuthorizationEnforcementReceiptGate gate = gate(List.of());

        assertThatCode(() -> gate.validate(configuration(ProjectAuthorizationRolloutMode.SHADOW, Map.of())))
            .doesNotThrowAnyException();
        assertThatCode(() -> gate.validate(configuration(ProjectAuthorizationRolloutMode.LEGACY, Map.of())))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("receipt 없이 ENFORCE를 설정하면 fail closed한다")
    void receipt_없이_enforce를_설정하면_fail_closed한다() {
        assertFailure(
            gate(List.of()),
            configuration(ProjectAuthorizationRolloutMode.ENFORCE, Map.of()),
            ProjectAuthorizationRolloutFailureCode.ENFORCEMENT_ACTION_SET_MISMATCH
        );
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ProjectAuthorizationEnforcementWave.class)
    @DisplayName("각 wave 전체 action과 유효 receipt가 정확히 일치하면 ENFORCE를 허용한다")
    void 각_wave의_유효_receipt는_enforce를_허용한다(ProjectAuthorizationEnforcementWave wave) {
        EnumMap<ProjectPolicyAction, ProjectAuthorizationRolloutMode> overrides =
            new EnumMap<>(ProjectPolicyAction.class);
        wave.actions().forEach(action -> overrides.put(action, ProjectAuthorizationRolloutMode.ENFORCE));

        assertThatCode(() -> gate(List.of(receipt(wave))).validate(
            configuration(ProjectAuthorizationRolloutMode.SHADOW, overrides)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("default ENFORCE는 네 wave receipt가 모두 있어야 허용한다")
    void default_enforce는_네_wave_receipt가_모두_있어야_허용한다() {
        List<ProjectAuthorizationEnforcementReceipt> receipts =
            java.util.Arrays.stream(ProjectAuthorizationEnforcementWave.values())
                .map(ProjectAuthorizationEnforcementReceiptGateTest::receipt)
                .toList();

        assertThatCode(() -> gate(receipts).validate(
            configuration(ProjectAuthorizationRolloutMode.ENFORCE, Map.of())))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("receipt가 남아 있어도 전체 wave를 SHADOW로 rollback할 수 있다")
    void receipt가_남아_있어도_전체_wave를_shadow로_rollback할_수_있다() {
        assertThatCode(() -> gate(List.of(receipt(ProjectAuthorizationEnforcementWave.READ_CAPABILITY)))
            .validate(configuration(ProjectAuthorizationRolloutMode.SHADOW, Map.of())))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("receipt 범위 안에서 일부 action만 ENFORCE해도 rollback 상태로 허용한다")
    void receipt_범위_안에서_일부_action만_enforce해도_허용한다() {
        assertThatCode(() -> gate(List.of(receipt(ProjectAuthorizationEnforcementWave.READ_CAPABILITY)))
            .validate(configuration(ProjectAuthorizationRolloutMode.SHADOW,
                Map.of(ProjectPolicyAction.PROJECT_READ, ProjectAuthorizationRolloutMode.ENFORCE))))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("receipt가 승인하지 않은 action을 ENFORCE하면 거부한다")
    void receipt가_승인하지_않은_action을_enforce하면_거부한다() {
        assertFailure(
            gate(List.of(receipt(ProjectAuthorizationEnforcementWave.READ_CAPABILITY))),
            configuration(ProjectAuthorizationRolloutMode.SHADOW,
                Map.of(ProjectPolicyAction.PROJECT_CREATE, ProjectAuthorizationRolloutMode.ENFORCE)),
            ProjectAuthorizationRolloutFailureCode.ENFORCEMENT_ACTION_SET_MISMATCH
        );
    }

    @Test
    @DisplayName("default ENFORCE에서 일부 action을 SHADOW로 rollback해도 전체 receipt로 허용한다")
    void default_enforce에서_일부_action을_shadow로_rollback할_수_있다() {
        List<ProjectAuthorizationEnforcementReceipt> receipts =
            java.util.Arrays.stream(ProjectAuthorizationEnforcementWave.values())
                .map(ProjectAuthorizationEnforcementReceiptGateTest::receipt)
                .toList();

        assertThatCode(() -> gate(receipts).validate(configuration(
            ProjectAuthorizationRolloutMode.ENFORCE,
            Map.of(ProjectPolicyAction.PROJECT_READ, ProjectAuthorizationRolloutMode.SHADOW)
        ))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("receipt policyVersion이 compiled policy와 다르면 거부한다")
    void receipt_policy_version이_compiled_policy와_다르면_거부한다() {
        ProjectAuthorizationEnforcementReceipt invalid = withMetadata(receipt(
            ProjectAuthorizationEnforcementWave.READ_CAPABILITY), "2.0.0", POLICY_FINGERPRINT, ARTIFACT_SHA);

        assertMetadataFailure(invalid, ProjectAuthorizationRolloutFailureCode.RECEIPT_POLICY_VERSION_MISMATCH);
    }

    @Test
    @DisplayName("receipt fingerprint가 compiled policy와 다르면 거부한다")
    void receipt_fingerprint가_compiled_policy와_다르면_거부한다() {
        ProjectAuthorizationEnforcementReceipt invalid = withMetadata(receipt(
            ProjectAuthorizationEnforcementWave.READ_CAPABILITY), POLICY_VERSION, "f".repeat(64), ARTIFACT_SHA);

        assertMetadataFailure(invalid, ProjectAuthorizationRolloutFailureCode.RECEIPT_POLICY_FINGERPRINT_MISMATCH);
    }

    @Test
    @DisplayName("receipt artifact SHA가 fixed generated artifact와 다르면 거부한다")
    void receipt_artifact_sha가_generated_artifact와_다르면_거부한다() {
        ProjectAuthorizationEnforcementReceipt invalid = withMetadata(receipt(
            ProjectAuthorizationEnforcementWave.READ_CAPABILITY), POLICY_VERSION, POLICY_FINGERPRINT, "a".repeat(64));

        assertMetadataFailure(invalid, ProjectAuthorizationRolloutFailureCode.RECEIPT_ARTIFACT_SHA_MISMATCH);
    }

    @Test
    @DisplayName("future approvedAt receipt를 거부한다")
    void future_approved_at_receipt를_거부한다() {
        assertTimeFailure(
            receipt(ProjectAuthorizationEnforcementWave.READ_CAPABILITY, NOW.plusSeconds(1), NOW.plusSeconds(3600)),
            ProjectAuthorizationRolloutFailureCode.RECEIPT_APPROVED_AT_IN_FUTURE
        );
    }

    @Test
    @DisplayName("approvedAt 경계와 같은 시각에는 receipt가 유효하다")
    void approved_at_경계와_같은_시각에는_receipt가_유효하다() {
        ProjectAuthorizationEnforcementReceipt receipt = receipt(
            ProjectAuthorizationEnforcementWave.READ_CAPABILITY, NOW, NOW.plusSeconds(3600));
        EnumMap<ProjectPolicyAction, ProjectAuthorizationRolloutMode> overrides =
            new EnumMap<>(ProjectPolicyAction.class);
        receipt.actions().forEach(action -> overrides.put(action, ProjectAuthorizationRolloutMode.ENFORCE));

        assertThatCode(() -> gate(List.of(receipt)).validate(
            configuration(ProjectAuthorizationRolloutMode.SHADOW, overrides)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("approvedAt이 expiresAt보다 빠르지 않은 receipt를 거부한다")
    void invalid_time_window_receipt를_거부한다() {
        assertTimeFailure(
            receipt(ProjectAuthorizationEnforcementWave.READ_CAPABILITY, NOW, NOW),
            ProjectAuthorizationRolloutFailureCode.RECEIPT_TIME_WINDOW_INVALID
        );
    }

    @Test
    @DisplayName("expiresAt 경계에 도달한 receipt를 거부한다")
    void expired_receipt를_거부한다() {
        assertTimeFailure(
            receipt(ProjectAuthorizationEnforcementWave.READ_CAPABILITY, NOW.minusSeconds(3600), NOW),
            ProjectAuthorizationRolloutFailureCode.RECEIPT_EXPIRED
        );
    }

    private static ProjectAuthorizationEnforcementReceiptGate gate(
        List<ProjectAuthorizationEnforcementReceipt> receipts
    ) {
        return new ProjectAuthorizationEnforcementReceiptGate(
            new ProjectAuthorizationEnforcementReceiptDocument("1.0", receipts),
            new ProjectAuthorizationEnforcementTarget(POLICY_VERSION, POLICY_FINGERPRINT, ARTIFACT_SHA),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private static ProjectAuthorizationRolloutConfiguration configuration(
        ProjectAuthorizationRolloutMode defaultMode,
        Map<ProjectPolicyAction, ProjectAuthorizationRolloutMode> overrides
    ) {
        return new ProjectAuthorizationRolloutConfiguration(defaultMode, overrides);
    }

    private static ProjectAuthorizationEnforcementReceipt receipt(ProjectAuthorizationEnforcementWave wave) {
        return receipt(wave, NOW.minusSeconds(3600), NOW.plusSeconds(3600));
    }

    private static ProjectAuthorizationEnforcementReceipt receipt(
        ProjectAuthorizationEnforcementWave wave,
        Instant approvedAt,
        Instant expiresAt
    ) {
        return new ProjectAuthorizationEnforcementReceipt(
            POLICY_VERSION,
            POLICY_FINGERPRINT,
            ARTIFACT_SHA,
            wave,
            wave.actions(),
            "operator@umc.example",
            approvedAt,
            expiresAt
        );
    }

    private static ProjectAuthorizationEnforcementReceipt withMetadata(
        ProjectAuthorizationEnforcementReceipt source,
        String policyVersion,
        String policyFingerprint,
        String artifactSha
    ) {
        return new ProjectAuthorizationEnforcementReceipt(
            policyVersion,
            policyFingerprint,
            artifactSha,
            source.wave(),
            source.actions(),
            source.approver(),
            source.approvedAt(),
            source.expiresAt()
        );
    }

    private static void assertMetadataFailure(
        ProjectAuthorizationEnforcementReceipt receipt,
        ProjectAuthorizationRolloutFailureCode code
    ) {
        EnumMap<ProjectPolicyAction, ProjectAuthorizationRolloutMode> overrides =
            new EnumMap<>(ProjectPolicyAction.class);
        receipt.wave().actions().forEach(action -> overrides.put(action, ProjectAuthorizationRolloutMode.ENFORCE));
        assertFailure(gate(List.of(receipt)), configuration(ProjectAuthorizationRolloutMode.SHADOW, overrides), code);
    }

    private static void assertTimeFailure(
        ProjectAuthorizationEnforcementReceipt receipt,
        ProjectAuthorizationRolloutFailureCode code
    ) {
        assertMetadataFailure(receipt, code);
    }

    private static void assertFailure(
        ProjectAuthorizationEnforcementReceiptGate gate,
        ProjectAuthorizationRolloutConfiguration configuration,
        ProjectAuthorizationRolloutFailureCode code
    ) {
        assertThatThrownBy(() -> gate.validate(configuration))
            .isInstanceOfSatisfying(
                ProjectAuthorizationRolloutConfigurationException.class,
                exception -> assertThat(exception.code()).isEqualTo(code)
            );
    }
}
