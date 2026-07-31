package com.umc.product.community.application.port.in.query.thread.message.dto;

public record CommunityThreadMessageMutationInfo(
    CommunityThreadMessageInfo message,
    boolean deduplicated
) {
}
