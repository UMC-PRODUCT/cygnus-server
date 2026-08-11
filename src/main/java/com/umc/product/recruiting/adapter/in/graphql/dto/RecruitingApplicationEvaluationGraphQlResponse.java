package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationEvaluationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record RecruitingApplicationEvaluationGraphQlResponse(
    String evaluationId,
    String applicationId,
    String evaluatorMemberId,
    RecruitingEvaluatorStage stage,
    RecruitingApplicationEvaluationDecision decision,
    String comment,
    Instant submittedAt
) {

    public static RecruitingApplicationEvaluationGraphQlResponse from(RecruitingApplicationEvaluationInfo info) {
        return new RecruitingApplicationEvaluationGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION_EVALUATION, info.id()),
            GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, info.applicationId()),
            GlobalId.encode(GlobalIdTypes.MEMBER, info.evaluatorMemberId()),
            info.stage(),
            info.decision(),
            info.comment(),
            info.submittedAt()
        );
    }
}
