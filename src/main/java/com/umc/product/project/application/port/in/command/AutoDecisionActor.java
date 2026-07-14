package com.umc.product.project.application.port.in.command;

import java.util.Objects;
import java.util.Optional;

public sealed interface AutoDecisionActor
    permits AutoDecisionActor.Member, AutoDecisionActor.SystemActor {

    String MATCHING_ROUND_SCHEDULER_ID = "matching-round-scheduler";

    static AutoDecisionActor matchingRoundScheduler() {
        return new SystemActor(MATCHING_ROUND_SCHEDULER_ID);
    }

    Optional<Long> memberId();

    record Member(long value) implements AutoDecisionActor {
        @Override
        public Optional<Long> memberId() {
            return Optional.of(value);
        }
    }

    record SystemActor(String systemId) implements AutoDecisionActor {
        public SystemActor {
            Objects.requireNonNull(systemId, "systemId must not be null");
            if (!MATCHING_ROUND_SCHEDULER_ID.equals(systemId)) {
                throw new IllegalArgumentException("등록되지 않은 매칭 자동 선발 systemId입니다.");
            }
        }

        @Override
        public Optional<Long> memberId() {
            return Optional.empty();
        }
    }
}
