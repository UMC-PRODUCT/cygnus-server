package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationDetailInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;

public record RecruitingApplicationReviewGraphQlResponse(
    Long applicationId,
    String applicantName,
    String applicantEmail,
    Long applicantMemberId,
    Instant submittedAt,
    Long formResponseId,
    boolean documentEvaluatedByMe,
    boolean interviewEvaluatedByMe,
    List<RecruitingApplicationReviewDetailGraphQlResponse.Answer> answers
) {

    public static RecruitingApplicationReviewGraphQlResponse from(RecruitingApplicationSummaryInfo info) {
        return new RecruitingApplicationReviewGraphQlResponse(
            info.applicationId(),
            info.applicantName(),
            info.email(),
            info.applicantMemberId(),
            info.submittedAt(),
            null,
            info.documentEvaluatedByMe(),
            info.interviewEvaluatedByMe(),
            List.of()
        );
    }

    public static RecruitingApplicationReviewGraphQlResponse from(RecruitingApplicationDetailInfo info) {
        RecruitingApplicationReviewDetailGraphQlResponse detail =
            RecruitingApplicationReviewDetailGraphQlResponse.from(info);
        RecruitingApplicationSummaryInfo application = info.application();
        return new RecruitingApplicationReviewGraphQlResponse(
            application.applicationId(),
            application.applicantName(),
            application.email(),
            application.applicantMemberId(),
            application.submittedAt(),
            detail.formResponseId(),
            application.documentEvaluatedByMe(),
            application.interviewEvaluatedByMe(),
            detail.answers()
        );
    }
}
