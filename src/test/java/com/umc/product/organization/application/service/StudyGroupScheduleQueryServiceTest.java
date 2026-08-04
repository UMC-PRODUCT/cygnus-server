package com.umc.product.organization.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupSchedulePort;

@ExtendWith(MockitoExtension.class)
class StudyGroupScheduleQueryServiceTest {

    @Mock
    LoadStudyGroupSchedulePort loadStudyGroupSchedulePort;

    @Mock
    GetStudyGroupUseCase getStudyGroupUseCase;

    @InjectMocks
    StudyGroupScheduleQueryService sut;

    @Test
    void 호출자_시야의_스터디_그룹IDs를_받아_매핑된_일정IDs를_반환() {
        // given - 호출자가 이미 "보이는 스터디 그룹" 을 알고 있는 경우.
        //   memberId 밖에 없다면 findVisibleScheduleIdsByMemberId 를 쓴다.
        List<Long> visibleGroupIds = List.of(1L, 2L);
        Set<Long> mappedScheduleIds = Set.of(100L, 200L, 300L);

        given(loadStudyGroupSchedulePort.findScheduleIdsByStudyGroupIds(visibleGroupIds))
            .willReturn(mappedScheduleIds);

        // when
        Set<Long> result = sut.findScheduleIdsByStudyGroupIds(visibleGroupIds);

        // then — Service 는 단순 합성 facade. 가공 없이 그대로 통과.
        assertThat(result).containsExactlyInAnyOrder(100L, 200L, 300L);
    }

    @Test
    void 권한_없는_사용자는_visible_그룹이_없어_DB_호출_없이_빈_Set() {
        // given - 회장단도 파트장도 아닌 일반 챌린저라 보이는 그룹이 없는 경우.
        //   풀스캔 / 무용 IN() 쿼리 방지를 위해 즉시 빈 Set 반환

        // when
        Set<Long> result = sut.findScheduleIdsByStudyGroupIds(List.of());

        // then
        assertThat(result).isEmpty();
        verify(loadStudyGroupSchedulePort, never()).findScheduleIdsByStudyGroupIds(any());
    }

    @Test
    @DisplayName("memberId만 받아 보이는 스터디 그룹을 구한 뒤 매핑된 일정 ID를 반환한다")
    void findVisibleScheduleIdsByMemberIdResolvesVisibleGroupsThenSchedules() {
        // given - Schedule 도메인은 memberId 만 넘기고, 어떤 그룹이 보이는지는 Organization 이 판단한다
        Long memberId = 1L;
        Set<Long> visibleGroupIds = Set.of(1L, 2L);

        given(getStudyGroupUseCase.findVisibleStudyGroupIds(memberId)).willReturn(visibleGroupIds);
        given(loadStudyGroupSchedulePort.findScheduleIdsByStudyGroupIds(visibleGroupIds))
            .willReturn(Set.of(100L, 200L));

        // when
        Set<Long> result = sut.findVisibleScheduleIdsByMemberId(memberId);

        // then
        assertThat(result).containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    @DisplayName("보이는 스터디 그룹이 없으면 일정 조회 없이 빈 Set을 반환한다")
    void findVisibleScheduleIdsByMemberIdSkipsScheduleQueryWhenNoVisibleGroup() {
        // given - 권한 없는 일반 챌린저. 빈 IN() 쿼리가 나가지 않아야 한다
        Long memberId = 1L;
        given(getStudyGroupUseCase.findVisibleStudyGroupIds(memberId)).willReturn(Set.of());

        // when
        Set<Long> result = sut.findVisibleScheduleIdsByMemberId(memberId);

        // then
        assertThat(result).isEmpty();
        verify(loadStudyGroupSchedulePort, never()).findScheduleIdsByStudyGroupIds(any());
    }

    @Test
    @DisplayName("보이는 그룹은 있으나 등록된 일정이 없으면 빈 Set을 반환한다")
    void findVisibleScheduleIdsByMemberIdReturnsEmptyWhenGroupsHaveNoSchedule() {
        // given - 새로 만들어져 아직 일정이 매핑되지 않은 그룹들. 예외가 아니라 자연스러운 빈 결과여야 한다
        Long memberId = 1L;
        Set<Long> visibleGroupIds = Set.of(1L, 2L);

        given(getStudyGroupUseCase.findVisibleStudyGroupIds(memberId)).willReturn(visibleGroupIds);
        given(loadStudyGroupSchedulePort.findScheduleIdsByStudyGroupIds(visibleGroupIds)).willReturn(Set.of());

        // when
        Set<Long> result = sut.findVisibleScheduleIdsByMemberId(memberId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 시야의_그룹에_등록된_일정이_없어도_정상_빈_Set_반환() {
        // given — 회장단/파트장 권한은 있어 visibleGroupIds 가 채워졌지만,
        //         그 그룹들에 아직 schedule 매핑이 안 된 경우 (새 그룹들).
        //         예외가 아니라 자연스러운 빈 결과여야 함.
        List<Long> visibleGroupIds = List.of(1L, 2L);
        given(loadStudyGroupSchedulePort.findScheduleIdsByStudyGroupIds(visibleGroupIds))
            .willReturn(Set.of());

        // when
        Set<Long> result = sut.findScheduleIdsByStudyGroupIds(visibleGroupIds);

        // then
        assertThat(result).isEmpty();
    }
}
