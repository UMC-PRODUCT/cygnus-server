package com.umc.product.analytics.application.port.in.query.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;

@Builder
public record AdminOperationsCommunityActivityInfo(
    List<ActivityBucketInfo> buckets,
    long totalPostCount,
    long totalCommentCount
) {

    public static AdminOperationsCommunityActivityInfo of(
        List<ActivityBucketInfo> buckets,
        long totalPostCount,
        long totalCommentCount
    ) {
        return AdminOperationsCommunityActivityInfo.builder()
            .buckets(List.copyOf(buckets))
            .totalPostCount(totalPostCount)
            .totalCommentCount(totalCommentCount)
            .build();
    }

    @Builder
    public record ActivityBucketInfo(LocalDate date, long postCount, long commentCount) {

        public static ActivityBucketInfo of(LocalDate date, long postCount, long commentCount) {
            return ActivityBucketInfo.builder()
                .date(date)
                .postCount(postCount)
                .commentCount(commentCount)
                .build();
        }
    }
}
