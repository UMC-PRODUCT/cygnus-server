package com.umc.product.registry.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.umc.product.registry.application.port.out.RegistryAdvisoryLockPort;
import com.umc.product.registry.application.port.out.RegistryControlPort;
import com.umc.product.registry.application.port.out.RegistryRolloutPort;
import com.umc.product.registry.application.service.RegistryBackfillCoordinator;
import com.umc.product.registry.domain.RegistryBackfillBatch;
import com.umc.product.registry.domain.RegistryBackfillCheckpoint;
import com.umc.product.registry.domain.RegistryBackfillInterruptedException;
import com.umc.product.registry.domain.RegistryCutoverState;
import com.umc.product.registry.domain.RegistryDriftType;
import com.umc.product.registry.domain.RegistryReconciliationResult;
import com.umc.product.registry.domain.RegistrySourceReconciliation;
import com.umc.product.registry.domain.RegistryStatus;

class RegistryBackfillCoordinatorTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-07-18T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void backfilling은_저장된_checkpoint_다음_parent부터_resume한다() {
        // given
        InMemoryControl control = new InMemoryControl(RegistryStatus.BACKFILLING);
        control.saveCheckpoint(new RegistryBackfillCheckpoint(
            "file-usage", "member", 500L, 500L, false));
        RecordingRollout rollout = RecordingRollout.clean("file-usage", List.of("member"));
        RegistryBackfillCoordinator coordinator = coordinator(rollout, control);

        // when
        coordinator.backfill("file-usage");

        // then
        assertThat(rollout.requestedAfterIds).containsExactly(500L);
        assertThat(control.resetCount).isZero();
        assertThat(control.state.status()).isEqualTo(RegistryStatus.VALIDATED);
    }

    @Test
    void disabled는_모든_checkpoint를_reset하고_low_pk부터_full_rescan한다() {
        // given
        InMemoryControl control = new InMemoryControl(RegistryStatus.DISABLED);
        control.saveCheckpoint(new RegistryBackfillCheckpoint(
            "file-usage", "member", 900L, 900L, true));
        RecordingRollout rollout = RecordingRollout.clean("file-usage", List.of("member"));
        RegistryBackfillCoordinator coordinator = coordinator(rollout, control);

        // when
        coordinator.backfill("file-usage");

        // then
        assertThat(control.resetCount).isEqualTo(1);
        assertThat(rollout.requestedAfterIds).containsExactly(0L);
    }

    @Test
    void process_interrupt는_backfilling_checkpoint를_보존하고_다음_실행이_resume한다() {
        // given
        InMemoryControl control = new InMemoryControl(RegistryStatus.DISABLED);
        RecordingRollout rollout = RecordingRollout.interruptAfterFirstBatch("file-usage", "member");
        RegistryBackfillCoordinator coordinator = coordinator(rollout, control);

        // when & then
        assertThatThrownBy(() -> coordinator.backfill("file-usage"))
            .isInstanceOf(RegistryBackfillInterruptedException.class);
        assertThat(control.state.status()).isEqualTo(RegistryStatus.BACKFILLING);
        assertThat(control.loadCheckpoint("file-usage", "member").lastParentId()).isEqualTo(500L);

        rollout.resume();
        coordinator.backfill("file-usage");
        assertThat(rollout.requestedAfterIds).containsExactly(0L, 500L, 500L);
        assertThat(control.state.status()).isEqualTo(RegistryStatus.VALIDATED);
    }

    @Test
    void reconcile_drift가_하나라도_있으면_blocked로_전이하고_실패한다() {
        // given
        InMemoryControl control = new InMemoryControl(RegistryStatus.DISABLED);
        RecordingRollout rollout = RecordingRollout.drifting("file-usage", "member");
        RegistryBackfillCoordinator coordinator = coordinator(rollout, control);

        // when & then
        assertThatThrownBy(() -> coordinator.backfill("file-usage"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("drift");
        assertThat(control.state.status()).isEqualTo(RegistryStatus.BLOCKED);
    }

    @Test
    void reconcile_action은_control_state와_checkpoint를_전혀_쓰지_않는다() {
        // given
        InMemoryControl control = new InMemoryControl(RegistryStatus.READY);
        RecordingRollout rollout = RecordingRollout.clean("file-usage", List.of("member"));
        RegistryBackfillCoordinator coordinator = coordinator(rollout, control);
        int writesBefore = control.writeCount;

        // when
        List<RegistryReconciliationResult> first = coordinator.reconcileAll();
        List<RegistryReconciliationResult> second = coordinator.reconcileAll();

        // then
        assertThat(first).isEqualTo(second);
        assertThat(control.writeCount).isEqualTo(writesBefore);
    }

    @Test
    void validated와_ready_registry는_backfill을_거부하고_checkpoint를_건드리지_않는다() {
        for (RegistryStatus status : List.of(RegistryStatus.VALIDATED, RegistryStatus.READY)) {
            // given
            InMemoryControl control = new InMemoryControl(status);
            RecordingRollout rollout = RecordingRollout.clean("file-usage", List.of("member"));
            RegistryBackfillCoordinator coordinator = coordinator(rollout, control);

            // when & then
            assertThatThrownBy(() -> coordinator.backfill("file-usage"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VALIDATED/READY");
            assertThat(control.resetCount).isZero();
            assertThat(control.state.status()).isEqualTo(status);
        }
    }

    @Test
    void clean_validated_registry는_ready로_승격하고_rollback은_disabled로_복귀시킨다() {
        // given
        InMemoryControl control = new InMemoryControl(RegistryStatus.VALIDATED);
        RecordingRollout rollout = RecordingRollout.clean("file-usage", List.of("member"));
        RegistryBackfillCoordinator coordinator = coordinator(rollout, control);

        // when
        coordinator.promoteReady("file-usage");

        // then
        assertThat(control.state.status()).isEqualTo(RegistryStatus.READY);
        coordinator.rollback("file-usage", "operator rollback");
        assertThat(control.state.status()).isEqualTo(RegistryStatus.DISABLED);
    }

    private RegistryBackfillCoordinator coordinator(
        RecordingRollout rollout,
        InMemoryControl control
    ) {
        RegistryAdvisoryLockPort lockPort = registryName -> () -> { };
        return new RegistryBackfillCoordinator(
            List.of(rollout), control, lockPort, CLOCK, 500, 20);
    }

    private static final class InMemoryControl implements RegistryControlPort {

        private RegistryCutoverState state;
        private final Map<String, RegistryBackfillCheckpoint> checkpoints = new LinkedHashMap<>();
        private int resetCount;
        private int writeCount;

        private InMemoryControl(RegistryStatus status) {
            this.state = new RegistryCutoverState("file-usage", status, null, "");
        }

        @Override
        public RegistryCutoverState loadState(String registryName) {
            return state;
        }

        @Override
        public boolean transition(
            String registryName,
            RegistryStatus expected,
            RegistryStatus target,
            Instant verifiedAt,
            String details
        ) {
            writeCount++;
            if (state.status() != expected) {
                return false;
            }
            state = new RegistryCutoverState(registryName, target, verifiedAt, details);
            return true;
        }

        @Override
        public RegistryBackfillCheckpoint loadCheckpoint(String registryName, String sourceName) {
            return checkpoints.getOrDefault(
                sourceName,
                RegistryBackfillCheckpoint.initial(registryName, sourceName));
        }

        @Override
        public void saveCheckpoint(RegistryBackfillCheckpoint checkpoint) {
            writeCount++;
            checkpoints.put(checkpoint.sourceName(), checkpoint);
        }

        @Override
        public void resetCheckpoints(String registryName, List<String> sourceNames) {
            writeCount++;
            resetCount++;
            checkpoints.clear();
        }
    }

    private static final class RecordingRollout implements RegistryRolloutPort {

        private final String registryName;
        private final List<String> sourceNames;
        private final RegistryReconciliationResult reconciliation;
        private final List<Long> requestedAfterIds = new ArrayList<>();
        private boolean interrupt;
        private int invocations;

        private RecordingRollout(
            String registryName,
            List<String> sourceNames,
            RegistryReconciliationResult reconciliation,
            boolean interrupt
        ) {
            this.registryName = registryName;
            this.sourceNames = sourceNames;
            this.reconciliation = reconciliation;
            this.interrupt = interrupt;
        }

        static RecordingRollout clean(String registryName, List<String> sourceNames) {
            return new RecordingRollout(
                registryName,
                sourceNames,
                new RegistryReconciliationResult(registryName, List.of()),
                false);
        }

        static RecordingRollout interruptAfterFirstBatch(String registryName, String sourceName) {
            return new RecordingRollout(
                registryName,
                List.of(sourceName),
                new RegistryReconciliationResult(registryName, List.of()),
                true);
        }

        static RecordingRollout drifting(String registryName, String sourceName) {
            Map<RegistryDriftType, Long> counts = new EnumMap<>(RegistryDriftType.class);
            counts.put(RegistryDriftType.SOURCE_ONLY_MISSING, 1L);
            RegistrySourceReconciliation source = new RegistrySourceReconciliation(
                sourceName, counts, List.of("missing:file-a"));
            return new RecordingRollout(
                registryName,
                List.of(sourceName),
                new RegistryReconciliationResult(registryName, List.of(source)),
                false);
        }

        void resume() {
            interrupt = false;
        }

        @Override
        public String registryName() {
            return registryName;
        }

        @Override
        public List<String> sourceNames() {
            return sourceNames;
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
            requestedAfterIds.add(afterParentId);
            invocations++;
            if (interrupt && invocations == 2) {
                throw new RegistryBackfillInterruptedException("simulated stop");
            }
            if (interrupt) {
                return new RegistryBackfillBatch(500L, 500L, false);
            }
            return new RegistryBackfillBatch(afterParentId, 0L, true);
        }

        @Override
        public RegistryReconciliationResult reconcile(int detailLimit) {
            return reconciliation;
        }
    }
}
