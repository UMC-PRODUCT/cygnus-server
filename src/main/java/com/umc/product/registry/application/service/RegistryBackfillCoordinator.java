package com.umc.product.registry.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.umc.product.registry.application.port.out.RegistryAdvisoryLockPort;
import com.umc.product.registry.application.port.out.RegistryControlPort;
import com.umc.product.registry.application.port.out.RegistryRolloutPort;
import com.umc.product.registry.domain.RegistryBackfillAction;
import com.umc.product.registry.domain.RegistryBackfillBatch;
import com.umc.product.registry.domain.RegistryBackfillCheckpoint;
import com.umc.product.registry.domain.RegistryBackfillInterruptedException;
import com.umc.product.registry.domain.RegistryCutoverState;
import com.umc.product.registry.domain.RegistryReconciliationResult;
import com.umc.product.registry.domain.RegistryStatus;

public class RegistryBackfillCoordinator {

    private final Map<String, RegistryRolloutPort> rolloutByRegistry;
    private final RegistryControlPort controlPort;
    private final RegistryAdvisoryLockPort lockPort;
    private final Clock clock;
    private final int batchSize;
    private final int detailLimit;

    public RegistryBackfillCoordinator(
        List<RegistryRolloutPort> rolloutPorts,
        RegistryControlPort controlPort,
        RegistryAdvisoryLockPort lockPort,
        Clock clock,
        int batchSize,
        int detailLimit
    ) {
        if (batchSize <= 0 || detailLimit <= 0) {
            throw new IllegalArgumentException("batch size와 detail limit은 양수여야 합니다.");
        }
        this.rolloutByRegistry = indexRollouts(rolloutPorts);
        this.controlPort = controlPort;
        this.lockPort = lockPort;
        this.clock = clock;
        this.batchSize = batchSize;
        this.detailLimit = detailLimit;
    }

    public List<RegistryReconciliationResult> execute(RegistryBackfillAction action) {
        return switch (action) {
            case BACKFILL -> rolloutByRegistry.keySet().stream().map(this::backfill).toList();
            case RECONCILE -> reconcileAll();
        };
    }

    public RegistryReconciliationResult backfill(String registryName) {
        RegistryRolloutPort rollout = requireRollout(registryName);
        try (var ignored = lockPort.acquire(registryName)) {
            RegistryCutoverState state = prepareBackfill(rollout);
            Instant cutoverAt = clock.instant();
            try {
                rollout.preflight();
                rollout.startBackfill(cutoverAt);
                backfillSources(rollout);
                rollout.beforeValidation(cutoverAt);
                RegistryReconciliationResult result = rollout.reconcile(detailLimit);
                if (!result.isClean()) {
                    transition(state.registryName(), RegistryStatus.BACKFILLING,
                        RegistryStatus.BLOCKED, null, result.summary());
                    throw new IllegalStateException("registry reconcile drift: " + result.summary());
                }
                transition(state.registryName(), RegistryStatus.BACKFILLING,
                    RegistryStatus.VALIDATED, cutoverAt, result.summary());
                return result;
            } catch (RegistryBackfillInterruptedException e) {
                throw e;
            } catch (RuntimeException e) {
                blockIfBackfilling(registryName, e);
                throw e;
            }
        }
    }

    public List<RegistryReconciliationResult> reconcileAll() {
        return rolloutByRegistry.values().stream()
            .map(rollout -> {
                try (var ignored = lockPort.acquire(rollout.registryName())) {
                    return rollout.reconcile(detailLimit);
                }
            })
            .toList();
    }

    public void promoteReady(String registryName) {
        RegistryRolloutPort rollout = requireRollout(registryName);
        try (var ignored = lockPort.acquire(registryName)) {
            RegistryCutoverState state = controlPort.loadState(registryName);
            if (state.status() != RegistryStatus.VALIDATED) {
                throw new IllegalStateException("VALIDATED registry만 READY로 전이할 수 있습니다.");
            }
            RegistryReconciliationResult result = rollout.reconcile(detailLimit);
            if (!result.isClean()) {
                transition(registryName, RegistryStatus.VALIDATED,
                    RegistryStatus.BLOCKED, null, result.summary());
                throw new IllegalStateException("registry reconcile drift: " + result.summary());
            }
            transition(registryName, RegistryStatus.VALIDATED,
                RegistryStatus.READY, clock.instant(), result.summary());
        }
    }

    public void rollback(String registryName, String details) {
        requireRollout(registryName);
        try (var ignored = lockPort.acquire(registryName)) {
            RegistryStatus current = controlPort.loadState(registryName).status();
            if (current == RegistryStatus.DISABLED) {
                return;
            }
            if (!controlPort.transition(registryName, current,
                RegistryStatus.DISABLED, null, details)) {
                throw new IllegalStateException("registry rollback 상태가 경합으로 변경되었습니다.");
            }
        }
    }

    private RegistryCutoverState prepareBackfill(RegistryRolloutPort rollout) {
        RegistryCutoverState current = controlPort.loadState(rollout.registryName());
        if (current.status() == RegistryStatus.VALIDATED || current.status() == RegistryStatus.READY) {
            throw new IllegalStateException("VALIDATED/READY registry는 BACKFILL할 수 없습니다.");
        }
        if (current.status() == RegistryStatus.BACKFILLING) {
            return current;
        }
        if (current.status() == RegistryStatus.BLOCKED) {
            transition(rollout.registryName(), RegistryStatus.BLOCKED,
                RegistryStatus.DISABLED, null, "blocked retry rollback");
        }
        controlPort.resetCheckpoints(rollout.registryName(), rollout.sourceNames());
        transition(rollout.registryName(), RegistryStatus.DISABLED,
            RegistryStatus.BACKFILLING, null, "backfill started");
        return controlPort.loadState(rollout.registryName());
    }

    private void backfillSources(RegistryRolloutPort rollout) {
        for (String sourceName : rollout.sourceNames()) {
            RegistryBackfillCheckpoint checkpoint = controlPort.loadCheckpoint(
                rollout.registryName(), sourceName);
            while (!checkpoint.completed()) {
                RegistryBackfillBatch batch = rollout.backfillBatch(
                    sourceName, checkpoint.lastParentId(), batchSize);
                checkpoint = checkpoint.advance(batch);
                controlPort.saveCheckpoint(checkpoint);
            }
        }
    }

    private void blockIfBackfilling(String registryName, RuntimeException failure) {
        RegistryCutoverState state = controlPort.loadState(registryName);
        if (state.status() == RegistryStatus.BACKFILLING) {
            transition(registryName, RegistryStatus.BACKFILLING,
                RegistryStatus.BLOCKED, null, failure.getClass().getSimpleName());
        }
    }

    private void transition(
        String registryName,
        RegistryStatus expected,
        RegistryStatus target,
        Instant verifiedAt,
        String details
    ) {
        if (!expected.canProgressTo(target)) {
            throw new IllegalStateException("허용되지 않은 registry 상태 전이: " + expected + " -> " + target);
        }
        if (!controlPort.transition(registryName, expected, target, verifiedAt, details)) {
            throw new IllegalStateException("registry 상태가 경합으로 변경되었습니다: " + registryName);
        }
    }

    private RegistryRolloutPort requireRollout(String registryName) {
        RegistryRolloutPort rollout = rolloutByRegistry.get(registryName);
        if (rollout == null) {
            throw new IllegalArgumentException("등록되지 않은 registry입니다: " + registryName);
        }
        return rollout;
    }

    private static Map<String, RegistryRolloutPort> indexRollouts(List<RegistryRolloutPort> ports) {
        if (ports == null || ports.isEmpty()) {
            throw new IllegalArgumentException("rollout port는 하나 이상 필요합니다.");
        }
        Map<String, RegistryRolloutPort> indexed = new LinkedHashMap<>();
        ports.stream()
            .sorted((left, right) -> left.registryName().compareTo(right.registryName()))
            .forEach(port -> {
                if (indexed.putIfAbsent(port.registryName(), port) != null) {
                    throw new IllegalArgumentException("중복 registry rollout port: " + port.registryName());
                }
            });
        return Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
    }
}
