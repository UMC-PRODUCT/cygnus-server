package com.umc.product.demoday.application.port.in.query.dto;

import java.util.List;

public record DemodayAdminVoteListInfo(
    Long pollId,
    List<DemodayAdminVoteInfo> content,
    Long nextCursor,
    boolean hasNext
) {

    public static DemodayAdminVoteListInfo empty(Long pollId) {
        return new DemodayAdminVoteListInfo(pollId, List.of(), null, false);
    }
}
