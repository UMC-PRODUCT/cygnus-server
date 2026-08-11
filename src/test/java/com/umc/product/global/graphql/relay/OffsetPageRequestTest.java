package com.umc.product.global.graphql.relay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@DisplayName("OffsetPageRequest — 임의 offset Pageable 구현")
class OffsetPageRequestTest {

    @Test
    void getOffset은_생성_시_전달한_offset을_그대로_반환한다() {
        // given
        OffsetPageRequest pageable = new OffsetPageRequest(25, 10);

        // when & then
        assertThat(pageable.getOffset()).isEqualTo(25);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    void getPageNumber는_offset을_limit으로_나눈_몫이다() {
        assertThat(new OffsetPageRequest(0, 10).getPageNumber()).isZero();
        assertThat(new OffsetPageRequest(25, 10).getPageNumber()).isEqualTo(2);
        assertThat(new OffsetPageRequest(30, 10).getPageNumber()).isEqualTo(3);
    }

    @Test
    void 음수_offset은_거부한다() {
        assertThatThrownBy(() -> new OffsetPageRequest(-1, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("offset must not be negative");
    }

    @Test
    void limit은_1_이상이어야_한다() {
        assertThatThrownBy(() -> new OffsetPageRequest(0, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("limit must be greater than 0");
        assertThatThrownBy(() -> new OffsetPageRequest(0, -5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("limit must be greater than 0");
    }

    @Test
    void next는_offset을_limit만큼_전진시킨다() {
        // given
        OffsetPageRequest pageable = new OffsetPageRequest(5, 10);

        // when
        Pageable next = pageable.next();

        // then
        assertThat(next.getOffset()).isEqualTo(15);
        assertThat(next.getPageSize()).isEqualTo(10);
    }

    @Test
    void previousOrFirst는_offset을_limit만큼_후퇴시킨다() {
        // given
        OffsetPageRequest pageable = new OffsetPageRequest(25, 10);

        // when
        Pageable previous = pageable.previousOrFirst();

        // then
        assertThat(previous.getOffset()).isEqualTo(15);
    }

    @Test
    void previousOrFirst는_limit보다_작은_offset에서_0으로_클램프한다() {
        // given — 페이지 경계에 정렬되지 않은 offset
        OffsetPageRequest pageable = new OffsetPageRequest(5, 10);

        // when
        Pageable previous = pageable.previousOrFirst();

        // then
        assertThat(previous.getOffset()).isZero();
    }

    @Test
    void previousOrFirst는_첫_페이지에서_첫_페이지를_반환한다() {
        // given
        OffsetPageRequest pageable = new OffsetPageRequest(0, 10);

        // when
        Pageable previous = pageable.previousOrFirst();

        // then
        assertThat(previous.getOffset()).isZero();
        assertThat(previous.getPageSize()).isEqualTo(10);
    }

    @Test
    void hasPrevious는_offset이_양수일_때만_true다() {
        assertThat(new OffsetPageRequest(0, 10).hasPrevious()).isFalse();
        assertThat(new OffsetPageRequest(1, 10).hasPrevious()).isTrue();
    }

    @Test
    void first는_offset_0으로_돌아간다() {
        // given
        OffsetPageRequest pageable = new OffsetPageRequest(42, 10);

        // when
        Pageable first = pageable.first();

        // then
        assertThat(first.getOffset()).isZero();
        assertThat(first.getPageSize()).isEqualTo(10);
    }

    @Test
    void withPage는_페이지_번호_곱하기_limit을_offset으로_사용한다() {
        // given
        OffsetPageRequest pageable = new OffsetPageRequest(7, 10);

        // when
        Pageable paged = pageable.withPage(3);

        // then
        assertThat(paged.getOffset()).isEqualTo(30);
        assertThat(paged.getPageSize()).isEqualTo(10);
    }

    @Test
    void 정렬은_기본_unsorted이고_null이면_unsorted로_보정한다() {
        assertThat(new OffsetPageRequest(0, 10).getSort()).isEqualTo(Sort.unsorted());
        assertThat(new OffsetPageRequest(0, 10, null).getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void 정렬은_파생_Pageable에도_유지된다() {
        // given
        Sort sort = Sort.by("id").descending();
        OffsetPageRequest pageable = new OffsetPageRequest(10, 10, sort);

        // when & then
        assertThat(pageable.getSort()).isEqualTo(sort);
        assertThat(pageable.next().getSort()).isEqualTo(sort);
        assertThat(pageable.previousOrFirst().getSort()).isEqualTo(sort);
        assertThat(pageable.first().getSort()).isEqualTo(sort);
        assertThat(pageable.withPage(2).getSort()).isEqualTo(sort);
    }
}
