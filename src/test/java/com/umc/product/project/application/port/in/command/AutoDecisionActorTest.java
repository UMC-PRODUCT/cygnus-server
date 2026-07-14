package com.umc.product.project.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutoDecisionActorTest {

    @Test
    @DisplayName("회원 actor는 회원 ID를 타입으로 보존한다")
    void memberActorKeepsMemberId() {
        AutoDecisionActor actor = new AutoDecisionActor.Member(42L);

        assertThat(actor.memberId()).contains(42L);
    }

    @Test
    @DisplayName("스케줄러 actor는 고정된 system ID로만 생성된다")
    void schedulerActorUsesFixedSystemId() {
        AutoDecisionActor actor = AutoDecisionActor.matchingRoundScheduler();

        assertThat(actor).isEqualTo(
            new AutoDecisionActor.SystemActor(AutoDecisionActor.MATCHING_ROUND_SCHEDULER_ID)
        );
        assertThat(actor.memberId()).isEmpty();
    }

    @Test
    @DisplayName("위조된 system ID와 null system ID는 거부한다")
    void rejectsForgedOrNullSystemId() {
        assertThatThrownBy(() -> new AutoDecisionActor.SystemActor("forged-scheduler"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatNullPointerException()
            .isThrownBy(() -> new AutoDecisionActor.SystemActor(null));
    }
}
