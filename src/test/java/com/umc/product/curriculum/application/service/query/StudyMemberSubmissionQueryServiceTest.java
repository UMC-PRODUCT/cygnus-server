package com.umc.product.curriculum.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionInfo;
import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionQuery;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort.BestWorkbookHolder;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.ChallengerWorkbookStatus;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberPageInfo;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyMemberSubmissionQueryServiceTest {

    private static final Long GISU_ID = 9L;
    private static final Long GROUP_ID = 10L;
    private static final Long WEEKLY_CURRICULUM_ID = 20L;
    private static final Long ORIGINAL_WORKBOOK_ID = 30L;
    private static final Long REQUESTER_ID = 1L;

    private static final Long PASSED_MEMBER = 100L;
    private static final Long FAILED_MEMBER = 200L;
    private static final Long NOT_DEPLOYED_MEMBER = 300L;

    @Mock
    private GetStudyGroupUseCase getStudyGroupUseCase;
    @Mock
    private GetGisuUseCase getGisuUseCase;
    @Mock
    private GetMemberUseCase getMemberUseCase;
    @Mock
    private LoadCurriculumPort loadCurriculumPort;
    @Mock
    private LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    @Mock
    private LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    @Mock
    private LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    @Mock
    private LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    @Mock
    private LoadMissionSubmissionPort loadMissionSubmissionPort;
    @Mock
    private LoadMissionFeedbackPort loadMissionFeedbackPort;
    @Mock
    private LoadWeeklyBestWorkbookPort loadWeeklyBestWorkbookPort;
    @InjectMocks
    private StudyMemberSubmissionQueryService service;

    private OriginalWorkbookMission mission;

    @BeforeEach
    void setUp() {
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            Curriculum.create(GISU_ID, ChallengerPart.SPRINGBOOT, "커리큘럼"), 3L, false, "3주차",
            Instant.EPOCH, Instant.parse("2026-08-01T00:00:00Z")
        );
        ReflectionTestUtils.setField(weekly, "id", WEEKLY_CURRICULUM_ID);

        OriginalWorkbook original = OriginalWorkbook.createAsDraft(
            weekly, "3주차 워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        ReflectionTestUtils.setField(original, "id", ORIGINAL_WORKBOOK_ID);

        mission = OriginalWorkbookMission.create(original, "미션", null, MissionType.LINK, true);
        ReflectionTestUtils.setField(mission, "id", 40L);

        // 그룹 하나에 스터디원 3명 — 통과 / 탈락 / 워크북 미배포
        given(getStudyGroupUseCase.getVisibleStudyGroupMembers(any(), any(), any(), anyInt()))
            .willReturn(List.of(
                studyMember(51L, PASSED_MEMBER),
                studyMember(52L, FAILED_MEMBER),
                studyMember(53L, NOT_DEPLOYED_MEMBER)
            ));
        given(getGisuUseCase.getActiveGisuId()).willReturn(GISU_ID);
        given(loadCurriculumPort.findByGisuIdAndPart(GISU_ID, ChallengerPart.SPRINGBOOT))
            .willReturn(Optional.of(new CurriculumProjection(1L, ChallengerPart.SPRINGBOOT, "커리큘럼")));
        given(loadWeeklyCurriculumPort.findByCurriculumId(1L, 3L)).willReturn(List.of(weekly));
        given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumIdIn(anyList()))
            .willReturn(List.of(original));
        given(loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(anyList()))
            .willReturn(List.of(mission));

        ChallengerWorkbook passedWorkbook = challengerWorkbook(original, PASSED_MEMBER, 61L);
        ChallengerWorkbook failedWorkbook = challengerWorkbook(original, FAILED_MEMBER, 62L);
        given(loadChallengerWorkbookPort.listByLookupKeys(anyList()))
            .willReturn(List.of(passedWorkbook, failedWorkbook));

        MissionSubmission passedSubmission = submission(passedWorkbook, 71L);
        MissionSubmission failedSubmission = submission(failedWorkbook, 72L);
        given(loadMissionSubmissionPort.listActiveByChallengerWorkbookIdIn(anyList()))
            .willReturn(List.of(passedSubmission, failedSubmission));
        given(loadMissionFeedbackPort.listByMissionSubmissionIdIn(anyList()))
            .willReturn(List.of(
                feedback(passedSubmission, FeedbackResult.PASS),
                feedback(failedSubmission, FeedbackResult.FAIL)
            ));

        given(loadWeeklyBestWorkbookPort.findHolders(any(), any()))
            .willReturn(List.of(new BestWorkbookHolder(GROUP_ID, WEEKLY_CURRICULUM_ID, PASSED_MEMBER)));

        given(getMemberUseCase.findAllByIds(any())).willReturn(Map.of(
            PASSED_MEMBER, member(PASSED_MEMBER, "김통과"),
            FAILED_MEMBER, member(FAILED_MEMBER, "박탈락"),
            NOT_DEPLOYED_MEMBER, member(NOT_DEPLOYED_MEMBER, "이미제출")
        ));
    }

    @Test
    @DisplayName("제출하지 않은 스터디원도 결과에 포함되며 워크북 ID 가 비어 있다")
    void includesMembersWithoutWorkbook() {
        List<StudyMemberSubmissionInfo> result = service.getStudyMemberSubmissions(query());

        assertThat(result).hasSize(3);

        StudyMemberSubmissionInfo notDeployed = findByMemberId(result, NOT_DEPLOYED_MEMBER);
        assertThat(notDeployed.weeks()).singleElement()
            .satisfies(week -> {
                assertThat(week.challengerWorkbookId()).isNull();
                assertThat(week.weeklyCurriculumTitle()).isEqualTo("3주차");
                assertThat(week.status()).isEqualTo(ChallengerWorkbookStatus.NOT_SUBMITTED);
                assertThat(week.isBest()).isFalse();
            });
    }

    @Test
    @DisplayName("피드백 결과에 따라 주차별 상태가 통과/탈락으로 갈린다")
    void resolvesStatusPerFeedback() {
        List<StudyMemberSubmissionInfo> result = service.getStudyMemberSubmissions(query());

        assertThat(findByMemberId(result, PASSED_MEMBER).weeks()).singleElement()
            .satisfies(week -> {
                assertThat(week.status()).isEqualTo(ChallengerWorkbookStatus.PASS);
                assertThat(week.weeklyCurriculumTitle()).isEqualTo("3주차");
            });
        assertThat(findByMemberId(result, FAILED_MEMBER).weeks()).singleElement()
            .satisfies(week -> assertThat(week.status()).isEqualTo(ChallengerWorkbookStatus.FAIL));
    }

    @Test
    @DisplayName("베스트 선정 여부는 통과 여부와 별개 필드로 내려간다")
    void bestIsIndependentOfStatus() {
        List<StudyMemberSubmissionInfo> result = service.getStudyMemberSubmissions(query());

        assertThat(findByMemberId(result, PASSED_MEMBER).weeks().get(0).isBest()).isTrue();
        assertThat(findByMemberId(result, FAILED_MEMBER).weeks().get(0).isBest()).isFalse();
    }

    @Test
    @DisplayName("멤버 도메인에서 이름·학교·프로필을 합성한다")
    void assemblesMemberInfo() {
        StudyMemberSubmissionInfo passed = findByMemberId(service.getStudyMemberSubmissions(query()), PASSED_MEMBER);

        assertThat(passed.memberName()).isEqualTo("김통과");
        assertThat(passed.schoolName()).isEqualTo("중앙대");
        assertThat(passed.studyGroupName()).isEqualTo("SpringBoot 스터디");
        assertThat(passed.part()).isEqualTo(ChallengerPart.SPRINGBOOT);
    }

    @Test
    @DisplayName("조회 가능한 스터디 그룹이 없으면 빈 목록을 반환한다")
    void noVisibleGroup_empty() {
        given(getStudyGroupUseCase.getVisibleStudyGroupMembers(any(), any(), any(), anyInt()))
            .willReturn(List.of());

        assertThat(service.getStudyMemberSubmissions(query())).isEmpty();
    }

    @Test
    @DisplayName("조회 가능 주차는 활성 기수 파트별 커리큘럼 주차의 union 을 distinct 오름차순으로 반환한다")
    void availableWeekNos_unionAcrossParts() {
        given(loadCurriculumPort.findByGisuIdAndPart(anyLong(), any())).willReturn(Optional.empty());
        given(loadCurriculumPort.findByGisuIdAndPart(GISU_ID, ChallengerPart.SPRINGBOOT))
            .willReturn(Optional.of(new CurriculumProjection(1L, ChallengerPart.SPRINGBOOT, "스프링 커리큘럼")));
        given(loadCurriculumPort.findByGisuIdAndPart(GISU_ID, ChallengerPart.WEB))
            .willReturn(Optional.of(new CurriculumProjection(2L, ChallengerPart.WEB, "웹 커리큘럼")));
        given(loadWeeklyCurriculumPort.findByCurriculumId(1L, null))
            .willReturn(List.of(weeklyOf(3L), weeklyOf(1L)));
        given(loadWeeklyCurriculumPort.findByCurriculumId(2L, null))
            .willReturn(List.of(weeklyOf(3L), weeklyOf(8L)));

        assertThat(service.getAvailableWeekNos(null)).containsExactly(1L, 3L, 8L);
    }

    @Test
    @DisplayName("그룹을 지정하면 그 그룹 파트의 주차만 반환한다")
    void availableWeekNos_groupSpecified_narrowsToGroupPart() {
        given(getStudyGroupUseCase.getById(GROUP_ID)).willReturn(StudyGroupInfo.create(
            GROUP_ID, "SpringBoot 스터디", GISU_ID, ChallengerPart.SPRINGBOOT, Instant.EPOCH, List.of(), List.of()
        ));
        given(loadCurriculumPort.findByGisuIdAndPart(GISU_ID, ChallengerPart.SPRINGBOOT))
            .willReturn(Optional.of(new CurriculumProjection(1L, ChallengerPart.SPRINGBOOT, "스프링 커리큘럼")));
        given(loadWeeklyCurriculumPort.findByCurriculumId(1L, null))
            .willReturn(List.of(weeklyOf(2L), weeklyOf(1L)));

        assertThat(service.getAvailableWeekNos(GROUP_ID)).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("커리큘럼이 없으면 빈 주차 목록을 반환한다")
    void availableWeekNos_noCurriculum_empty() {
        given(loadCurriculumPort.findByGisuIdAndPart(anyLong(), any())).willReturn(Optional.empty());

        assertThat(service.getAvailableWeekNos(null)).isEmpty();
    }

    private WeeklyCurriculum weeklyOf(Long weekNo) {
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            Curriculum.create(GISU_ID, ChallengerPart.SPRINGBOOT, "커리큘럼"), weekNo, false, weekNo + "주차",
            Instant.EPOCH, Instant.parse("2026-08-01T00:00:00Z")
        );
        ReflectionTestUtils.setField(weekly, "id", 900L + weekNo);
        return weekly;
    }

    private StudyMemberSubmissionQuery query() {
        return new StudyMemberSubmissionQuery(REQUESTER_ID, GROUP_ID, List.of(3L), null, 20);
    }

    private StudyMemberSubmissionInfo findByMemberId(List<StudyMemberSubmissionInfo> result, Long memberId) {
        return result.stream()
            .filter(info -> info.memberId().equals(memberId))
            .findFirst()
            .orElseThrow();
    }

    private StudyGroupMemberPageInfo studyMember(Long studyGroupMemberId, Long memberId) {
        return new StudyGroupMemberPageInfo(
            studyGroupMemberId, GROUP_ID, "SpringBoot 스터디", ChallengerPart.SPRINGBOOT, memberId
        );
    }

    private ChallengerWorkbook challengerWorkbook(OriginalWorkbook original, Long memberId, Long id) {
        ChallengerWorkbook workbook = ChallengerWorkbook.create(original, memberId, GROUP_ID);
        ReflectionTestUtils.setField(workbook, "id", id);
        return workbook;
    }

    private MissionSubmission submission(ChallengerWorkbook workbook, Long id) {
        MissionSubmission submission = MissionSubmission.create(mission, workbook, "https://github.com/pr/1");
        ReflectionTestUtils.setField(submission, "id", id);
        return submission;
    }

    private MissionFeedback feedback(MissionSubmission submission, FeedbackResult result) {
        MissionFeedback feedback = MissionFeedback.create(submission, REQUESTER_ID, "피드백", result);
        ReflectionTestUtils.setField(feedback, "id", submission.getId() + 1000L);
        return feedback;
    }

    private MemberInfo member(Long id, String name) {
        return MemberInfo.builder()
            .id(id)
            .name(name)
            .nickname(name + "닉")
            .schoolName("중앙대")
            .profileImageLink("https://cdn.example.com/" + id)
            .build();
    }
}
