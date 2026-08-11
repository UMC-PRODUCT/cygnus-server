package com.umc.product.global.graphql.relay;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@DisplayName("RelayConnection — Connection 변환(fromPage/fromList)")
class RelayConnectionTest {

    @Test
    void fromPage는_Pageable_offset_기준으로_오프셋_커서를_이어_붙인다() {
        // given — offset 5부터 3건, 전체 20건인 중간 페이지
        Page<Long> page = new PageImpl<>(List.of(10L, 20L, 30L), new OffsetPageRequest(5, 3), 20);

        // when
        ConnectionArguments arguments = ConnectionArguments.of(3, RelayCursor.encodeOffset(4), null, null);
        RelayConnection<String> connection = RelayConnection.fromPage(page, arguments, value -> "node-" + value);

        // then
        assertThat(connection.edges()).hasSize(3);
        assertThat(connection.edges().get(0).cursor()).isEqualTo(RelayCursor.encodeOffset(5));
        assertThat(connection.edges().get(1).cursor()).isEqualTo(RelayCursor.encodeOffset(6));
        assertThat(connection.edges().get(2).cursor()).isEqualTo(RelayCursor.encodeOffset(7));
        assertThat(connection.edges().get(0).node()).isEqualTo("node-10");
        assertThat(connection.edges().get(2).node()).isEqualTo("node-30");
        assertThat(connection.totalCount()).isEqualTo(20);
    }

    @Test
    void fromPage_중간_페이지는_양방향_페이지가_모두_존재한다() {
        // given
        Page<Long> page = new PageImpl<>(List.of(10L, 20L, 30L), new OffsetPageRequest(5, 3), 20);

        // when
        ConnectionArguments arguments = ConnectionArguments.of(3, RelayCursor.encodeOffset(4), null, null);
        RelayConnection<String> connection = RelayConnection.fromPage(page, arguments, value -> "node-" + value);

        // then
        assertThat(connection.pageInfo().hasNextPage()).isTrue();
        assertThat(connection.pageInfo().hasPreviousPage()).isTrue();
        assertThat(connection.pageInfo().startCursor()).isEqualTo(RelayCursor.encodeOffset(5));
        assertThat(connection.pageInfo().endCursor()).isEqualTo(RelayCursor.encodeOffset(7));
    }

    @Test
    void fromPage_전체를_담은_첫_페이지는_양방향_페이지가_없다() {
        // given
        Page<Long> page = new PageImpl<>(List.of(1L, 2L, 3L), new OffsetPageRequest(0, 5), 3);

        // when
        ConnectionArguments arguments = ConnectionArguments.of(5, null, null, null);
        RelayConnection<String> connection = RelayConnection.fromPage(page, arguments, value -> "node-" + value);

        // then
        assertThat(connection.pageInfo().hasNextPage()).isFalse();
        assertThat(connection.pageInfo().hasPreviousPage()).isFalse();
        assertThat(connection.pageInfo().startCursor()).isEqualTo(RelayCursor.encodeOffset(0));
        assertThat(connection.pageInfo().endCursor()).isEqualTo(RelayCursor.encodeOffset(2));
        assertThat(connection.totalCount()).isEqualTo(3);
    }

    @Test
    void fromPage_빈_페이지는_커서_없이_빈_edges를_반환한다() {
        // given
        Page<Long> page = new PageImpl<>(List.of(), new OffsetPageRequest(0, 5), 0);

        // when
        ConnectionArguments arguments = ConnectionArguments.of(5, null, null, null);
        RelayConnection<String> connection = RelayConnection.fromPage(page, arguments, value -> "node-" + value);

        // then
        assertThat(connection.edges()).isEmpty();
        assertThat(connection.totalCount()).isZero();
        assertThat(connection.pageInfo().hasNextPage()).isFalse();
        assertThat(connection.pageInfo().hasPreviousPage()).isFalse();
        assertThat(connection.pageInfo().startCursor()).isNull();
        assertThat(connection.pageInfo().endCursor()).isNull();
    }

    @Test
    void fromPage_unpaged_페이지는_offset_0부터_커서를_매긴다() {
        // given
        Page<Long> page = new PageImpl<>(List.of(1L, 2L), Pageable.unpaged(), 2);

        // when
        ConnectionArguments arguments = ConnectionArguments.of(2, null, null, null);
        RelayConnection<String> connection = RelayConnection.fromPage(page, arguments, value -> "node-" + value);

        // then
        assertThat(connection.edges().get(0).cursor()).isEqualTo(RelayCursor.encodeOffset(0));
        assertThat(connection.edges().get(1).cursor()).isEqualTo(RelayCursor.encodeOffset(1));
        assertThat(connection.pageInfo().hasPreviousPage()).isFalse();
    }

    @Test
    void fromPage는_first_0의_보정_조회_결과를_edges에_노출하지_않는다() {
        // given — Pageable 제약 때문에 1건을 읽었지만 GraphQL 요청은 first=0이다.
        ConnectionArguments arguments = ConnectionArguments.of(0, null, null, null);
        Page<Long> page = new PageImpl<>(List.of(1L), new OffsetPageRequest(0, 1), 3);

        // when
        RelayConnection<String> connection = RelayConnection.fromPage(page, arguments, Object::toString);

        // then
        assertThat(connection.edges()).isEmpty();
        assertThat(connection.pageInfo().hasNextPage()).isTrue();
        assertThat(connection.pageInfo().hasPreviousPage()).isFalse();
        assertThat(connection.pageInfo().startCursor()).isNull();
        assertThat(connection.pageInfo().endCursor()).isNull();
        assertThat(connection.totalCount()).isEqualTo(3);
    }

    @Test
    void fromPage는_before_없는_last를_전체_크기_기준으로_변환한다() {
        // given — 전체 6건의 마지막 2건을 조회한 Page다.
        ConnectionArguments arguments = ConnectionArguments.of(null, null, 2, null);
        Page<Long> page = new PageImpl<>(List.of(5L, 6L), new OffsetPageRequest(4, 2), 6);

        // when
        RelayConnection<String> connection = RelayConnection.fromPage(page, arguments, Object::toString);

        // then
        assertThat(connection.edges()).extracting(RelayEdge::cursor)
            .containsExactly(RelayCursor.encodeOffset(4), RelayCursor.encodeOffset(5));
        assertThat(connection.pageInfo().hasNextPage()).isFalse();
        assertThat(connection.pageInfo().hasPreviousPage()).isTrue();
    }

    @Test
    void fromList는_first와_after로_중간을_슬라이싱한다() {
        // given — 10건 중 after(4) 다음부터 3건
        List<String> items = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        ConnectionArguments arguments = ConnectionArguments.of(3, RelayCursor.encodeOffset(4), null, null);

        // when
        RelayConnection<String> connection = RelayConnection.fromList(items, arguments, String::toUpperCase);

        // then
        assertThat(connection.edges()).hasSize(3);
        assertThat(connection.edges().get(0).cursor()).isEqualTo(RelayCursor.encodeOffset(5));
        assertThat(connection.edges().get(0).node()).isEqualTo("F");
        assertThat(connection.edges().get(2).cursor()).isEqualTo(RelayCursor.encodeOffset(7));
        assertThat(connection.edges().get(2).node()).isEqualTo("H");
        assertThat(connection.pageInfo().hasNextPage()).isTrue();
        assertThat(connection.pageInfo().hasPreviousPage()).isTrue();
        assertThat(connection.totalCount()).isEqualTo(10);
    }

    @Test
    void fromList는_before_없는_last를_전체_크기_기준으로_계산한다() {
        // given — 6건의 끝에서 2건
        List<String> items = List.of("a", "b", "c", "d", "e", "f");
        ConnectionArguments arguments = ConnectionArguments.of(null, null, 2, null);

        // when
        RelayConnection<String> connection = RelayConnection.fromList(items, arguments, String::toUpperCase);

        // then
        assertThat(connection.edges()).hasSize(2);
        assertThat(connection.edges().get(0).cursor()).isEqualTo(RelayCursor.encodeOffset(4));
        assertThat(connection.edges().get(0).node()).isEqualTo("E");
        assertThat(connection.edges().get(1).node()).isEqualTo("F");
        assertThat(connection.pageInfo().hasNextPage()).isFalse();
        assertThat(connection.pageInfo().hasPreviousPage()).isTrue();
    }

    @Test
    void fromList는_first를_적용한_결과에_last를_순서대로_적용한다() {
        // given — first 5건 중 마지막 2건은 offset 3, 4다.
        List<String> items = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        ConnectionArguments arguments = ConnectionArguments.of(5, null, 2, null);

        // when
        RelayConnection<String> connection = RelayConnection.fromList(items, arguments, String::toUpperCase);

        // then
        assertThat(connection.edges()).extracting(RelayEdge::node).containsExactly("D", "E");
        assertThat(connection.pageInfo().hasNextPage()).isTrue();
        assertThat(connection.pageInfo().hasPreviousPage()).isTrue();
    }

    @Test
    void fromList_빈_목록은_커서_없이_빈_Connection을_반환한다() {
        // given
        List<String> items = List.of();
        ConnectionArguments arguments = ConnectionArguments.of(null, null, null, null);

        // when
        RelayConnection<String> connection = RelayConnection.fromList(items, arguments, String::toUpperCase);

        // then
        assertThat(connection.edges()).isEmpty();
        assertThat(connection.totalCount()).isZero();
        assertThat(connection.pageInfo().hasNextPage()).isFalse();
        assertThat(connection.pageInfo().hasPreviousPage()).isFalse();
        assertThat(connection.pageInfo().startCursor()).isNull();
        assertThat(connection.pageInfo().endCursor()).isNull();
    }

    @Test
    void fromList_범위_밖_offset은_빈_edges를_반환한다() {
        // given — 3건뿐인데 after(9)부터 요청
        List<String> items = List.of("a", "b", "c");
        ConnectionArguments arguments = ConnectionArguments.of(5, RelayCursor.encodeOffset(9), null, null);

        // when
        RelayConnection<String> connection = RelayConnection.fromList(items, arguments, String::toUpperCase);

        // then
        assertThat(connection.edges()).isEmpty();
        assertThat(connection.totalCount()).isEqualTo(3);
        assertThat(connection.pageInfo().hasNextPage()).isFalse();
        assertThat(connection.pageInfo().hasPreviousPage()).isTrue();
        assertThat(connection.pageInfo().startCursor()).isNull();
        assertThat(connection.pageInfo().endCursor()).isNull();
    }

    @Test
    void fromList_끝부분_슬라이스는_남은_만큼만_반환하고_다음_페이지가_없다() {
        // given — 5건 중 after(2) 다음부터 first=10 요청 → 2건만 남는다.
        List<String> items = List.of("a", "b", "c", "d", "e");
        ConnectionArguments arguments = ConnectionArguments.of(10, RelayCursor.encodeOffset(2), null, null);

        // when
        RelayConnection<String> connection = RelayConnection.fromList(items, arguments, String::toUpperCase);

        // then
        assertThat(connection.edges()).hasSize(2);
        assertThat(connection.edges().get(0).cursor()).isEqualTo(RelayCursor.encodeOffset(3));
        assertThat(connection.edges().get(1).cursor()).isEqualTo(RelayCursor.encodeOffset(4));
        assertThat(connection.pageInfo().hasNextPage()).isFalse();
        assertThat(connection.pageInfo().hasPreviousPage()).isTrue();
        assertThat(connection.totalCount()).isEqualTo(5);
    }
}
