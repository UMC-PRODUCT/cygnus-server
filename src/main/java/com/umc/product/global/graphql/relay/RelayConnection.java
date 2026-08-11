package com.umc.product.global.graphql.relay;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;

/**
 * Relay Connection 스펙의 Connection 타입. 스키마의 {@code XConnection} 타입에 대응한다.
 * totalCount는 스펙이 허용하는 확장 필드다.
 */
public record RelayConnection<T>(
    List<RelayEdge<T>> edges,
    RelayPageInfo pageInfo,
    long totalCount
) {

    /**
     * 요청 인자까지 반영해 Page를 Connection으로 변환한다. Pageable 제약 때문에 first=0을
     * 1건 조회로 보정한 경우에도 edge는 정확히 0건을 반환한다.
     */
    public static <S, T> RelayConnection<T> fromPage(
        Page<S> page,
        ConnectionArguments arguments,
        Function<S, T> mapper
    ) {
        ConnectionArguments.ConnectionWindow window = arguments.toWindow(page.getTotalElements());
        int edgeCount = Math.min(window.limit(), page.getContent().size());
        List<RelayEdge<T>> edges = IntStream.range(0, edgeCount)
            .mapToObj(index -> new RelayEdge<>(
                RelayCursor.encodeOffset(window.offset() + index),
                mapper.apply(page.getContent().get(index))
            ))
            .toList();
        return new RelayConnection<>(
            edges,
            pageInfo(edges, window.hasNextPage(), window.hasPreviousPage()),
            page.getTotalElements()
        );
    }

    /**
     * 전체 목록을 Relay 스펙의 슬라이싱 알고리즘으로 잘라 Connection으로 변환한다.
     */
    public static <S, T> RelayConnection<T> fromList(
        List<S> items,
        ConnectionArguments arguments,
        Function<S, T> mapper
    ) {
        ConnectionArguments.ConnectionWindow window = arguments.toWindow(items.size());
        int fromIndex = (int)window.offset();
        int toIndex = (int)Math.min(window.offset() + window.limit(), items.size());
        List<RelayEdge<T>> edges = IntStream.range(fromIndex, toIndex)
            .mapToObj(index -> new RelayEdge<>(
                RelayCursor.encodeOffset(index),
                mapper.apply(items.get(index))
            ))
            .toList();
        return new RelayConnection<>(
            edges,
            pageInfo(edges, window.hasNextPage(), window.hasPreviousPage()),
            items.size()
        );
    }

    private static <T> RelayPageInfo pageInfo(
        List<RelayEdge<T>> edges,
        boolean hasNextPage,
        boolean hasPreviousPage
    ) {
        return new RelayPageInfo(
            hasNextPage,
            hasPreviousPage,
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor()
        );
    }

}
