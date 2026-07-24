package com.umc.product.curriculum.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

@ExtendWith(MockitoExtension.class)
class ChallengerWorkbookQueryServiceTest {

    @Mock
    private LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    @Mock
    private LoadMissionSubmissionPort loadMissionSubmissionPort;
    @Mock
    private LoadMissionFeedbackPort loadMissionFeedbackPort;
    @Mock
    private GetChallengerUseCase getChallengerUseCase;
    @Mock
    private LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    @Mock
    private LoadWeeklyBestWorkbookPort loadWeeklyBestWorkbookPort;
    @InjectMocks
    private ChallengerWorkbookQueryService service;

    private ChallengerWorkbook workbook;

    @BeforeEach
    void setUp() {
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼"), 1L, false, "1주차",
            Instant.EPOCH, Instant.parse("2026-08-01T00:00:00Z")
        );
        ReflectionTestUtils.setField(weekly, "id", 20L);
        OriginalWorkbook original = OriginalWorkbook.createAsDraft(
            weekly, "워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        ReflectionTestUtils.setField(original, "id", 1L);
        workbook = ChallengerWorkbook.create(original, 3L, 10L);
        ReflectionTestUtils.setField(workbook, "id", 2L);
    }

    @Test
    @DisplayName("같은 기수의 챌린저 이력이 있으면 챌린저 워크북을 조회한다")
    void sameGisuHistory_allowed() {
        given(loadChallengerWorkbookPort.getById(2L)).willReturn(workbook);
        given(getChallengerUseCase.findByMemberIdAndGisuId(4L, 9L))
            .willReturn(Optional.of(ChallengerInfo.builder().memberId(4L).gisuId(9L).build()));
        given(loadMissionSubmissionPort.listActiveByChallengerWorkbookId(2L)).willReturn(List.of());
        given(loadOriginalWorkbookMissionPort.findByOriginalWorkbookId(1L)).willReturn(List.of());

        assertThat(service.getById(2L, 4L).challengerWorkbookId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("같은 회원·주차·그룹의 베스트 선정 여부와 필수 미션을 상세 응답에 반영한다")
    void selectedBestAndRequiredMissions_areIncluded() {
        OriginalWorkbookMission requiredMission = OriginalWorkbookMission.create(
            workbook.getOriginalWorkbook(),
            "필수 미션",
            null,
            MissionType.MEMO,
            true
        );
        ReflectionTestUtils.setField(requiredMission, "id", 11L);
        given(loadChallengerWorkbookPort.getById(2L)).willReturn(workbook);
        given(getChallengerUseCase.findByMemberIdAndGisuId(4L, 9L))
            .willReturn(Optional.of(ChallengerInfo.builder().memberId(4L).gisuId(9L).build()));
        given(loadMissionSubmissionPort.listActiveByChallengerWorkbookId(2L)).willReturn(List.of());
        given(loadOriginalWorkbookMissionPort.findByOriginalWorkbookId(1L))
            .willReturn(List.of(requiredMission));
        given(loadWeeklyBestWorkbookPort.existsByMemberIdAndWeeklyCurriculumIdAndStudyGroupId(
            3L,
            workbook.getOriginalWorkbook().getWeeklyCurriculum().getId(),
            10L
        )).willReturn(true);

        var result = service.getById(2L, 4L);

        assertThat(result.isBestWorkbook()).isTrue();
        assertThat(result.requiredMissionIds()).containsExactly(11L);
    }

    @Test
    @DisplayName("같은 기수의 챌린저 이력이 없으면 챌린저 워크북 조회를 거부한다")
    void differentGisu_denied() {
        given(loadChallengerWorkbookPort.getById(2L)).willReturn(workbook);
        given(getChallengerUseCase.findByMemberIdAndGisuId(4L, 9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(2L, 4L))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
    }
}
