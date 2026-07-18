package com.umc.product.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.chat.adapter.out.backfill.ChatRoomOwnershipRolloutAdapter;
import com.umc.product.registry.adapter.out.jdbc.PostgresRegistryControlAdapter;
import com.umc.product.registry.application.port.in.query.GetRegistryReadinessUseCase;
import com.umc.product.registry.application.port.out.RegistryControlPort;
import com.umc.product.registry.application.port.out.RegistryRolloutPort;
import com.umc.product.registry.application.service.RegistryCutoverFenceService;
import com.umc.product.registry.domain.RegistryBackfillBatch;
import com.umc.product.registry.domain.RegistryCutoverMilestone;
import com.umc.product.registry.domain.RegistryCutoverPlan;
import com.umc.product.registry.domain.RegistryName;
import com.umc.product.registry.domain.RegistryReconciliationResult;
import com.umc.product.registry.domain.RegistrySourceReconciliation;
import com.umc.product.registry.domain.RegistryStatus;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("Registry rolling cutover와 lifecycle contract 통합")
@ResourceLock("file-upload-lifecycle-contract")
class RegistryCutoverIntegrationTest extends IntegrationTestSupport {

    private static final String LIFECYCLE_CONSTRAINT = "ck_file_metadata_upload_lifecycle";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GetRegistryReadinessUseCase readinessUseCase;

    @Autowired
    private FileUsageRegistryReadinessPort storageReadiness;

    @Test
    @DisplayName("expand-only mismatch는 NOT VALID에 보존되고 보정 뒤 VALIDATE된다")
    void lifecycle_contract는_not_valid_추가와_validate를_분리한다() {
        assertThat(jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM flyway_schema_history
            WHERE version = '2026.07.16.00.40'
            """, Long.class)).isOne();
        jdbcTemplate.execute("ALTER TABLE file_metadata DROP CONSTRAINT IF EXISTS "
            + LIFECYCLE_CONSTRAINT);
        insertFile("legacy-mismatch", false, true);

        jdbcTemplate.execute("""
            ALTER TABLE file_metadata
            ADD CONSTRAINT ck_file_metadata_upload_lifecycle
            CHECK (is_uploaded = (confirmed_at IS NOT NULL)) NOT VALID
            """);

        assertThat(constraintValidated()).isFalse();
        assertThat(countFile("legacy-mismatch")).isOne();
        assertThatThrownBy(() -> jdbcTemplate.execute("""
            ALTER TABLE file_metadata
            VALIDATE CONSTRAINT ck_file_metadata_upload_lifecycle
            """)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertFile("new-mismatch", false, true))
            .isInstanceOf(DataAccessException.class);

        jdbcTemplate.update("UPDATE file_metadata SET confirmed_at = NULL WHERE id = ?",
            "legacy-mismatch");
        jdbcTemplate.execute("""
            ALTER TABLE file_metadata
            VALIDATE CONSTRAINT ck_file_metadata_upload_lifecycle
            """);

        assertThat(constraintValidated()).isTrue();
    }

    @Test
    @DisplayName("final primary-replica reconcile과 coverage가 clean일 때만 세 READY와 property를 연다")
    void clean_cutover만_READY와_runtime_property를_연다() {
        seedStates(RegistryStatus.VALIDATED);
        RecordingRuntimePorts runtime = new RecordingRuntimePorts();
        RegistryCutoverFenceService fence = fence(runtime, registryName -> true);

        boolean promoted = fence.promoteReady(forwardPlan());

        assertThat(promoted).isTrue();
        assertThat(loadStatuses().values()).containsOnly(RegistryStatus.READY);
        assertThat(runtime.events).containsExactly("properties-enable");
        assertThat(storageReadiness.getStatus()).isEqualTo(FileUsageRegistryStatus.READY);
    }

    @Test
    @DisplayName("READY 뒤 old-writer source row는 실제 reconcile drift로 세 registry와 cleanup을 닫는다")
    void old_writer_drift는_runtime_fence와_cleanup을_닫는다() {
        seedStates(RegistryStatus.VALIDATED);
        RecordingRuntimePorts runtime = new RecordingRuntimePorts();
        RegistryCutoverFenceService fence = fence(runtime, registryName -> true);
        assertThat(fence.promoteReady(forwardPlan())).isTrue();
        jdbcTemplate.update("""
            INSERT INTO chat_room (created_at, updated_at)
            VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);

        boolean clean = fence.reconcileRuntimeFence();

        assertThat(clean).isFalse();
        assertThat(loadStatuses().values()).containsOnly(RegistryStatus.BLOCKED);
        assertThat(runtime.events).endsWith("properties-disable");
        assertThat(storageReadiness.getStatus()).isEqualTo(FileUsageRegistryStatus.DISABLED);
    }

    @Test
    @DisplayName("replica mismatch는 READY 전환과 property enable을 거부하고 BLOCKED로 닫는다")
    void replica_mismatch는_property_only_cutover를_거부한다() {
        seedStates(RegistryStatus.VALIDATED);
        RecordingRuntimePorts runtime = new RecordingRuntimePorts();
        RegistryCutoverFenceService fence = fence(
            runtime,
            registryName -> !RegistryName.FORM_OWNERSHIP.canonicalName().equals(registryName)
        );

        boolean promoted = fence.promoteReady(forwardPlan());

        assertThat(promoted).isFalse();
        assertThat(loadStatuses().values()).containsOnly(RegistryStatus.BLOCKED);
        assertThat(runtime.events).containsExactly("properties-disable");
    }

    @Test
    @DisplayName("forward rollout에서 maintenance나 old-pod drain을 건너뛴 plan은 gate 진입 전 거부한다")
    void forward_plan은_maintenance와_old_pod_drain을_강제한다() {
        List<RegistryCutoverMilestone> missingMaintenance = new ArrayList<>(
            List.of(RegistryCutoverMilestone.values())
        );
        missingMaintenance.remove(RegistryCutoverMilestone.STORAGE_MAINTENANCE_START);
        List<RegistryCutoverMilestone> wrongDrainOrder = new ArrayList<>(
            List.of(RegistryCutoverMilestone.values())
        );
        java.util.Collections.swap(wrongDrainOrder, 2, 3);

        assertThatThrownBy(() -> new RegistryCutoverPlan(missingMaintenance))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegistryCutoverPlan(wrongDrainOrder))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("drift transition이 경합으로 실패해도 property는 먼저 닫고 traffic은 전환하지 않는다")
    void transition_failure에도_runtime_property는_fail_closed다() {
        seedStates(RegistryStatus.READY);
        RecordingRuntimePorts runtime = new RecordingRuntimePorts();
        RegistryControlPort failingControl = new PostgresRegistryControlAdapter(jdbcTemplate) {
            @Override
            public boolean transition(
                String registryName,
                RegistryStatus expected,
                RegistryStatus target,
                Instant verifiedAt,
                String details
            ) {
                return false;
            }
        };
        RegistryCutoverFenceService fence = fence(
            runtime,
            registryName -> false,
            failingControl
        );

        assertThatThrownBy(fence::reconcileRuntimeFence)
            .isInstanceOf(IllegalStateException.class);
        assertThat(runtime.events).containsExactly("properties-disable");
        assertThat(runtime.events).doesNotContain("traffic-transition");
    }

    @Test
    @DisplayName("mixed-version rollback은 maintenance 뒤 세 DISABLED와 property false 후 traffic을 전환한다")
    void rollback은_maintenance와_fail_closed_state를_선행한다() {
        seedStates(RegistryStatus.READY);
        RecordingRuntimePorts runtime = new RecordingRuntimePorts();
        RegistryCutoverFenceService fence = fence(runtime, registryName -> true);

        fence.rollbackAndTransitionTraffic();

        assertThat(loadStatuses().values()).containsOnly(RegistryStatus.DISABLED);
        assertThat(runtime.events).containsExactly(
            "maintenance-start",
            "properties-disable",
            "traffic-transition"
        );
        assertThat(storageReadiness.getStatus()).isEqualTo(FileUsageRegistryStatus.DISABLED);
    }

    private RegistryCutoverFenceService fence(
        RecordingRuntimePorts runtime,
        com.umc.product.registry.application.port.out.RegistryReplicaVerificationPort replica
    ) {
        return fence(runtime, replica, new PostgresRegistryControlAdapter(jdbcTemplate));
    }

    private RegistryCutoverPlan forwardPlan() {
        return new RegistryCutoverPlan(List.of(RegistryCutoverMilestone.values()));
    }

    private RegistryCutoverFenceService fence(
        RecordingRuntimePorts runtime,
        com.umc.product.registry.application.port.out.RegistryReplicaVerificationPort replica,
        RegistryControlPort controlPort
    ) {
        return new RegistryCutoverFenceService(
            controlPort,
            List.of(
                cleanRollout(RegistryName.STORAGE_USAGE),
                cleanRollout(RegistryName.FORM_OWNERSHIP),
                new ChatRoomOwnershipRolloutAdapter(jdbcTemplate)
            ),
            replica,
            readinessUseCase,
            runtime,
            runtime,
            runtime,
            Clock.fixed(Instant.parse("2026-07-18T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    private RegistryRolloutPort cleanRollout(RegistryName registryName) {
        return new RegistryRolloutPort() {
            @Override
            public String registryName() {
                return registryName.canonicalName();
            }

            @Override
            public List<String> sourceNames() {
                return List.of("fixture");
            }

            @Override
            public void preflight() {
            }

            @Override
            public RegistryBackfillBatch backfillBatch(
                String sourceName,
                long afterParentId,
                int batchSize
            ) {
                return new RegistryBackfillBatch(afterParentId, 0L, true);
            }

            @Override
            public RegistryReconciliationResult reconcile(int detailLimit) {
                return new RegistryReconciliationResult(
                    registryName.canonicalName(),
                    List.of(new RegistrySourceReconciliation("fixture", Map.of(), List.of()))
                );
            }
        };
    }

    private void seedStates(RegistryStatus status) {
        jdbcTemplate.update("DELETE FROM registry_cutover_state");
        for (RegistryName registryName : RegistryName.values()) {
            jdbcTemplate.update("""
                INSERT INTO registry_cutover_state
                    (registry_name, status, verified_at, details)
                VALUES (?, ?, NULL, '')
                """, registryName.canonicalName(), status.name());
        }
    }

    private Map<RegistryName, RegistryStatus> loadStatuses() {
        Map<RegistryName, RegistryStatus> statuses = new EnumMap<>(RegistryName.class);
        jdbcTemplate.query("""
            SELECT registry_name, status FROM registry_cutover_state
            """, resultSet -> {
                while (resultSet.next()) {
                    statuses.put(
                        RegistryName.fromCanonicalName(resultSet.getString("registry_name")),
                        RegistryStatus.valueOf(resultSet.getString("status"))
                    );
                }
                return null;
            });
        return statuses;
    }

    private void insertFile(String fileId, boolean uploaded, boolean withConfirmedAt) {
        jdbcTemplate.update("""
            INSERT INTO file_metadata
                (id, is_uploaded, created_at, updated_at, file_size, category,
                 storage_provider, content_type, storage_key, original_file_name, confirmed_at)
            VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'ETC',
                    'AWS_S3', 'text/plain', ?, 'test.txt',
                    CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END)
            """, fileId, uploaded, "registry-cutover/" + fileId, withConfirmedAt);
    }

    private long countFile(String fileId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM file_metadata WHERE id = ?",
            Long.class,
            fileId
        );
    }

    private boolean constraintValidated() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT convalidated FROM pg_constraint WHERE conname = ?
            """, Boolean.class, LIFECYCLE_CONSTRAINT));
    }

    private static final class RecordingRuntimePorts implements
        com.umc.product.registry.application.port.out.RegistryRuntimeSwitchPort,
        com.umc.product.registry.application.port.out.RegistryMaintenanceFencePort,
        com.umc.product.registry.application.port.out.RegistryTrafficTransitionPort {

        private final List<String> events = new ArrayList<>();

        @Override
        public void enableCleanupAndEnforcement() {
            events.add("properties-enable");
        }

        @Override
        public void disableCleanupAndEnforcement() {
            events.add("properties-disable");
        }

        @Override
        public void startStorageMaintenance() {
            events.add("maintenance-start");
        }

        @Override
        public void transitionTraffic() {
            events.add("traffic-transition");
        }
    }
}
