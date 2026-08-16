package com.umc.product.project.adapter.in.graphql.dto;

import com.umc.product.project.domain.enums.MatchingPhase;

public enum MatchingRoundPhaseView {
    FIRST,
    SECOND,
    THIRD,
    RANDOM_MATCHING;

    public static MatchingRoundPhaseView from(MatchingPhase phase) {
        return switch (phase) {
            case FIRST -> FIRST;
            case SECOND -> SECOND;
            case THIRD -> THIRD;
        };
    }
}
