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

        var result = sut.findOverlaps(10L, 20L, List.of(100L, 200L));

        assertThat(result).singleElement().satisfies(slot -> {
            assertThat(slot.startsAt()).isEqualTo(startsAt);
            assertThat(slot.availableFormResponseIds()).containsExactlyInAnyOrder(100L, 200L);
        });
        then(getScheduleOverlapUseCase).should().getOverlap(10L, 20L, Set.of(100L, 200L));
    }
}
