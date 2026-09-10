package com.umc.product.analytics.adapter.in.web.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsCommunityActivityInfo;

import lombok.Builder;

@Builder
public record AdminOperationsCommunityActivityResponse(
    List<ActivityBucketResponse> buckets,
    long totalPostCount,
    long totalCommentCount
) {

    public static AdminOperationsCommunityActivityResponse from(AdminOperationsCommunityActivityInfo info) {
        return AdminOperationsCommunityActivityResponse.builder()
            .buckets(info.buckets().stream().map(ActivityBucketResponse::from).toList())
            .totalPostCount(info.totalPostCount())
            .totalCommentCount(info.totalCommentCount())
            .build();
    }

    @Builder
    public record ActivityBucketResponse(LocalDate date, long postCount, long commentCount) {

        public static ActivityBucketResponse from(AdminOperationsCommunityActivityInfo.ActivityBucketInfo info) {
            return ActivityBucketResponse.builder()
                .date(info.date())
                .postCount(info.postCount())
                .commentCount(info.commentCount())
                .build();
        }
    }
}
