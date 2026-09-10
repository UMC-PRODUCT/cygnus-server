package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateWeeklyBestWorkbookCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.application.port.out.SaveWeeklyBestWorkbookPort;
import com.umc.product.curriculum.application.service.evaluator.CurriculumStudyGroupStaffPolicy;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;

@ExtendWith(MockitoExtension.class)
class WeeklyBestWorkbookCommandServiceTest {

    private static final Long MEMBER_ID = 30L;
    private static final Long GROUP_ID = 10L;

    @Mock private LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    @Mock private LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    @Mock private LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    @Mock private LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    @Mock private LoadMissionSubmissionPort loadMissionSubmissionPort;
    @Mock private LoadMissionFeedbackPort loadMissionFeedbackPort;
    @Mock private LoadWeeklyBestWorkbookPort loadWeeklyBestWorkbookPort;
    @Mock private SaveWeeklyBestWorkbookPort saveWeeklyBestWorkbookPort;
    @Mock private GetChallengerUseCase getChallengerUseCase;
    @Mock private GetStudyGroupUseCase getStudyGroupUseCase;
    @Mock private CheckPermissionUseCase checkPermissionUseCase;
    @Mock private CurriculumStudyGroupStaffPolicy staffPolicy;
    @Mock private MissionMutationPolicy missionMutationPolicy;
    @InjectMocks private WeeklyBestWorkbookCommandService service;

    private WeeklyCurriculum weekly;
    private OriginalWorkbook original;
    private OriginalWorkbookMission mission;
    private ChallengerWorkbook workbook;
    private MissionSubmission submission;

    @BeforeEach
    void setUp() {
        Curriculum curriculum = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼");
        weekly = WeeklyCurriculum.create(
            curriculum, 1L, false, "1주차", Instant.EPOCH, Instant.parse("2026-08-01T00:00:00Z")
        );
        ReflectionTestUtils.setField(weekly, "id", 20L);
        original = OriginalWorkbook.createAsReady(
            weekly, "워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        original.changeStatus(OriginalWorkbookStatus.RELEASED, 1L);
        ReflectionTestUtils.setField(original, "id", 100L);
        mission = OriginalWorkbookMission.create(original, "필수 미션", null, MissionType.MEMO, true);
        ReflectionTestUtils.setField(mission, "id", 200L);
        workbook = ChallengerWorkbook.create(original, MEMBER_ID, GROUP_ID);
        ReflectionTestUtils.setField(workbook, "id", 300L);
        submission = MissionSubmission.create(mission, workbook, "제출");
        ReflectionTestUtils.setField(submission, "id", 400L);
    }

    @Test
    @DisplayName("모든 필수 미션 제출에 PASS 피드백이 있으면 베스트 워크북을 선정한다")
    void allRequiredMissionsPassed_selectsBest() {
        givenCommonEligibility();
        given(loadMissionFeedbackPort.listByMissionSubmissionIdIn(List.of(400L)))
            .willReturn(List.of(MissionFeedback.create(submission, 50L, "통과", FeedbackResult.PASS)));

        assertThatCode(() -> service.selectBest(command(GROUP_ID))).doesNotThrowAnyException();

        verify(saveWeeklyBestWorkbookPort).save(any(WeeklyBestWorkbook.class));
    }

    @Test
    @DisplayName("필수 미션에 PASS 피드백이 없으면 베스트 워크북 선정을 거부한다")
    void missingPassFeedback_rejected() {
        givenCommonEligibility();
        given(loadMissionFeedbackPort.listByMissionSubmissionIdIn(List.of(400L))).willReturn(List.of());

        assertThatThrownBy(() -> service.selectBest(command(GROUP_ID)))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.BEST_WORKBOOK_REQUIREMENTS_NOT_MET);
    }

    @Test
    @DisplayName("필수 미션 제출이 없으면 베스트 워크북 선정을 거부한다")
    void missingRequiredSubmissionRejected() {
        givenCommonEligibility();
        given(loadMissionSubmissionPort.listActiveByChallengerWorkbookIdIn(List.of(300L)))
            .willReturn(List.of());

        assertThatThrownBy(() -> service.selectBest(command(GROUP_ID)))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.BEST_WORKBOOK_REQUIREMENTS_NOT_MET);
    }

    @Test
    @DisplayName("필수 미션의 피드백이 FAIL뿐이면 베스트 워크북 선정을 거부한다")
    void failedRequiredSubmissionRejected() {
        givenCommonEligibility();
        given(loadMissionFeedbackPort.listByMissionSubmissionIdIn(List.of(400L)))
            .willReturn(List.of(MissionFeedback.create(submission, 50L, "실패", FeedbackResult.FAIL)));

        assertThatThrownBy(() -> service.selectBest(command(GROUP_ID)))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.BEST_WORKBOOK_REQUIREMENTS_NOT_MET);
    }

    @Test
    @DisplayName("필수 미션에 PASS와 FAIL 피드백이 함께 있으면 베스트 선정을 거부한다")
    void mixedPassAndFailFeedbackRejected() {
        givenCommonEligibility();
        given(loadMissionFeedbackPort.listByMissionSubmissionIdIn(List.of(400L)))
            .willReturn(List.of(
                MissionFeedback.create(submission, 50L, "통과", FeedbackResult.PASS),
                MissionFeedback.create(submission, 51L, "실패", FeedbackResult.FAIL)
            ));

        assertThatThrownBy(() -> service.selectBest(command(GROUP_ID)))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.BEST_WORKBOOK_REQUIREMENTS_NOT_MET);
    }

    @Test
    @DisplayName("같은 그룹의 다른 학교 회장단은 베스트 대상 회원을 선정할 수 없다")
    void differentSchoolCoreCannotSelectTargetMember() {
        SubjectAttributes subject = SubjectAttributes.builder()
            .memberId(50L)
            .schoolId(200L)
            .build();
        given(loadWeeklyCurriculumPort.getById(20L)).willReturn(weekly);
        given(getStudyGroupUseCase.getById(GROUP_ID)).willReturn(StudyGroupInfo.create(
            GROUP_ID, "혼합 학교 그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH,
            List.of(60L), List.of(MEMBER_ID, 31L)
        ));
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(
            ChallengerInfo.builder()
                .memberId(MEMBER_ID)
                .gisuId(9L)
                .part(ChallengerPart.SPRINGBOOT)
                .challengerStatus(ChallengerStatus.ACTIVE)
                .build()
        ));
        given(checkPermissionUseCase.loadSubject(50L)).willReturn(subject);
        given(staffPolicy.canManage(subject, GROUP_ID, MEMBER_ID, 9L)).willReturn(false);

        assertThatThrownBy(() -> service.selectBest(command(GROUP_ID)))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
    }

    @Test
    @DisplayName("면제된 워크북은 PASS 피드백이 있어도 베스트로 선정할 수 없다")
    void excusedWorkbookRejected() {
        workbook.excuse("면제", 50L);
        given(loadWeeklyCurriculumPort.getById(20L)).willReturn(weekly);
        given(getStudyGroupUseCase.getById(GROUP_ID)).willReturn(StudyGroupInfo.create(
            GROUP_ID, "그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH, List.of(50L), List.of(MEMBER_ID)
        ));
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(
            ChallengerInfo.builder()
                .memberId(MEMBER_ID)
                .gisuId(9L)
                .part(ChallengerPart.SPRINGBOOT)
                .challengerStatus(ChallengerStatus.ACTIVE)
                .build()
        ));
        givenDecisionAuthority();
        given(loadWeeklyBestWorkbookPort.existsByWeeklyCurriculumIdAndStudyGroupId(20L, GROUP_ID))
            .willReturn(false);
        given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumId(20L)).willReturn(List.of(original));
        given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(MEMBER_ID, List.of(100L)))
            .willReturn(List.of(workbook));

        assertThatThrownBy(() -> service.selectBest(command(GROUP_ID)))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.BEST_WORKBOOK_REQUIREMENTS_NOT_MET);
    }

    @Test
    @DisplayName("선택 미션은 베스트 선정 조건에서 제외한다")
    void optionalMissionExcludedFromEligibility() {
        OriginalWorkbookMission optional = OriginalWorkbookMission.create(
            original, "선택 미션", null, MissionType.MEMO, false
        );
        ReflectionTestUtils.setField(optional, "id", 201L);
        givenCommonEligibility();
        given(loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(List.of(100L)))
            .willReturn(List.of(mission, optional));
        given(loadMissionFeedbackPort.listByMissionSubmissionIdIn(List.of(400L)))
            .willReturn(List.of(MissionFeedback.create(submission, 50L, "통과", FeedbackResult.PASS)));

        assertThatCode(() -> service.selectBest(command(GROUP_ID))).doesNotThrowAnyException();

        verify(saveWeeklyBestWorkbookPort).save(any(WeeklyBestWorkbook.class));
    }

    @Test
    @DisplayName("같은 그룹과 주차에 베스트가 이미 있으면 선정을 거부한다")
    void existingGroupWeekBestRejected() {
        given(loadWeeklyCurriculumPort.getById(20L)).willReturn(weekly);
        given(getStudyGroupUseCase.getById(GROUP_ID)).willReturn(StudyGroupInfo.create(
            GROUP_ID, "그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH, List.of(50L), List.of(MEMBER_ID)
        ));
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(
            ChallengerInfo.builder()
                .memberId(MEMBER_ID)
                .gisuId(9L)
                .part(ChallengerPart.SPRINGBOOT)
                .challengerStatus(ChallengerStatus.ACTIVE)
                .build()
        ));
        givenDecisionAuthority();
        given(loadWeeklyBestWorkbookPort.existsByWeeklyCurriculumIdAndStudyGroupId(20L, GROUP_ID))
            .willReturn(true);

        assertThatThrownBy(() -> service.selectBest(command(GROUP_ID)))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.WEEKLY_BEST_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("선정 대상이 요청된 스터디 그룹 구성원이 아니면 거부한다")
    void memberOutsideGroup_rejected() {
        given(loadWeeklyCurriculumPort.getById(20L)).willReturn(weekly);
        given(getStudyGroupUseCase.getById(GROUP_ID)).willReturn(StudyGroupInfo.create(
            GROUP_ID, "그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH, List.of(50L), List.of(31L)
        ));

        assertThatThrownBy(() -> service.selectBest(command(GROUP_ID)))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.STUDY_GROUP_NOT_MATCHED);
    }

    private void givenCommonEligibility() {
        given(loadWeeklyCurriculumPort.getById(20L)).willReturn(weekly);
        given(getStudyGroupUseCase.getById(GROUP_ID)).willReturn(StudyGroupInfo.create(
            GROUP_ID, "그룹", 9L, ChallengerPart.SPRINGBOOT, Instant.EPOCH, List.of(50L), List.of(MEMBER_ID)
        ));
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(
            ChallengerInfo.builder()
                .memberId(MEMBER_ID)
                .gisuId(9L)
                .part(ChallengerPart.SPRINGBOOT)
                .challengerStatus(ChallengerStatus.ACTIVE)
                .build()
        ));
        givenDecisionAuthority();
        given(loadWeeklyBestWorkbookPort.existsByWeeklyCurriculumIdAndStudyGroupId(20L, GROUP_ID))
            .willReturn(false);
        given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumId(20L)).willReturn(List.of(original));
        given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(MEMBER_ID, List.of(100L)))
            .willReturn(List.of(workbook));
        given(loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(List.of(100L)))
            .willReturn(List.of(mission));
        given(loadMissionSubmissionPort.listActiveByChallengerWorkbookIdIn(List.of(300L)))
            .willReturn(List.of(submission));
    }

    private CreateWeeklyBestWorkbookCommand command(Long groupId) {
        return CreateWeeklyBestWorkbookCommand.builder()
            .decidedMemberId(50L)
            .bestMemberId(MEMBER_ID)
            .weeklyCurriculumId(20L)
            .studyGroupId(groupId)
            .reason("잘함")
            .build();
    }

    private void givenDecisionAuthority() {
        SubjectAttributes subject = SubjectAttributes.builder()
            .memberId(50L)
            .schoolId(100L)
            .build();
        given(checkPermissionUseCase.loadSubject(50L)).willReturn(subject);
        given(staffPolicy.canManage(subject, GROUP_ID, MEMBER_ID, 9L)).willReturn(true);
    }

    @Test
    void 같은_기수라도_다른_트랙_그룹의_베스트로_선정하지_못한다() {
        // given
        WeeklyCurriculum trackWeek = WeeklyCurriculum.create(
            Curriculum.createForTrack(9L, ChallengerTrack.WEB_PRODUCT_ENGINEER, "웹"),
            1L, false, "1주차", Instant.EPOCH, Instant.MAX);
        given(loadWeeklyCurriculumPort.getById(20L)).willReturn(trackWeek);
        given(getStudyGroupUseCase.getById(GROUP_ID)).willReturn(new StudyGroupInfo(
            GROUP_ID, "모바일 그룹", 9L, null, Instant.EPOCH, List.of(50L), List.of(MEMBER_ID),
            ChallengerTrack.MOBILE_PRODUCT_ENGINEER));

        // when / then
        assertThatThrownBy(() -> service.selectBest(command(GROUP_ID)))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode").isEqualTo(CurriculumErrorCode.STUDY_GROUP_NOT_MATCHED);
    }

    @Test
    void 그룹_트랙과_수강_트랙이_일치하면_기존_필수미션_조건으로_베스트를_선정한다() {
        // given
        Curriculum trackCurriculum = Curriculum.createForTrack(9L, ChallengerTrack.WEB_PRODUCT_ENGINEER, "웹");
        ReflectionTestUtils.setField(weekly, "curriculum", trackCurriculum);
        givenCommonEligibility();
        given(getStudyGroupUseCase.getById(GROUP_ID)).willReturn(new StudyGroupInfo(
            GROUP_ID, "웹 그룹", 9L, null, Instant.EPOCH, List.of(50L), List.of(MEMBER_ID),
            ChallengerTrack.WEB_PRODUCT_ENGINEER));
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(ChallengerInfo.builder()
            .memberId(MEMBER_ID).gisuId(9L).tracks(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER))
            .challengerStatus(ChallengerStatus.ACTIVE).build()));
        given(loadMissionFeedbackPort.listByMissionSubmissionIdIn(List.of(400L)))
            .willReturn(List.of(MissionFeedback.create(submission, 50L, "통과", FeedbackResult.PASS)));

        // when / then
        assertThatCode(() -> service.selectBest(command(GROUP_ID))).doesNotThrowAnyException();
        verify(saveWeeklyBestWorkbookPort).save(any(WeeklyBestWorkbook.class));
    }

}
