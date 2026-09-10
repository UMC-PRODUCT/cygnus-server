package com.umc.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.authorization.application.port.in.query.CheckChallengerAuthorityUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.curriculum.application.port.in.command.ManageCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateCurriculumCommand;
import com.umc.product.curriculum.application.port.in.query.GetCurriculumUseCase;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.organization.application.port.out.command.SaveGisuPort;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.SchoolFixture;

@TestPropertySource(properties = "app.seed.enabled=true")
class TrackSeedIntegrationTest extends IntegrationTestSupport {

    @Autowired private SaveGisuPort saveGisuPort;
    @Autowired private ChapterFixture chapterFixture;
    @Autowired private SchoolFixture schoolFixture;
    @Autowired private SaveMemberPort saveMemberPort;
    @Autowired private GetMemberUseCase getMemberUseCase;
    @Autowired private GetChallengerUseCase getChallengerUseCase;
    @Autowired private CheckChallengerAuthorityUseCase checkChallengerAuthorityUseCase;
    @Autowired private GetCurriculumUseCase getCurriculumUseCase;
    @Autowired private ManageCurriculumUseCase manageCurriculumUseCase;
    @Autowired private LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    @Autowired private LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;

    @MockitoBean
    private SendWebhookAlarmUseCase sendWebhookAlarmUseCase;

    private Long gisuId;
    private Long chapterId;
    private Long schoolId;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        var gisu = saveGisuPort.save(Gisu.create(
            99L, now.minus(1, ChronoUnit.DAYS), now.plus(90, ChronoUnit.DAYS), false, GisuLearningType.TRACK));
        var chapter = chapterFixture.지부(gisu, "시딩 지부");
        var school = schoolFixture.지부에_소속된_학교("시딩 학교", chapter);
        gisuId = gisu.getId();
        chapterId = chapter.getId();
        schoolId = school.getId();
    }

    @Test
    @DisplayName("단건 시딩으로 한 챌린저의 복수 Track 수강 정보를 저장한다")
    void seedChallengerWithMultipleTracks() throws Exception {
        // Given
        var member = saveMemberPort.save(Member.create(
            "학습자", "학습자", "track-seed@example.com", schoolId, null));
        String request = """
            {"memberId": %d, "gisuId": %d, "tracks": ["WEB_PRODUCT_ENGINEER", "DESIGN"]}
            """.formatted(member.getId(), gisuId);

        // When
        mockMvc.perform(post("/test/seed/challenger")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Then
        var challenger = getChallengerUseCase.getByMemberIdAndGisuId(member.getId(), gisuId);
        assertThat(challenger.part()).isNull();
        assertThat(challenger.tracks()).containsExactlyInAnyOrder(
            ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.DESIGN);
    }

    @Test
    @DisplayName("수강 없는 기수 소속을 생성한 뒤 역할을 부여해야 학교 회장단 권한을 얻는다")
    void seedStaffWithoutTracksThenAssignRole() throws Exception {
        // Given
        var member = saveMemberPort.save(Member.create(
            "학교회장", "학교회장", "staff-seed@example.com", schoolId, null));
        String request = """
            {"memberId": %d, "gisuId": %d, "tracks": []}
            """.formatted(member.getId(), gisuId);

        // When
        mockMvc.perform(post("/test/seed/challenger")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk());

        // Then
        var challenger = getChallengerUseCase.getByMemberIdAndGisuId(member.getId(), gisuId);
        assertThat(challenger.part()).isNull();
        assertThat(challenger.tracks()).isEmpty();
        assertThat(checkChallengerAuthorityUseCase.isSchoolCoreInGisu(member.getId(), gisuId, schoolId)).isFalse();

        // When
        String roleRequest = """
            {"challengerId": %d, "gisuId": %d, "roleType": "SCHOOL_PRESIDENT", "organizationId": %d}
            """.formatted(challenger.challengerId(), gisuId, schoolId);
        mockMvc.perform(post("/test/seed/challenger-role")
                .contentType(MediaType.APPLICATION_JSON).content(roleRequest))
            .andExpect(status().isOk());

        // Then
        assertThat(checkChallengerAuthorityUseCase.isSchoolCoreInGisu(member.getId(), gisuId, schoolId)).isTrue();
        assertThat(checkChallengerAuthorityUseCase.isCentralMemberInGisu(member.getId(), gisuId)).isFalse();
    }

    @Test
    @DisplayName("Part와 tracks를 모두 누락하면 수강 없는 소속으로 간주하지 않고 거부한다")
    void rejectMissingLearningSelection() throws Exception {
        // Given
        var member = saveMemberPort.save(Member.create(
            "학습자", "학습자", "missing-track@example.com", schoolId, null));
        String request = """
            {"memberId": %d, "gisuId": %d}
            """.formatted(member.getId(), gisuId);

        // When
        mockMvc.perform(post("/test/seed/challenger")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isBadRequest());

        // Then
        assertThat(getChallengerUseCase.findByMemberIdAndGisuId(member.getId(), gisuId)).isEmpty();
    }

    @Test
    @DisplayName("기존 Part 기수에서는 빈 tracks로 Part 필수 조건을 우회할 수 없다")
    void rejectEmptyTracksInPartGisu() throws Exception {
        // Given
        Instant now = Instant.now();
        var partGisu = saveGisuPort.save(Gisu.create(
            98L, now.minus(100, ChronoUnit.DAYS), now.minus(10, ChronoUnit.DAYS), false));
        var member = saveMemberPort.save(Member.create(
            "학습자", "학습자", "empty-part@example.com", schoolId, null));
        String request = """
            {"memberId": %d, "gisuId": %d, "tracks": []}
            """.formatted(member.getId(), partGisu.getId());

        // When
        mockMvc.perform(post("/test/seed/challenger")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isBadRequest());

        // Then
        assertThat(getChallengerUseCase.findByMemberIdAndGisuId(member.getId(), partGisu.getId())).isEmpty();
    }

    @Test
    @DisplayName("Track 분포 시딩으로 학교별 회원과 해당 Track 챌린저를 함께 생성한다")
    void seedChallengersByTrackAndSchool() throws Exception {
        // Given
        long memberCount = getMemberUseCase.countAll();
        String request = """
            {"gisuId": %d, "countPerTrackPerSchool": 1,
             "tracks": ["WEB_PRODUCT_ENGINEER"], "chapterIds": [%d]}
            """.formatted(gisuId, chapterId);

        // When
        mockMvc.perform(post("/test/seed/challengers")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.totalCreated").value(1))
            .andExpect(jsonPath("$.result.totalFailed").value(0))
            .andExpect(jsonPath("$.result.perCellSummary[0].track").value("WEB_PRODUCT_ENGINEER"));

        // Then
        assertThat(getMemberUseCase.countAll()).isEqualTo(memberCount + 1);
        var challengers = getChallengerUseCase.getAllByGisuId(gisuId);
        assertThat(challengers).singleElement().satisfies(challenger -> {
            assertThat(challenger.part()).isNull();
            assertThat(challenger.tracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
            assertThat(getMemberUseCase.listIdsBySchoolId(schoolId)).contains(challenger.memberId());
        });
    }

    @Test
    @DisplayName("Track 기수에 Part 분포 시딩을 요청하면 회원 생성 전에 거부한다")
    void rejectPartSeedWithoutLeavingMembers() throws Exception {
        // Given
        long memberCount = getMemberUseCase.countAll();
        String request = """
            {"gisuId": %d, "countPerPartPerSchool": 1,
             "parts": ["WEB"], "chapterIds": [%d]}
            """.formatted(gisuId, chapterId);

        // When
        mockMvc.perform(post("/test/seed/challengers")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isBadRequest());

        // Then
        assertThat(getMemberUseCase.countAll()).isEqualTo(memberCount);
        assertThat(getChallengerUseCase.getAllByGisuId(gisuId)).isEmpty();
    }

    @Test
    @DisplayName("Track 커리큘럼 시딩으로 주차별 원본 워크북과 미션까지 저장한다")
    void seedTrackCurriculumWithWorkbooksAndMissions() throws Exception {
        // Given
        String request = """
            {"gisuId": %d, "tracks": ["WEB_PRODUCT_ENGINEER"],
             "weeksPerCurriculum": 2, "missionsPerWorkbook": 1}
            """.formatted(gisuId);

        // When
        var response = mockMvc.perform(post("/test/seed/curriculum")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.createdCurriculumIds.length()").value(1))
            .andExpect(jsonPath("$.result.createdWeeklyCurriculumIds.length()").value(2))
            .andExpect(jsonPath("$.result.createdOriginalWorkbookIds.length()").value(2))
            .andExpect(jsonPath("$.result.createdMissionIds.length()").value(2))
            .andExpect(jsonPath("$.result.curriculumFailed").value(0))
            .andExpect(jsonPath("$.result.weeklyCurriculumFailed").value(0))
            .andExpect(jsonPath("$.result.originalWorkbookFailed").value(0))
            .andExpect(jsonPath("$.result.missionFailed").value(0))
            .andReturn().getResponse();

        // Then
        JsonNode result = objectMapper.readTree(response.getContentAsString()).path("result");
        var curriculum = getCurriculumUseCase.getCurriculumOverview(
            gisuId, null, ChallengerTrack.WEB_PRODUCT_ENGINEER, null);
        assertThat(curriculum.curriculumId()).isEqualTo(result.path("createdCurriculumIds").get(0).asLong());
        assertThat(curriculum.part()).isNull();
        assertThat(curriculum.track()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(curriculum.weeks()).extracting(week -> week.weekNo()).containsExactly(1L, 2L);
        var weeklyIds = curriculum.weeks().stream().map(week -> week.weeklyCurriculumId()).toList();
        for (JsonNode workbookId : result.path("createdOriginalWorkbookIds")) {
            var workbook = loadOriginalWorkbookPort.getById(workbookId.asLong());
            assertThat(weeklyIds).contains(workbook.getWeeklyCurriculum().getId());
            assertThat(workbook.getType()).isEqualTo(OriginalWorkbookType.MAIN);
            assertThat(workbook.getOriginalWorkbookStatus()).isEqualTo(OriginalWorkbookStatus.READY);
            assertThat(loadOriginalWorkbookMissionPort.findByOriginalWorkbookId(workbook.getId()))
                .singleElement().satisfies(mission -> assertThat(mission.isNecessary()).isTrue());
        }
    }

    @Test
    @DisplayName("미리 등록한 Track 커리큘럼의 제목을 보존하며 주차를 시딩하고 재시딩은 거부한다")
    void seedExistingEmptyTrackCurriculumWithoutOverwritingContent() throws Exception {
        // Given
        Long curriculumId = manageCurriculumUseCase.create(CreateCurriculumCommand.builder()
            .gisuId(gisuId).track(ChallengerTrack.WEB_PRODUCT_ENGINEER).title("운영진이 준비한 웹 커리큘럼").build());
        String request = """
            {"gisuId": %d, "tracks": ["WEB_PRODUCT_ENGINEER"],
             "weeksPerCurriculum": 2, "missionsPerWorkbook": 1}
            """.formatted(gisuId);

        // When
        var response = mockMvc.perform(post("/test/seed/curriculum")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.createdCurriculumIds.length()").value(0))
            .andExpect(jsonPath("$.result.createdWeeklyCurriculumIds.length()").value(2))
            .andExpect(jsonPath("$.result.createdOriginalWorkbookIds.length()").value(2))
            .andExpect(jsonPath("$.result.createdMissionIds.length()").value(2))
            .andExpect(jsonPath("$.result.curriculumFailed").value(0))
            .andReturn().getResponse();

        // Then
        var curriculum = getCurriculumUseCase.getCurriculumOverview(
            gisuId, null, ChallengerTrack.WEB_PRODUCT_ENGINEER, null);
        assertThat(curriculum.curriculumId()).isEqualTo(curriculumId);
        assertThat(curriculum.title()).isEqualTo("운영진이 준비한 웹 커리큘럼");
        var weeklyIds = curriculum.weeks().stream().map(week -> week.weeklyCurriculumId()).toList();
        JsonNode result = objectMapper.readTree(response.getContentAsString()).path("result");
        for (JsonNode workbookId : result.path("createdOriginalWorkbookIds")) {
            var workbook = loadOriginalWorkbookPort.getById(workbookId.asLong());
            assertThat(weeklyIds).contains(workbook.getWeeklyCurriculum().getId());
            assertThat(loadOriginalWorkbookMissionPort.findByOriginalWorkbookId(workbook.getId())).hasSize(1);
        }

        // When
        mockMvc.perform(post("/test/seed/curriculum")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.curriculumFailed").value(1))
            .andExpect(jsonPath("$.result.createdWeeklyCurriculumIds.length()").value(0))
            .andExpect(jsonPath("$.result.createdOriginalWorkbookIds.length()").value(0));

        // Then
        assertThat(getCurriculumUseCase.getCurriculumOverview(
            gisuId, null, ChallengerTrack.WEB_PRODUCT_ENGINEER, null)).isEqualTo(curriculum);
    }
}
