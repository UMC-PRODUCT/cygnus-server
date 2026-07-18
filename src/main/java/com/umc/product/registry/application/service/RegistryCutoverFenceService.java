package com.umc.product.registry.application.service;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.umc.product.registry.application.port.in.query.GetRegistryReadinessUseCase;
import com.umc.product.registry.application.port.out.RegistryControlPort;
import com.umc.product.registry.application.port.out.RegistryMaintenanceFencePort;
import com.umc.product.registry.application.port.out.RegistryReplicaVerificationPort;
import com.umc.product.registry.application.port.out.RegistryRolloutPort;
import com.umc.product.registry.application.port.out.RegistryRuntimeSwitchPort;
import com.umc.product.registry.application.port.out.RegistryTrafficTransitionPort;
import com.umc.product.registry.domain.RegistryCutoverPlan;
import com.umc.product.registry.domain.RegistryCutoverState;
import com.umc.product.registry.domain.RegistryName;
import com.umc.product.registry.domain.RegistryStatus;

public class RegistryCutoverFenceService {

    private static final int RECONCILIATION_DETAIL_LIMIT = 20;

    private final RegistryControlPort controlPort;
    private final Map<RegistryName, RegistryRolloutPort> rolloutByRegistry;
    private final RegistryReplicaVerificationPort replicaVerificationPort;
    private final GetRegistryReadinessUseCase readinessUseCase;
    private final RegistryRuntimeSwitchPort runtimeSwitchPort;
    private final RegistryMaintenanceFencePort maintenanceFencePort;
    private final RegistryTrafficTransitionPort trafficTransitionPort;
    private final Clock clock;

    public RegistryCutoverFenceService(
        RegistryControlPort controlPort,
        List<RegistryRolloutPort> rolloutPorts,
        RegistryReplicaVerificationPort replicaVerificationPort,
        GetRegistryReadinessUseCase readinessUseCase,
        RegistryRuntimeSwitchPort runtimeSwitchPort,
        RegistryMaintenanceFencePort maintenanceFencePort,
        RegistryTrafficTransitionPort trafficTransitionPort,
        Clock clock
    ) {
        this.controlPort = controlPort;
        this.rolloutByRegistry = indexRollouts(rolloutPorts);
        this.replicaVerificationPort = replicaVerificationPort;
        this.readinessUseCase = readinessUseCase;
        this.runtimeSwitchPort = runtimeSwitchPort;
        this.maintenanceFencePort = maintenanceFencePort;
        this.trafficTransitionPort = trafficTransitionPort;
        this.clock = clock;
    }

    public boolean promoteReady(RegistryCutoverPlan cutoverPlan) {
        Objects.requireNonNull(cutoverPlan, "registry cutover plan은 필수입니다.");
        if (!allValidatedAndClean()) {
            runtimeSwitchPort.disableCleanupAndEnforcement();
            blockCutover("final reconciliation, replica, or namespace coverage failed");
            return false;
        }
        for (RegistryName registryName : RegistryName.values()) {
            transition(
                registryName,
                RegistryStatus.VALIDATED,
                RegistryStatus.READY,
                "final reconciliation and replica verification clean"
            );
        }
        runtimeSwitchPort.enableCleanupAndEnforcement();
        return true;
    }

    public boolean reconcileRuntimeFence() {
        if (allReadyAndClean()) {
            return true;
        }
        runtimeSwitchPort.disableCleanupAndEnforcement();
        blockCutover("runtime drift, replica mismatch, or namespace coverage failure");
        return false;
    }

    public void rollbackAndTransitionTraffic() {
        maintenanceFencePort.startStorageMaintenance();
        try {
            for (RegistryName registryName : RegistryName.values()) {
                disable(registryName);
            }
        } catch (RuntimeException exception) {
            runtimeSwitchPort.disableCleanupAndEnforcement();
            throw exception;
        }
        runtimeSwitchPort.disableCleanupAndEnforcement();
        trafficTransitionPort.transitionTraffic();
    }

    private boolean allValidatedAndClean() {
        for (RegistryName registryName : RegistryName.values()) {
            if (loadStatus(registryName) != RegistryStatus.VALIDATED || !isClean(registryName)) {
                return false;
            }
        }
        return true;
    }

    private boolean allReadyAndClean() {
        for (RegistryName registryName : RegistryName.values()) {
            if (loadStatus(registryName) != RegistryStatus.READY || !isClean(registryName)) {
                return false;
            }
        }
        return true;
    }

    private boolean isClean(RegistryName registryName) {
        try {
            RegistryRolloutPort rollout = rolloutByRegistry.get(registryName);
            if (rollout == null
                || !rollout.reconcile(RECONCILIATION_DETAIL_LIMIT).isClean()
                || !replicaVerificationPort.isClean(registryName.canonicalName())) {
                return false;
            }
            return registryName == RegistryName.STORAGE_USAGE
                || readinessUseCase.invalidNamespaces(registryName).isEmpty();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void blockCutover(String details) {
        for (RegistryName registryName : RegistryName.values()) {
            RegistryStatus status = loadStatus(registryName);
            if (status == RegistryStatus.VALIDATED || status == RegistryStatus.READY
                || status == RegistryStatus.BACKFILLING) {
                transition(registryName, status, RegistryStatus.BLOCKED, details);
            }
        }
    }

    private void disable(RegistryName registryName) {
        RegistryStatus status = loadStatus(registryName);
        if (status == RegistryStatus.DISABLED) {
            return;
        }
        if (status != RegistryStatus.BLOCKED) {
            transition(registryName, status, RegistryStatus.BLOCKED, "rollback fence");
        }
        transition(
            registryName,
            RegistryStatus.BLOCKED,
            RegistryStatus.DISABLED,
            "rollback; next forward resets checkpoints and performs full backfill"
        );
    }

    private RegistryStatus loadStatus(RegistryName registryName) {
        RegistryCutoverState state = controlPort.loadState(registryName.canonicalName());
        return state.status();
    }

    private void transition(
        RegistryName registryName,
        RegistryStatus expected,
        RegistryStatus target,
        String details
    ) {
        if (!controlPort.transition(
            registryName.canonicalName(),
            expected,
            target,
            target == RegistryStatus.READY ? clock.instant() : null,
            details
        )) {
            throw new IllegalStateException("registry cutover state transition이 실패했습니다.");
        }
    }

    private static Map<RegistryName, RegistryRolloutPort> indexRollouts(
        List<RegistryRolloutPort> rolloutPorts
    ) {
        Map<RegistryName, RegistryRolloutPort> indexed = new EnumMap<>(RegistryName.class);
        for (RegistryRolloutPort rollout : List.copyOf(rolloutPorts)) {
            RegistryName name = RegistryName.fromCanonicalName(rollout.registryName());
            if (indexed.putIfAbsent(name, rollout) != null) {
                throw new IllegalArgumentException("registry rollout evaluator가 중복되었습니다.");
            }
        }
        return Map.copyOf(indexed);
    }
}
