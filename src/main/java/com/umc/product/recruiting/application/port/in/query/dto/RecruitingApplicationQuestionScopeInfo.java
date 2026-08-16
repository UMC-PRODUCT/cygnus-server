package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.Set;

public record RecruitingApplicationQuestionScopeInfo(
    Set<Long> allowedQuestionIds,
    Set<Long> requiredQuestionIds
) {

    public RecruitingApplicationQuestionScopeInfo {
        allowedQuestionIds = Set.copyOf(allowedQuestionIds);
        requiredQuestionIds = Set.copyOf(requiredQuestionIds);
    }
}
