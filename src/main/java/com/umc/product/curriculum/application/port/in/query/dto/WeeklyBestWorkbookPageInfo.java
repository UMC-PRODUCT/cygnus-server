package com.umc.product.curriculum.application.port.in.query.dto;

import java.util.List;

public record WeeklyBestWorkbookPageInfo(
    List<WeeklyBestWorkbookInfo> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {
    public static WeeklyBestWorkbookPageInfo empty(int page, int size) {
        return new WeeklyBestWorkbookPageInfo(List.of(), page, size, 0, 0, false, page > 0);
    }
}
