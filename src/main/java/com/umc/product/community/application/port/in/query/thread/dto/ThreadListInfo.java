package com.umc.product.community.application.port.in.query.thread.dto;

import java.util.List;

public record ThreadListInfo(
    List<ThreadSummaryInfo> pinned,
    List<ThreadSummaryInfo> threads,
    Integer nextOffset,
    long total
) {

    public ThreadListInfo {
        pinned = pinned == null ? List.of() : List.copyOf(pinned);
        threads = threads == null ? List.of() : List.copyOf(threads);
        if (nextOffset != null && nextOffset < 0) {
            throw new IllegalArgumentException("nextOffset must not be negative");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
    }
}
