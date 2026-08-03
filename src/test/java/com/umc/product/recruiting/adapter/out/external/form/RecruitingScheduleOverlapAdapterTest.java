package com.umc.product.recruiting.adapter.out.external.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.form.application.port.in.query.GetScheduleOverlapUseCase;
import com.umc.product.form.application.port.in.query.dto.ScheduleOverlapSlotInfo;

@ExtendWith(MockitoExtension.class)
class RecruitingScheduleOverlapAdapterTest {

    @Mock GetScheduleOverlapUseCase getScheduleOverlapUseCase;

    @Test
    @DisplayName("Form과 일정 질문을 지정해 15분 가능 시간과 응답자를 조회한다")
    void findOverlapsDelegatesFormAndQuestion() {
        Instant startsAt = Instant.parse("2026-08-10T00:00:00Z");
        given(getScheduleOverlapUseCase.getOverlap(10L, 20L, Set.of(100L, 200L)))
            .willReturn(List.of(new ScheduleOverlapSlotInfo(startsAt, Set.of(100L, 200L))));
        RecruitingScheduleOverlapAdapter sut = new RecruitingScheduleOverlapAdapter(getScheduleOverlapUseCase);

        var result = sut.findOverlaps(10L, 20L, List.of(100L, 200L), null, null);

        assertThat(result).singleElement().satisfies(slot -> {
            assertThat(slot.startsAt()).isEqualTo(startsAt);
            assertThat(slot.availableFormResponseIds()).containsExactlyInAnyOrder(100L, 200L);
        });
        then(getScheduleOverlapUseCase).should().getOverlap(10L, 20L, Set.of(100L, 200L));
    }

    @Test
    @DisplayName("범위가 주어지면 범위 밖 슬롯은 걸러내고, 범용 overlap 계산 자체는 그대로 위임한다")
    void findOverlapsFiltersSlotsOutsideRequestedRange() {
        Instant before = Instant.parse("2026-08-09T23:45:00Z");
        Instant inside = Instant.parse("2026-08-10T00:00:00Z");
        Instant after = Instant.parse("2026-08-11T00:00:00Z");
        given(getScheduleOverlapUseCase.getOverlap(10L, 20L, Set.of(100L))).willReturn(List.of(
            new ScheduleOverlapSlotInfo(before, Set.of(100L)),
            new ScheduleOverlapSlotInfo(inside, Set.of(100L)),
            new ScheduleOverlapSlotInfo(after, Set.of(100L))
        ));
        RecruitingScheduleOverlapAdapter sut = new RecruitingScheduleOverlapAdapter(getScheduleOverlapUseCase);

        var result = sut.findOverlaps(
            10L, 20L, List.of(100L),
            Instant.parse("2026-08-10T00:00:00Z"),
            Instant.parse("2026-08-11T00:00:00Z")
        );

        assertThat(result).singleElement()
            .satisfies(slot -> assertThat(slot.startsAt()).isEqualTo(inside));
        then(getScheduleOverlapUseCase).should().getOverlap(10L, 20L, Set.of(100L));
    }
}
