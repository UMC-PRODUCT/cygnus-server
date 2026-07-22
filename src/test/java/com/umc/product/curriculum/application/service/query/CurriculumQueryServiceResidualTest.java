package com.umc.product.curriculum.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurriculumQueryService 잔여 경계")
class CurriculumQueryServiceResidualTest {

    @Mock GetChallengerUseCase getChallengerUseCase;
    @Mock LoadCurriculumPort loadCurriculumPort;
    @Mock LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    @Mock LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    @Mock LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    @Mock LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    @Mock LoadMissionSubmissionPort loadMissionSubmissionPort;
    @Mock LoadMissionFeedbackPort loadMissionFeedbackPort;

    CurriculumQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new CurriculumQueryService(
            getChallengerUseCase, loadCurriculumPort, loadWeeklyCurriculumPort,
            loadOriginalWorkbookPort, loadOriginalWorkbookMissionPort,
            loadChallengerWorkbookPort, loadMissionSubmissionPort, loadMissionFeedbackPort
        );
        given(getChallengerUseCase.getByMemberIdAndGisuId(1L, 9L))
            .willReturn(ChallengerInfo.builder().part(ChallengerPart.SPRINGBOOT).build());
        given(loadCurriculumPort.getByGisuIdAndPart(9L, ChallengerPart.SPRINGBOOT))
            .willReturn(new CurriculumProjection(100L, ChallengerPart.SPRINGBOOT, "커리큘럼"));
    }

    @Test
    @DisplayName("주차가 없으면 하위 port를 조회하지 않고 빈 진행률을 반환한다")
    void empty_week_short_circuits() {
        given(loadWeeklyCurriculumPort.findByCurriculumId(100L, null)).willReturn(List.of());

        MyCurriculumInfo result = sut.getMyProgress(1L, 9L);

        assertThat(result.weeks()).isEmpty();
        then(loadOriginalWorkbookPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("피드백 없음·FAIL만 존재·PASS 포함 상태를 각각 PENDING·FAIL·PASS로 조립한다")
    void resolves_every_submission_status_and_feedback() {
        WeeklyCurriculum weekly = weekly();
        OriginalWorkbook workbook = OriginalWorkbook.createAsReady(
            weekly, "워크북", "설명", "url", "내용", OriginalWorkbookType.MAIN
        );
        ReflectionTestUtils.setField(workbook, "id", 200L);
        OriginalWorkbookMission pendingMission = mission(301L, workbook, "대기");
        OriginalWorkbookMission failMission = mission(302L, workbook, "실패");
        OriginalWorkbookMission passMission = mission(303L, workbook, "통과");
        ChallengerWorkbook challengerWorkbook = mock(ChallengerWorkbook.class);
        given(challengerWorkbook.getId()).willReturn(400L);
        given(challengerWorkbook.getOriginalWorkbook()).willReturn(workbook);

        MissionSubmission pending = submission(501L, pendingMission);
        MissionSubmission fail = submission(502L, failMission);
        MissionSubmission pass = submission(503L, passMission);
        MissionFeedback failFeedback = feedback(601L, fail, FeedbackResult.FAIL, "다시 제출");
        MissionFeedback passFeedback = feedback(602L, pass, FeedbackResult.PASS, "통과");
        MissionFeedback priorFailFeedback = feedback(603L, pass, FeedbackResult.FAIL, "이전 실패");

        given(loadWeeklyCurriculumPort.findByCurriculumId(100L, null)).willReturn(List.of(weekly));
        given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumIdIn(List.of(10L)))
            .willReturn(List.of(workbook));
        given(loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(List.of(200L)))
            .willReturn(List.of(pendingMission, failMission, passMission));
        given(loadChallengerWorkbookPort.findByMemberIdAndOriginalWorkbookIdIn(1L, List.of(200L)))
            .willReturn(List.of(challengerWorkbook));
        given(loadMissionSubmissionPort.findByChallengerWorkbookIdIn(List.of(400L)))
            .willReturn(List.of(pending, fail, pass));
        given(loadMissionFeedbackPort.findByMissionSubmissionIdIn(List.of(501L, 502L, 503L)))
            .willReturn(List.of(failFeedback, passFeedback, priorFailFeedback));

        MyCurriculumInfo result = sut.getMyProgress(1L, 9L);

        var missions = result.weeks().get(0).releasedOriginalWorkbooks().get(0).missions();
        assertThat(missions).extracting(info -> info.submission().orElseThrow().status())
            .containsExactly(SubmissionStatus.PENDING, SubmissionStatus.FAIL, SubmissionStatus.PASS);
        assertThat(missions.get(0).submission().orElseThrow().hasFeedback()).isFalse();
        assertThat(missions.get(1).submission().orElseThrow().feedbacks())
            .singleElement().satisfies(info -> {
                assertThat(info.feedbackResult()).isEqualTo(FeedbackResult.FAIL);
                assertThat(info.content()).isEqualTo("다시 제출");
                assertThat(info.reviewerMemberId()).isEqualTo(7L);
            });
        assertThat(missions.get(2).submission().orElseThrow().hasFeedback()).isTrue();
    }

    private WeeklyCurriculum weekly() {
        Curriculum curriculum = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼");
        ReflectionTestUtils.setField(curriculum, "id", 100L);
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            curriculum, 1L, false, "1주차",
            Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2030-01-07T00:00:00Z")
        );
        ReflectionTestUtils.setField(weekly, "id", 10L);
        return weekly;
    }

    private OriginalWorkbookMission mission(Long id, OriginalWorkbook workbook, String title) {
        OriginalWorkbookMission mission = OriginalWorkbookMission.create(
            workbook, title, "설명", MissionType.LINK, true
        );
        ReflectionTestUtils.setField(mission, "id", id);
        return mission;
    }

    private MissionSubmission submission(Long id, OriginalWorkbookMission mission) {
        MissionSubmission submission = mock(MissionSubmission.class);
        given(submission.getId()).willReturn(id);
        given(submission.getOriginalWorkbookMission()).willReturn(mission);
        given(submission.getSubmittedAsType()).willReturn(MissionType.LINK);
        given(submission.getContent()).willReturn("https://example.com/" + id);
        return submission;
    }

    private MissionFeedback feedback(
        Long id,
        MissionSubmission submission,
        FeedbackResult result,
        String content
    ) {
        MissionFeedback feedback = mock(MissionFeedback.class);
        given(feedback.getId()).willReturn(id);
        given(feedback.getMissionSubmission()).willReturn(submission);
        given(feedback.getReviewerMemberId()).willReturn(7L);
        given(feedback.getContent()).willReturn(content);
        given(feedback.getFeedbackResult()).willReturn(result);
        return feedback;
    }
}
