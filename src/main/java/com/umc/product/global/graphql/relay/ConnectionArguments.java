package com.umc.product.global.graphql.relay;

import java.util.function.LongSupplier;

import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

/**
 * Relay Connection 스펙의 forward/backward 페이지네이션 인자.
 * cursor를 적용한 뒤 first, last 순서로 슬라이싱하여 offset/limit 윈도우로 변환한다.
 */
public record ConnectionArguments(
    Integer first,
    String after,
    Integer last,
    String before
) {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private static final long MAX_OFFSET = 10_000L;

    public static ConnectionArguments of(
        @Nullable Integer first,
        @Nullable String after,
        @Nullable Integer last,
        @Nullable String before
    ) {
        if (first != null && (first < 0 || first > MAX_PAGE_SIZE)) {
            throw new IllegalArgumentException("first must be between 0 and " + MAX_PAGE_SIZE);
        }
        if (last != null && (last < 0 || last > MAX_PAGE_SIZE)) {
            throw new IllegalArgumentException("last must be between 0 and " + MAX_PAGE_SIZE);
        }
        if (first == null && last == null) {
            first = DEFAULT_PAGE_SIZE;
        }
        return new ConnectionArguments(first, after, last, before);
    }

    /**
     * offset 기반 저장소 조회용 윈도우. 전체 크기를 모르는 경우 totalSize에 null을 넘기며,
     * 이때 last는 before와 함께 사용해야 한다.
     */
    public OffsetLimit toOffsetLimit(@Nullable Long totalSize) {
        long start = afterOffset();
        Long end = beforeOffset();
        if (totalSize != null) {
            start = Math.min(start, totalSize);
            end = end == null ? totalSize : Math.min(end, totalSize);
        }
        if (end != null && end < start) {
            end = start;
        }

        if (end == null && first == null) {
            throw new IllegalArgumentException("last requires before or the connection total size");
        }
        long slicedEnd = end == null ? start + first : end;
        if (first != null) {
            slicedEnd = Math.min(slicedEnd, start + first);
        }

        long offset = last == null ? start : Math.max(start, slicedEnd - last);
        int limit = Math.toIntExact(Math.max(slicedEnd - offset, 0));
        if (offset > MAX_OFFSET) {
            throw new IllegalArgumentException("pagination offset must be less than or equal to " + MAX_OFFSET);
        }
        return new OffsetLimit(offset, limit);
    }

    public Pageable toPageable() {
        OffsetLimit offsetLimit = toOffsetLimit(null);
        return new OffsetPageRequest(offsetLimit.offset(), Math.max(offsetLimit.limit(), 1));
    }

    /**
     * 전체 크기가 필요한 backward 페이지네이션이면 공급자로 크기를 구한 뒤 Pageable을 만든다.
     * forward 페이지네이션이나 before가 있는 backward 페이지네이션에서는 공급자를 호출하지 않는다.
     */
    public Pageable toPageable(LongSupplier totalSizeSupplier) {
        Long totalSize = requiresTotalSize() ? totalSizeSupplier.getAsLong() : null;
        OffsetLimit offsetLimit = toOffsetLimit(totalSize);
        return new OffsetPageRequest(offsetLimit.offset(), Math.max(offsetLimit.limit(), 1));
    }

    public boolean requiresTotalSize() {
        return last != null && before == null;
    }

    /**
     * 전체 크기를 아는 상태에서 최종 edge 범위와 PageInfo 플래그를 계산한다.
     */
    public ConnectionWindow toWindow(long totalSize) {
        if (totalSize < 0) {
            throw new IllegalArgumentException("totalSize must not be negative");
        }

        long cursorStart = Math.min(afterOffset(), totalSize);
        Long requestedEnd = beforeOffset();
        long cursorEnd = requestedEnd == null ? totalSize : Math.min(requestedEnd, totalSize);
        if (cursorEnd < cursorStart) {
            cursorEnd = cursorStart;
        }

        long firstSlicedEnd = first == null
            ? cursorEnd
            : Math.min(cursorEnd, cursorStart + first);
        long finalStart = last == null
            ? cursorStart
            : Math.max(cursorStart, firstSlicedEnd - last);
        int limit = Math.toIntExact(firstSlicedEnd - finalStart);

        if (finalStart > MAX_OFFSET) {
            throw new IllegalArgumentException("pagination offset must be less than or equal to " + MAX_OFFSET);
        }

        boolean hasPreviousPage = last != null
            ? cursorEnd - cursorStart > last
            : after != null && cursorStart > 0;
        boolean hasNextPage = first != null
            ? cursorEnd - cursorStart > first
            : before != null && cursorEnd < totalSize;

        return new ConnectionWindow(finalStart, limit, hasNextPage, hasPreviousPage);
    }

    private long afterOffset() {
        if (after == null) {
            return 0;
        }
        long offset = RelayCursor.decodeOffset(after);
        if (offset >= MAX_OFFSET) {
            throw offsetLimitException();
        }
        return offset + 1;
    }

    private Long beforeOffset() {
        if (before == null) {
            return null;
        }
        long offset = RelayCursor.decodeOffset(before);
        if (offset > MAX_OFFSET) {
            throw offsetLimitException();
        }
        return offset;
    }

    private IllegalArgumentException offsetLimitException() {
        return new IllegalArgumentException(
            "pagination offset must be less than or equal to " + MAX_OFFSET
        );
    }

    public record OffsetLimit(long offset, int limit) {
    }

    public record ConnectionWindow(
        long offset,
        int limit,
        boolean hasNextPage,
        boolean hasPreviousPage
    ) {
    }
}
