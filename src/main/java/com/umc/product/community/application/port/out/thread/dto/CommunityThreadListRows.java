package com.umc.product.community.application.port.out.thread.dto;

import java.util.List;

public record CommunityThreadListRows(
    List<CommunityThreadQueryRow> pinned,
    List<CommunityThreadQueryRow> unpinned,
    long unpinnedTotal
) {

    public CommunityThreadListRows {
        pinned = pinned == null ? List.of() : List.copyOf(pinned);
        unpinned = unpinned == null ? List.of() : List.copyOf(unpinned);
        if (unpinnedTotal < 0) {
            throw new IllegalArgumentException("unpinnedTotal must not be negative");
        }
    }
}
