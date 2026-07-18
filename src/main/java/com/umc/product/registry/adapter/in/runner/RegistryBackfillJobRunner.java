package com.umc.product.registry.adapter.in.runner;

import java.util.Objects;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import com.umc.product.registry.application.service.RegistryBackfillCoordinator;
import com.umc.product.registry.domain.RegistryBackfillAction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegistryBackfillJobRunner implements ApplicationRunner {

    private final RegistryBackfillCoordinator coordinator;
    private final RegistryBackfillAction action;
    private final ProcessTerminator processTerminator;

    public RegistryBackfillJobRunner(
        RegistryBackfillCoordinator coordinator,
        RegistryBackfillAction action,
        ProcessTerminator processTerminator
    ) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator는 필수입니다.");
        this.action = Objects.requireNonNull(action, "backfill action은 필수입니다.");
        this.processTerminator = Objects.requireNonNull(
            processTerminator,
            "process terminator는 필수입니다."
        );
    }

    @Override
    public void run(ApplicationArguments args) {
        coordinator.execute(action).forEach(result ->
            log.info("Registry rollout을 수행했습니다: action={}, {}", action, result.summary()));
        processTerminator.terminate();
    }

    @FunctionalInterface
    public interface ProcessTerminator {

        void terminate();
    }
}
