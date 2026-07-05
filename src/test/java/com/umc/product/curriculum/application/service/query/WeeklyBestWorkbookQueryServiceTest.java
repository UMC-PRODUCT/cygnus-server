package com.umc.product.curriculum.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
import com.umc.product.curriculum.application.port.in.query.dto.WeeklyBestWorkbookPageInfo;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.SearchWeeklyBestWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;

@ExtendWith(MockitoExtension.class)
class WeeklyBestWorkbookQueryServiceTest {

    @Mock
    SearchWeeklyBestWorkbookPort searchWeeklyBestWorkbookPort;

    @Mock
    LoadChallengerWorkbookPort loadChallengerWorkbookPort;

    @Mock
    LoadMissionSubmissionPort loadMissionSubmissionPort;

    @Mock
    LoadMissionFeedbackPort loadMissionFeedbackPort;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @InjectMocks
    WeeklyBestWorkbookQueryService sut;

    @Test
    @DisplayName("베스트 워크북 조회 시 size+1로 조회하고 다음 커서를 계산한다")
    void searchBestWorkbooksSuccess() {
        // given
        GetBestWorkbooksQuery query = GetBestWorkbooksQuery.of(
            9L,
            Set.of(1L),
            Set.of(ChallengerPart.SPRINGBOOT),
            List.of(1L),
            List.of(30L),
            null,
            1
        );

        WeeklyBestWorkbook first = weeklyBestWorkbook(100L, 40L, 30L);
        WeeklyBestWorkbook second = Mockito.mock(WeeklyBestWorkbook.class);
        ChallengerWorkbook challengerWorkbook = Mockito.mock(ChallengerWorkbook.class);
        OriginalWorkbook originalWorkbook = OriginalWorkbook.createAsReady(
            first.getWeeklyCurriculum(),
            "1주차 워크북",
            null,
            null,
            "본문",
            OriginalWorkbookType.MAIN
        );
        ReflectionTestUtils.setField(originalWorkbook, "id", 200L);
        given(challengerWorkbook.getId()).willReturn(500L);
        given(challengerWorkbook.getOriginalWorkbook()).willReturn(originalWorkbook);
        given(challengerWorkbook.getMemberId()).willReturn(40L);
        given(challengerWorkbook.getStudyGroupId()).willReturn(30L);
        given(challengerWorkbook.isExcused()).willReturn(false);
        given(challengerWorkbook.getContent()).willReturn("챌린저 본문");

        given(getMemberUseCase.listIdsBySchoolIds(Set.of(1L))).willReturn(Map.of(1L, Set.of(40L)));
        given(searchWeeklyBestWorkbookPort.searchBestWorkbooks(query.withMemberIds(Set.of(40L)).withSize(2)))
            .willReturn(List.of(first, second));
        given(loadChallengerWorkbookPort.findByMemberIdInAndWeeklyCurriculumIdIn(List.of(40L), List.of(10L)))
            .willReturn(List.of(challengerWorkbook));
        given(loadMissionSubmissionPort.findByChallengerWorkbookIdIn(List.of(500L))).willReturn(List.of());

        // when
        WeeklyBestWorkbookPageInfo result = sut.searchBestWorkbooks(query);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).weeklyBestWorkbookEntityId()).isEqualTo(100L);
        assertThat(result.content().get(0).challengerId()).isEqualTo(40L);
        assertThat(result.content().get(0).gisuId()).isEqualTo(9L);
        assertThat(result.content().get(0).part()).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThat(result.content().get(0).challengerWorkbooks()).hasSize(1);
        assertThat(result.nextCursor()).isEqualTo(100L);
        assertThat(result.hasNext()).isTrue();
    }

    private WeeklyBestWorkbook weeklyBestWorkbook(Long id, Long memberId, Long studyGroupId) {
        Curriculum curriculum = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "9기 스프링부트");
        ReflectionTestUtils.setField(curriculum, "id", 1L);
        WeeklyCurriculum weeklyCurriculum = WeeklyCurriculum.create(
            curriculum,
            1L,
            false,
            "1주차",
            Instant.parse("2026-06-01T00:00:00Z"),
            Instant.parse("2026-06-07T00:00:00Z")
        );
        ReflectionTestUtils.setField(weeklyCurriculum, "id", 10L);

        WeeklyBestWorkbook bestWorkbook = Mockito.mock(WeeklyBestWorkbook.class);
        given(bestWorkbook.getId()).willReturn(id);
        given(bestWorkbook.getMemberId()).willReturn(memberId);
        given(bestWorkbook.getStudyGroupId()).willReturn(studyGroupId);
        given(bestWorkbook.getWeeklyCurriculum()).willReturn(weeklyCurriculum);
        given(bestWorkbook.getReason()).willReturn("잘 작성했습니다");
        given(bestWorkbook.getDecidedMemberId()).willReturn(99L);
        return bestWorkbook;
    }
}
