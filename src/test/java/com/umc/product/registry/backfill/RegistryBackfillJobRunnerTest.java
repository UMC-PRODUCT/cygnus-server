package com.umc.product.registry.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.umc.product.registry.adapter.in.runner.RegistryBackfillJobRunner;
import com.umc.product.registry.adapter.in.runner.RegistryBackfillRunnerConfiguration;
import com.umc.product.registry.application.service.RegistryBackfillCoordinator;
import com.umc.product.registry.domain.RegistryBackfillAction;
import com.umc.product.registry.domain.RegistryReconciliationResult;

class RegistryBackfillJobRunnerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(RegistryBackfillRunnerConfiguration.class);

    @Test
    void 기본_profile에는_one_shot_runner_bean이_없다() {
        contextRunner.run(context ->
            assertThat(context).doesNotHaveBean(RegistryBackfillJobRunner.class));
    }

    @Test
    void registry_backfill_profile과_action이_있을_때만_runner를_생성한다() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=registry-backfill",
                "app.registry.backfill.action=RECONCILE")
            .withBean(RegistryBackfillCoordinator.class, () -> mock(RegistryBackfillCoordinator.class))
            .run(context -> assertThat(context).hasSingleBean(RegistryBackfillJobRunner.class));
    }

    @Test
    void runner는_설정된_backfill_action을_coordinator에_한번_전달한다() throws Exception {
        // given
        RegistryBackfillCoordinator coordinator = mock(RegistryBackfillCoordinator.class);
        given(coordinator.execute(RegistryBackfillAction.BACKFILL)).willReturn(List.of(
            new RegistryReconciliationResult("file-usage", List.of())
        ));
        AtomicBoolean terminated = new AtomicBoolean();
        RegistryBackfillJobRunner runner = new RegistryBackfillJobRunner(
            coordinator,
            RegistryBackfillAction.BACKFILL,
            () -> terminated.set(true)
        );

        // when
        runner.run(new DefaultApplicationArguments());

        // then
        verify(coordinator).execute(RegistryBackfillAction.BACKFILL);
        assertThat(terminated).isTrue();
    }

    @Test
    void backfill_drift_실패는_종료기로_삼키지_않고_예외를_전파한다() {
        // given
        RegistryBackfillCoordinator coordinator = mock(RegistryBackfillCoordinator.class);
        IllegalStateException drift = new IllegalStateException("registry reconcile drift");
        given(coordinator.execute(RegistryBackfillAction.BACKFILL)).willThrow(drift);
        AtomicBoolean terminated = new AtomicBoolean();
        RegistryBackfillJobRunner runner = new RegistryBackfillJobRunner(
            coordinator,
            RegistryBackfillAction.BACKFILL,
            () -> terminated.set(true)
        );

        // when & then
        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
            .isSameAs(drift);
        assertThat(terminated).isFalse();
    }
}
