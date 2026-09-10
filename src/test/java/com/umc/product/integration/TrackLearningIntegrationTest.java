package com.umc.product.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.out.LoadChallengerRecordPort;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.curriculum.application.port.in.command.ManageChallengerWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageMissionSubmissionUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageOriginalWorkbookMissionUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageOriginalWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateWeeklyCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.ChangeOriginalWorkbookStatusCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateOriginalWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeployChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateOriginalWorkbookMissionCommand;
import com.umc.product.curriculum.application.port.in.query.GetCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.query.GetOriginalWorkbookUseCase;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.organization.application.port.in.command.ManageGisuUseCase;
import com.umc.product.organization.application.port.in.command.ManageStudyGroupUseCase;
import com.umc.product.organization.application.port.in.command.dto.CreateGisuCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateStudyGroupCommand;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.out.command.SaveGisuPort;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.SchoolFixture;

class TrackLearningIntegrationTest extends IntegrationTestSupport {

    @Autowired private ManageGisuUseCase manageGisuUseCase;
    @Autowired private SaveGisuPort saveGisuPort;
    @Autowired private LoadGisuPort loadGisuPort;
    @Autowired private ChapterFixture chapterFixture;
    @Autowired private SchoolFixture schoolFixture;
    @Autowired private SaveMemberPort saveMemberPort;
    @Autowired private ManageChallengerRecordUseCase manageChallengerRecordUseCase;
    @Autowired private LoadChallengerRecordPort loadChallengerRecordPort;
    @Autowired private GetChallengerUseCase getChallengerUseCase;
    @Autowired private ManageStudyGroupUseCase manageStudyGroupUseCase;
    @Autowired private GetStudyGroupUseCase getStudyGroupUseCase;
    @Autowired private ManageCurriculumUseCase manageCurriculumUseCase;
    @Autowired private ManageWeeklyCurriculumUseCase manageWeeklyCurriculumUseCase;
    @Autowired private ManageOriginalWorkbookUseCase manageOriginalWorkbookUseCase;
    @Autowired private ManageOriginalWorkbookMissionUseCase manageOriginalWorkbookMissionUseCase;
    @Autowired private ManageChallengerWorkbookUseCase manageChallengerWorkbookUseCase;
    @Autowired private ManageMissionSubmissionUseCase manageMissionSubmissionUseCase;
    @Autowired private GetCurriculumUseCase getCurriculumUseCase;
    @Autowired private GetOriginalWorkbookUseCase getOriginalWorkbookUseCase;

    @MockitoBean
    private SendWebhookAlarmUseCase sendWebhookAlarmUseCase;

    @ParameterizedTest
    @EnumSource(GisuLearningType.class)
    @DisplayName("기수의 Part 또는 Track 코드 등록부터 스터디 배정과 워크북 제출까지 연결된다")
    void registerAndStudy(GisuLearningType learningType) {
        // Given
        Instant now = Instant.now();
        Long gisuId = learningType == GisuLearningType.TRACK
            ? manageGisuUseCase.create(new CreateGisuCommand(
                99L, now.minus(1, ChronoUnit.DAYS), now.plus(90, ChronoUnit.DAYS)))
            : saveGisuPort.save(Gisu.create(
                99L, now.minus(1, ChronoUnit.DAYS), now.plus(90, ChronoUnit.DAYS), false,
                GisuLearningType.PART)).getId();
        var chapter = chapterFixture.지부(loadGisuPort.getById(gisuId), "신규 지부");
        var school = schoolFixture.지부에_소속된_학교("학습 학교", chapter);
        var member = saveMemberPort.save(Member.create(
            "학습자", "학습자", "learner@example.com", school.getId(), null));
        var mentor = saveMemberPort.save(Member.create(
            "멘토", "멘토", "mentor@example.com", school.getId(), null));
        ChallengerPart part = learningType == GisuLearningType.PART ? ChallengerPart.WEB : null;
        ChallengerTrack track = learningType == GisuLearningType.TRACK
            ? ChallengerTrack.WEB_PRODUCT_ENGINEER : null;
        Long curriculumId = manageCurriculumUseCase.create(CreateCurriculumCommand.builder()
            .gisuId(gisuId).part(part).track(track).title("새 기수 웹 커리큘럼").build());
        Long weeklyId = manageWeeklyCurriculumUseCase.create(CreateWeeklyCurriculumCommand.builder()
            .curriculumId(curriculumId).weekNo(1L).isExtra(false).title("첫 주차")
            .startsAt(now.minus(1, ChronoUnit.DAYS)).endsAt(now.plus(7, ChronoUnit.DAYS)).build());
        Long originalId = manageOriginalWorkbookUseCase.create(CreateOriginalWorkbookCommand.builder()
            .weeklyCurriculumId(weeklyId).title("첫 워크북").content("학습 내용")
            .type(OriginalWorkbookType.MAIN).initialStatus(OriginalWorkbookStatus.DRAFT).build());
        Long missionId = manageOriginalWorkbookMissionUseCase.create(CreateOriginalWorkbookMissionCommand.builder()
            .originalWorkbookId(originalId).title("실습 결과").missionType(MissionType.MEMO)
            .isNecessary(true).build());

        // When
        Long recordId = manageChallengerRecordUseCase.create(CreateChallengerRecordCommand.builder()
            .gisuId(gisuId).chapterId(chapter.getId()).schoolId(school.getId())
            .memberName(member.getName()).creatorMemberId(mentor.getId()).part(part).track(track).build());
        String code = loadChallengerRecordPort.getById(recordId).getCode();
        manageChallengerRecordUseCase.consumeCode(new ConsumeChallengerRecordCommand(member.getId(), code));
        manageStudyGroupUseCase.create(new CreateStudyGroupCommand(
            "웹 스터디", gisuId, part, Set.of(mentor.getId()), Set.of(member.getId()), track));
        var group = learningType == GisuLearningType.PART
            ? getStudyGroupUseCase.findByMemberIdAndGisuIdAndPart(member.getId(), gisuId, part).orElseThrow()
            : getStudyGroupUseCase.findByMemberIdAndGisuIdAndTrack(member.getId(), gisuId, track).orElseThrow();
        for (OriginalWorkbookStatus status : List.of(OriginalWorkbookStatus.READY, OriginalWorkbookStatus.RELEASED)) {
            manageOriginalWorkbookUseCase.changeStatusForRelease(List.of(
                new ChangeOriginalWorkbookStatusCommand(originalId, status, mentor.getId())));
        }
        var request = new DeployChallengerWorkbookCommand(List.of(originalId), member.getId());
        var workbook = manageChallengerWorkbookUseCase.batchDeploy(request).getFirst();
        Long submissionId = manageMissionSubmissionUseCase.create(CreateMissionSubmissionCommand.builder()
            .challengerWorkbookId(workbook.challengerWorkbookId()).originalWorkbookMissionId(missionId)
            .requesterMemberId(member.getId()).content("실습을 마쳤습니다.").build());

        // Then
        assertThat(loadChallengerRecordPort.getById(recordId).isUsed()).isTrue();
        var challenger = getChallengerUseCase.getByMemberIdAndGisuId(member.getId(), gisuId);
        assertThat(challenger.part()).isEqualTo(part);
        if (track != null) {
            assertThat(challenger.tracks()).containsExactly(track);
        }
        assertThat(group.part()).isEqualTo(part);
        assertThat(group.track()).isEqualTo(track);
        assertThat(workbook.receivedStudyGroupId()).isEqualTo(group.groupId());
        assertThat(getOriginalWorkbookUseCase.getById(originalId, member.getId()).title()).isEqualTo("첫 워크북");
        assertThat(manageChallengerWorkbookUseCase.batchDeploy(request).getFirst().challengerWorkbookId())
            .isEqualTo(workbook.challengerWorkbookId());
        var progress = getCurriculumUseCase.getMyProgress(member.getId(), gisuId);
        assertThat(progress.curriculumId()).isEqualTo(curriculumId);
        assertThat(progress.track()).isEqualTo(track);
        var submittedMission = progress.weeks().getFirst().releasedOriginalWorkbooks().getFirst().missions().getFirst();
        assertThat(submittedMission.hasSubmission()).isTrue();
        assertThat(submissionId).isNotNull();
    }
}
