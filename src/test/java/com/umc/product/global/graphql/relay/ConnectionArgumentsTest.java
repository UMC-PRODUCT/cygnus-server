package com.umc.product.global.graphql.relay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

@DisplayName("ConnectionArguments — Relay 페이지네이션 인자 검증/오프셋 변환")
class ConnectionArgumentsTest {

    @Test
    void first와_last를_동시에_지정하면_first_이후_last_순서로_슬라이싱한다() {
        // given
        ConnectionArguments arguments = ConnectionArguments.of(5, null, 2, null);

        // when
        ConnectionArguments.ConnectionWindow window = arguments.toWindow(10);

        // then — 전체 10건에서 first 5건을 남긴 뒤 그중 마지막 2건을 반환한다.
        assertThat(window.offset()).isEqualTo(3);
        assertThat(window.limit()).isEqualTo(2);
        assertThat(window.hasNextPage()).isTrue();
        assertThat(window.hasPreviousPage()).isTrue();
    }

    @Test
    void first가_음수이거나_최대치를_넘으면_예외가_발생한다() {
        assertThatThrownBy(() -> ConnectionArguments.of(-1, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("first must be between 0 and 100");
        assertThatThrownBy(() -> ConnectionArguments.of(101, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("first must be between 0 and 100");
    }

    @Test
    void last가_음수이거나_최대치를_넘으면_예외가_발생한다() {
        assertThatThrownBy(() -> ConnectionArguments.of(null, null, -1, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("last must be between 0 and 100");
        assertThatThrownBy(() -> ConnectionArguments.of(null, null, 101, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("last must be between 0 and 100");
    }

    @Test
    void 경계값_0과_100은_허용한다() {
        // when
        ConnectionArguments zero = ConnectionArguments.of(0, null, null, null);
        ConnectionArguments max = ConnectionArguments.of(100, null, null, null);

        // then
        assertThat(zero.first()).isZero();
        assertThat(max.first()).isEqualTo(100);
    }

    @Test
    void first와_last를_모두_생략하면_기본값_first_20을_적용한다() {
        // when
        ConnectionArguments arguments = ConnectionArguments.of(null, null, null, null);

        // then
        assertThat(arguments.first()).isEqualTo(ConnectionArguments.DEFAULT_PAGE_SIZE);
        assertThat(arguments.last()).isNull();

        ConnectionArguments.OffsetLimit offsetLimit = arguments.toOffsetLimit(null);
        assertThat(offsetLimit.offset()).isZero();
        assertThat(offsetLimit.limit()).isEqualTo(20);
    }

    @Test
    void first만_지정하면_offset_0부터_first개를_조회한다() {
        // given
        ConnectionArguments arguments = ConnectionArguments.of(10, null, null, null);

        // when
        ConnectionArguments.OffsetLimit offsetLimit = arguments.toOffsetLimit(null);

        // then
        assertThat(offsetLimit.offset()).isZero();
        assertThat(offsetLimit.limit()).isEqualTo(10);
    }

    @Test
    void first와_after를_지정하면_after_다음_위치부터_조회한다() {
        // given — after 커서는 offset 4를 가리킨다.
        ConnectionArguments arguments = ConnectionArguments.of(10, RelayCursor.encodeOffset(4), null, null);

        // when
        ConnectionArguments.OffsetLimit offsetLimit = arguments.toOffsetLimit(null);

        // then
        assertThat(offsetLimit.offset()).isEqualTo(5);
        assertThat(offsetLimit.limit()).isEqualTo(10);
    }

    @Test
    void first에_before가_함께_오면_before_이전까지로_윈도우를_줄인다() {
        // given — (4, 8) 열린 구간에서 first=10이어도 3개만 남는다.
        ConnectionArguments arguments = ConnectionArguments.of(
            10, RelayCursor.encodeOffset(4), null, RelayCursor.encodeOffset(8)
        );

        // when
        ConnectionArguments.OffsetLimit offsetLimit = arguments.toOffsetLimit(null);

        // then
        assertThat(offsetLimit.offset()).isEqualTo(5);
        assertThat(offsetLimit.limit()).isEqualTo(3);
    }

    @Test
    void before가_after보다_앞이면_빈_윈도우로_클램프한다() {
        // given — after(9) 다음은 10인데 before(3)가 그보다 앞이다.
        ConnectionArguments arguments = ConnectionArguments.of(
            10, RelayCursor.encodeOffset(9), null, RelayCursor.encodeOffset(3)
        );

        // when
        ConnectionArguments.OffsetLimit offsetLimit = arguments.toOffsetLimit(null);

        // then
        assertThat(offsetLimit.offset()).isEqualTo(10);
        assertThat(offsetLimit.limit()).isZero();
    }

    @Test
    void last와_before를_지정하면_before_직전_last개를_조회한다() {
        // given
        ConnectionArguments arguments = ConnectionArguments.of(null, null, 5, RelayCursor.encodeOffset(10));

        // when
        ConnectionArguments.OffsetLimit offsetLimit = arguments.toOffsetLimit(null);

        // then
        assertThat(offsetLimit.offset()).isEqualTo(5);
        assertThat(offsetLimit.limit()).isEqualTo(5);
    }

    @Test
    void last가_before_이전_개수보다_크면_처음부터_있는_만큼만_조회한다() {
        // given — before(3) 이전에는 3개뿐이다.
        ConnectionArguments arguments = ConnectionArguments.of(null, null, 5, RelayCursor.encodeOffset(3));

        // when
        ConnectionArguments.OffsetLimit offsetLimit = arguments.toOffsetLimit(null);

        // then
        assertThat(offsetLimit.offset()).isZero();
        assertThat(offsetLimit.limit()).isEqualTo(3);
    }

    @Test
    void last에_after가_함께_오면_after_다음보다_앞으로_가지_않는다() {
        // given — (7, 9) 열린 구간에서 last=5여도 1개만 남는다.
        ConnectionArguments arguments = ConnectionArguments.of(
            null, RelayCursor.encodeOffset(7), 5, RelayCursor.encodeOffset(9)
        );

        // when
        ConnectionArguments.OffsetLimit offsetLimit = arguments.toOffsetLimit(null);

        // then
        assertThat(offsetLimit.offset()).isEqualTo(8);
        assertThat(offsetLimit.limit()).isEqualTo(1);
    }

    @Test
    void before_없는_last는_totalSize_기준으로_끝에서_last개를_조회한다() {
        // given
        ConnectionArguments arguments = ConnectionArguments.of(null, null, 5, null);

        // when
        ConnectionArguments.OffsetLimit offsetLimit = arguments.toOffsetLimit(50L);

        // then
        assertThat(offsetLimit.offset()).isEqualTo(45);
        assertThat(offsetLimit.limit()).isEqualTo(5);
    }

    @Test
    void before도_totalSize도_없는_last는_예외가_발생한다() {
        // given
        ConnectionArguments arguments = ConnectionArguments.of(null, null, 5, null);

        // when & then
        assertThatThrownBy(() -> arguments.toOffsetLimit(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("last requires before");
    }

    @Test
    void 최대_offset을_넘는_페이지네이션은_예외가_발생한다() {
        // given — after(10000) 다음 위치는 10001로 MAX_OFFSET(10000)을 넘는다.
        ConnectionArguments arguments = ConnectionArguments.of(1, RelayCursor.encodeOffset(10_000), null, null);

        // when & then
        assertThatThrownBy(() -> arguments.toOffsetLimit(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("10000");
    }

    @Test
    void 최대_offset_경계값은_허용한다() {
        // given — after(9999) 다음 위치는 정확히 10000이다.
        ConnectionArguments arguments = ConnectionArguments.of(1, RelayCursor.encodeOffset(9_999), null, null);

        // when
        ConnectionArguments.OffsetLimit offsetLimit = arguments.toOffsetLimit(null);

        // then
        assertThat(offsetLimit.offset()).isEqualTo(10_000);
    }

    @Test
    void toPageable은_오프셋과_limit을_그대로_옮긴_OffsetPageRequest를_반환한다() {
        // given
        ConnectionArguments arguments = ConnectionArguments.of(10, RelayCursor.encodeOffset(4), null, null);

        // when
        Pageable pageable = arguments.toPageable();

        // then
        assertThat(pageable).isInstanceOf(OffsetPageRequest.class);
        assertThat(pageable.getOffset()).isEqualTo(5);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    void toPageable은_빈_윈도우여도_최소_1건_조회로_보정한다() {
        // given — first=0이면 limit이 0이지만 Pageable은 size 1 이상이어야 한다.
        ConnectionArguments arguments = ConnectionArguments.of(0, null, null, null);

        // when
        Pageable pageable = arguments.toPageable();

        // then
        assertThat(pageable.getPageSize()).isEqualTo(1);
    }

    @Test
    void before_없는_last는_공급받은_totalSize로_뒤쪽_Pageable을_만든다() {
        // given
        AtomicBoolean totalSizeRequested = new AtomicBoolean(false);
        ConnectionArguments arguments = ConnectionArguments.of(null, null, 5, null);

        // when
        Pageable pageable = arguments.toPageable(() -> {
            totalSizeRequested.set(true);
            return 50L;
        });

        // then
        assertThat(totalSizeRequested).isTrue();
        assertThat(pageable.getOffset()).isEqualTo(45);
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }

    @Test
    void forward_페이지네이션은_totalSize_공급자를_호출하지_않는다() {
        // given
        AtomicBoolean totalSizeRequested = new AtomicBoolean(false);
        ConnectionArguments arguments = ConnectionArguments.of(5, null, null, null);

        // when
        arguments.toPageable(() -> {
            totalSizeRequested.set(true);
            return 50L;
        });

        // then
        assertThat(totalSizeRequested).isFalse();
    }
}
