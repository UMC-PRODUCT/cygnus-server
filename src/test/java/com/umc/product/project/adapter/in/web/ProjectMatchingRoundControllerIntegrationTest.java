package com.umc.product.project.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.project.adapter.out.persistence.ProjectApplicationJpaRepository;
import com.umc.product.project.adapter.out.scheduler.MatchingRoundDeadlineScheduler;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPort;
import com.umc.product.project.application.port.out.SaveProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.SaveProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.ChallengerFixture;
import com.umc.product.support.fixture.ChallengerRoleFixture;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.MemberFixture;

@AutoConfigureMockMvc(addFilters = false)
class ProjectMatchingRoundControllerIntegrationTest extends IntegrationTestSupport {

    private static final String BASE_URL = "/api/v1/project/matching-rounds";

    @Autowired
    private MemberFixture memberFixture;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private ChapterFixture chapterFixture;

    @Autowired
    private ChallengerFixture challengerFixture;

    @Autowired
    private ChallengerRoleFixture challengerRoleFixture;

    @Autowired
    private SaveProjectMatchingRoundPort saveProjectMatchingRoundPort;

    @Autowired
    private LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;

    @Autowired
    private SaveProjectPort saveProjectPort;

    @Autowired
    private SaveProjectApplicationFormPort saveProjectApplicationFormPort;

    @Autowired
    private ProjectApplicationJpaRepository projectApplicationJpaRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("test profile에서는 매칭 데드라인 background scheduler를 등록하지 않는다")
    void test_profile에서는_matching_deadline_scheduler를_등록하지_않는다() {
        assertThat(applicationContext.getBeansOfType(MatchingRoundDeadlineScheduler.class)).isEmpty();
    }

    @Test
    @DisplayName("지부장이 본인 지부에 매칭 차수를 생성할 수 있다")
    void 지부장이_본인_지부에_매칭_차수를_생성한다() throws Exception {
        // given
        OperatorContext context = chapterPresidentContext();
        authenticate(context.member().getId());
        int rowCountBefore = loadProjectMatchingRoundPort.listAll().size();

        // when
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(matchingRoundRequest(
                    "기획-디자인 1차 매칭",
                    "생성 성공 요청",
                    MatchingType.PLAN_DESIGN,
                    MatchingPhase.FIRST,
                    context.chapter().getId(),
                    "2026-05-10T00:00:00Z",
                    "2026-05-12T00:00:00Z",
                    "2026-05-13T00:00:00Z"
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.matchingRoundId").isString());

        // then
        List<ProjectMatchingRound> matchingRounds = loadProjectMatchingRoundPort.listAll();
        assertThat(matchingRounds).hasSize(rowCountBefore + 1);
        assertThat(matchingRounds)
            .extracting(ProjectMatchingRound::getGisuId)
            .contains(context.gisu().getId());
    }

    @Test
    @DisplayName("기수 ID가 없는 매칭 차수 생성 요청은 거부한다")
    void 기수_ID가_없는_매칭_차수_생성_요청은_거부한다() throws Exception {
        OperatorContext context = chapterPresidentContext();
        authenticate(context.member().getId());
        int rowCountBefore = loadProjectMatchingRoundPort.listAll().size();

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(matchingRoundRequestWithoutGisu(
                    "기획-디자인 1차 매칭",
                    "기수 ID 누락",
                    MatchingType.PLAN_DESIGN,
                    MatchingPhase.FIRST,
                    context.chapter().getId(),
                    "2026-05-10T00:00:00Z",
                    "2026-05-12T00:00:00Z",
                    "2026-05-13T00:00:00Z"
                )))
            .andExpect(status().isBadRequest());

        assertThat(loadProjectMatchingRoundPort.listAll()).hasSize(rowCountBefore);
    }

    @Test
    @DisplayName("관리 권한이 없는 지부에는 매칭 차수를 생성할 수 없다")
    void 관리_권한이_없는_지부에는_매칭_차수를_생성할_수_없다() throws Exception {
        // given
        OperatorContext context = chapterPresidentContext();
        Chapter otherChapter = chapterFixture.지부(context.gisu(), "other");
        authenticate(context.member().getId());

        // when & then
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(matchingRoundRequest(
                    "기획-디자인 1차 매칭",
                    "관리 권한이 없는 지부에 생성 요청",
                    MatchingType.PLAN_DESIGN,
                    MatchingPhase.FIRST,
                    otherChapter.getId(),
                    "2026-05-10T00:00:00Z",
                    "2026-05-12T00:00:00Z",
                    "2026-05-13T00:00:00Z"
                )))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT-0303"));
    }

    @Test
    @DisplayName("같은 지부에서 기간이 겹치는 매칭 차수는 생성할 수 없다")
    void 같은_지부에서_기간이_겹치는_매칭_차수는_생성할_수_없다() throws Exception {
        // given
        OperatorContext context = chapterPresidentContext();
        saveMatchingRound(
            "기획-디자인 1차 매칭",
            "기존 매칭 차수",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            context.chapter().getId(),
            "2026-05-10T00:00:00Z",
            "2026-05-12T00:00:00Z",
            "2026-05-13T00:00:00Z"
        );
        authenticate(context.member().getId());

        // when & then
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(matchingRoundRequest(
                    "기획-개발자 1차 매칭",
                    "기존 매칭 차수 기간과 일부 겹치는 요청",
                    MatchingType.PLAN_DEVELOPER,
                    MatchingPhase.FIRST,
                    context.chapter().getId(),
                    "2026-05-11T00:00:00Z",
                    "2026-05-14T00:00:00Z",
                    "2026-05-15T00:00:00Z"
                )))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT-0302"));
    }

    @Test
    @DisplayName("startsAt이 endsAt보다 늦으면 매칭 차수를 생성할 수 없다")
    void startsAt이_endsAt보다_늦으면_매칭_차수를_생성할_수_없다() throws Exception {
        // given
        OperatorContext context = chapterPresidentContext();
        authenticate(context.member().getId());

        // when & then
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(matchingRoundRequest(
                    "기획-디자인 2차 매칭",
                    "startsAt이 endsAt보다 늦은 잘못된 요청",
                    MatchingType.PLAN_DESIGN,
                    MatchingPhase.SECOND,
                    context.chapter().getId(),
                    "2026-05-12T00:00:00Z",
                    "2026-05-10T00:00:00Z",
                    "2026-05-13T00:00:00Z"
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT-0301"));
    }

    @Test
    @DisplayName("endsAt이 decisionDeadline보다 늦으면 매칭 차수를 생성할 수 없다")
    void endsAt이_decisionDeadline보다_늦으면_매칭_차수를_생성할_수_없다() throws Exception {
        // given
        OperatorContext context = chapterPresidentContext();
        authenticate(context.member().getId());

        // when & then
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(matchingRoundRequest(
                    "기획-개발자 2차 매칭",
                    "endsAt이 decisionDeadline보다 늦은 잘못된 요청",
                    MatchingType.PLAN_DEVELOPER,
                    MatchingPhase.SECOND,
                    context.chapter().getId(),
                    "2026-05-14T00:00:00Z",
                    "2026-05-18T00:00:00Z",
                    "2026-05-17T00:00:00Z"
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT-0301"));
    }

    @Test
    @DisplayName("매칭 차수를 부분 수정하면 제공되지 않은 값은 기존 값을 유지한다")
    void 매칭_차수를_부분_수정하면_제공되지_않은_값은_기존_값을_유지한다() throws Exception {
        // given
        OperatorContext context = chapterPresidentContext();
        ProjectMatchingRound matchingRound = saveMatchingRound(
            "기획-디자인 1차 매칭",
            "수정 전",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            context.chapter().getId(),
            "2026-05-10T00:00:00Z",
            "2026-05-12T00:00:00Z",
            "2026-05-13T00:00:00Z"
        );
        authenticate(context.member().getId());

        // when
        mockMvc.perform(patch(BASE_URL + "/{matchingRoundId}", matchingRound.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(Map.of(
                    "name", "기획-디자인 1차 매칭 수정",
                    "startsAt", "2026-05-20T00:00:00Z",
                    "endsAt", "2026-05-22T00:00:00Z",
                    "decisionDeadline", "2026-05-23T00:00:00Z"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // then
        entityManager.clear();
        ProjectMatchingRound updated = loadProjectMatchingRoundPort.getById(matchingRound.getId());
        assertThat(updated.getName()).isEqualTo("기획-디자인 1차 매칭 수정");
        assertThat(updated.getDescription()).isEqualTo("수정 전");
        assertThat(updated.getType()).isEqualTo(MatchingType.PLAN_DESIGN);
        assertThat(updated.getPhase()).isEqualTo(MatchingPhase.FIRST);
        assertThat(updated.getStartsAt()).isEqualTo(Instant.parse("2026-05-20T00:00:00Z"));
        assertThat(updated.getEndsAt()).isEqualTo(Instant.parse("2026-05-22T00:00:00Z"));
        assertThat(updated.getDecisionDeadline()).isEqualTo(Instant.parse("2026-05-23T00:00:00Z"));
        assertThat(updated.getChapterId()).isEqualTo(context.chapter().getId());
    }

    @Test
    @DisplayName("매칭 차수 부분 수정 결과의 기간 순서가 잘못되면 실패한다")
    void 매칭_차수_부분_수정_결과의_기간_순서가_잘못되면_실패한다() throws Exception {
        // given
        OperatorContext context = chapterPresidentContext();
        ProjectMatchingRound matchingRound = saveMatchingRound(
            "기획-디자인 1차 매칭",
            "수정 전",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            context.chapter().getId(),
            "2026-05-10T00:00:00Z",
            "2026-05-12T00:00:00Z",
            "2026-05-13T00:00:00Z"
        );
        authenticate(context.member().getId());

        // when & then
        mockMvc.perform(patch(BASE_URL + "/{matchingRoundId}", matchingRound.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(Map.of("startsAt", "2026-05-14T00:00:00Z"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT-0301"));
    }

    @Test
    @DisplayName("매칭 차수 수정 요청에 chapterId를 포함하면 실패한다")
    void 매칭_차수_수정_요청에_chapterId를_포함하면_실패한다() throws Exception {
        // given
        OperatorContext context = chapterPresidentContext();
        ProjectMatchingRound matchingRound = saveMatchingRound(
            "기획-디자인 1차 매칭",
            "수정 전",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            context.chapter().getId(),
            "2026-05-10T00:00:00Z",
            "2026-05-12T00:00:00Z",
            "2026-05-13T00:00:00Z"
        );
        authenticate(context.member().getId());

        // when & then
        mockMvc.perform(patch(BASE_URL + "/{matchingRoundId}", matchingRound.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(Map.of("chapterId", 999L))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("매칭 차수 수정 요청에 gisuId를 포함하면 실패한다")
    void 매칭_차수_수정_요청에_gisuId를_포함하면_실패한다() throws Exception {
        OperatorContext context = chapterPresidentContext();
        ProjectMatchingRound matchingRound = saveMatchingRound(
            "기획-디자인 1차 매칭",
            "수정 전",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            context.chapter().getId(),
            "2026-05-10T00:00:00Z",
            "2026-05-12T00:00:00Z",
            "2026-05-13T00:00:00Z"
        );
        authenticate(context.member().getId());
        int rowCountBefore = loadProjectMatchingRoundPort.listAll().size();

        mockMvc.perform(patch(BASE_URL + "/{matchingRoundId}", matchingRound.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(Map.of("gisuId", context.gisu().getId()))))
            .andExpect(status().isBadRequest());

        entityManager.clear();
        assertThat(loadProjectMatchingRoundPort.listAll()).hasSize(rowCountBefore);
        assertThat(loadProjectMatchingRoundPort.getById(matchingRound.getId()).getGisuId())
            .isEqualTo(context.gisu().getId());
    }

    @Test
    @DisplayName("지원서가 연결된 매칭 차수는 삭제할 수 없다")
    void 지원서가_연결된_매칭_차수는_삭제할_수_없다() throws Exception {
        // given
        OperatorContext context = chapterPresidentContext();
        ProjectMatchingRound matchingRound = saveMatchingRound(
            "기획-디자인 1차 매칭",
            "지원서가 연결된 매칭 차수",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            context.chapter().getId(),
            "2026-05-10T00:00:00Z",
            "2026-05-12T00:00:00Z",
            "2026-05-13T00:00:00Z"
        );
        saveProjectApplication(context, matchingRound);
        authenticate(context.member().getId());

        // when & then
        mockMvc.perform(delete(BASE_URL + "/{matchingRoundId}", matchingRound.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT-0304"));
    }

    @Test
    @DisplayName("chapterId 기준으로 해당 지부의 매칭 차수 목록을 조회한다")
    void chapterId_기준으로_해당_지부의_매칭_차수_목록을_조회한다() throws Exception {
        // given
        authenticate(memberFixture.일반("listchapter").getId());
        Gisu gisu = gisuDuring2026(10L);
        Chapter chapter = chapterFixture.지부(gisu, "owned");
        Chapter otherChapter = chapterFixture.지부(gisu, "other");
        ProjectMatchingRound first = saveMatchingRound(
            "기획-디자인 1차 매칭",
            "1지부 기획-디자인 1차 매칭",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            chapter.getId(),
            "2026-05-10T00:00:00Z",
            "2026-05-12T00:00:00Z",
            "2026-05-13T00:00:00Z"
        );
        ProjectMatchingRound second = saveMatchingRound(
            "기획-디자인 2차 매칭",
            "1지부 기획-디자인 2차 매칭",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.SECOND,
            chapter.getId(),
            "2026-05-20T00:00:00Z",
            "2026-05-22T00:00:00Z",
            "2026-05-23T00:00:00Z"
        );
        saveMatchingRound(
            "다른 지부 기획-디자인 1차 매칭",
            "다른 지부 매칭 차수",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            otherChapter.getId(),
            "2026-05-10T00:00:00Z",
            "2026-05-12T00:00:00Z",
            "2026-05-13T00:00:00Z"
        );

        // when & then
        mockMvc.perform(get(BASE_URL).param("chapterId", chapter.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result", hasSize(2)))
            .andExpect(jsonPath("$.result[0].id").value(first.getId().toString()))
            .andExpect(jsonPath("$.result[0].gisuId").value(gisu.getId().toString()))
            .andExpect(jsonPath("$.result[0].chapterId").value(chapter.getId().toString()))
            .andExpect(jsonPath("$.result[1].id").value(second.getId().toString()))
            .andExpect(jsonPath("$.result[1].chapterId").value(chapter.getId().toString()));
    }

    @Test
    @DisplayName("chapterId와 time 기준으로 지원 가능한 매칭 차수를 조회한다")
    void chapterId와_time_기준으로_지원_가능한_매칭_차수를_조회한다() throws Exception {
        // given
        authenticate(memberFixture.일반("listperiod").getId());
        Gisu gisu = gisuDuring2026(10L);
        Chapter chapter = chapterFixture.지부(gisu, "owned");
        ProjectMatchingRound openRound = saveMatchingRound(
            "기획-디자인 1차 매칭",
            "지원 가능한 매칭 차수",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            chapter.getId(),
            "2026-05-10T00:00:00Z",
            "2026-05-12T00:00:00Z",
            "2026-05-13T00:00:00Z"
        );
        saveMatchingRound(
            "기획-디자인 2차 매칭",
            "아직 시작하지 않은 매칭 차수",
            MatchingType.PLAN_DESIGN,
            MatchingPhase.SECOND,
            chapter.getId(),
            "2026-05-20T00:00:00Z",
            "2026-05-22T00:00:00Z",
            "2026-05-23T00:00:00Z"
        );

        // when & then
        mockMvc.perform(get(BASE_URL)
                .param("chapterId", chapter.getId().toString())
                .param("time", "2026-05-11T00:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result", hasSize(1)))
            .andExpect(jsonPath("$.result[0].id").value(openRound.getId().toString()))
            .andExpect(jsonPath("$.result[0].chapterId").value(chapter.getId().toString()))
            .andExpect(jsonPath("$.result[0].startsAt").value("2026-05-10T00:00:00Z"))
            .andExpect(jsonPath("$.result[0].endsAt").value("2026-05-12T00:00:00Z"));
    }

    @Test
    @DisplayName("time만 전달해도 AND 필터로 매칭 차수 목록을 조회한다")
    void time만_전달해도_매칭_차수_목록을_조회한다() throws Exception {
        authenticate(memberFixture.일반("listtime").getId());

        // when & then
        mockMvc.perform(get(BASE_URL).param("time", "2026-05-11T00:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("gisuId와 chapterId와 time을 선택적 AND 조건으로 적용한다")
    void gisuId와_chapterId와_time을_AND_조건으로_적용한다() throws Exception {
        authenticate(memberFixture.일반("listfilters").getId());
        Gisu gisu = gisuDuring2026(10L);
        Chapter chapter = chapterFixture.지부(gisu, "owned");
        Gisu otherGisu = gisuDuring2026(11L);
        Chapter otherChapter = chapterFixture.지부(otherGisu, "other");
        ProjectMatchingRound matchingRound = saveMatchingRound(
            "기획-디자인 1차 매칭", "필터 대상", MatchingType.PLAN_DESIGN, MatchingPhase.FIRST,
            chapter.getId(), "2026-05-10T00:00:00Z", "2026-05-12T00:00:00Z", "2026-05-13T00:00:00Z"
        );
        saveMatchingRound(
            "다른 기수 매칭", "필터 제외", MatchingType.PLAN_DESIGN, MatchingPhase.FIRST,
            otherChapter.getId(), "2026-05-10T00:00:00Z", "2026-05-12T00:00:00Z", "2026-05-13T00:00:00Z"
        );

        mockMvc.perform(get(BASE_URL)
                .param("gisuId", gisu.getId().toString())
                .param("chapterId", chapter.getId().toString())
                .param("time", "2026-05-11T00:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result", hasSize(1)))
            .andExpect(jsonPath("$.result[0].id").value(matchingRound.getId().toString()))
            .andExpect(jsonPath("$.result[0].gisuId").value(gisu.getId().toString()))
            .andExpect(jsonPath("$.result[0].chapterId").value(chapter.getId().toString()));
    }

    private OperatorContext chapterPresidentContext() {
        Gisu gisu = gisuDuring2026(10L);
        Chapter chapter = chapterFixture.지부(gisu, "owned");
        Member member = memberFixture.일반("operator");
        Challenger challenger = challengerFixture.챌린저(member.getId(), ChallengerPart.PLAN, gisu.getId());
        challengerRoleFixture.지부장(challenger.getId(), chapter.getId(), gisu.getId());
        return new OperatorContext(member, gisu, chapter);
    }

    private Gisu gisuDuring2026(Long generation) {
        return gisuFixture.비활성_기수(
            generation,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z")
        );
    }

    private void authenticate(Long memberId) {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(memberId)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private ProjectMatchingRound saveMatchingRound(
        String name,
        String description,
        MatchingType type,
        MatchingPhase phase,
        Long chapterId,
        String startsAt,
        String endsAt,
        String decisionDeadline
    ) {
        return saveProjectMatchingRoundPort.save(ProjectMatchingRound.create(
            name,
            description,
            type,
            phase,
            gisuIdForChapter(chapterId),
            chapterId,
            Instant.parse(startsAt),
            Instant.parse(endsAt),
            Instant.parse(decisionDeadline)
        ));
    }

    private void saveProjectApplication(OperatorContext context, ProjectMatchingRound matchingRound) {
        Project project = saveProjectPort.save(Project.createDraft(
            context.gisu().getId(),
            context.chapter().getId(),
            context.member().getId(),
            1L,
            context.member().getId()
        ));
        ProjectApplicationForm applicationForm = saveProjectApplicationFormPort.save(
            ProjectApplicationForm.create(project, 10_000L)
        );
        ProjectApplication application = ProjectApplication.create(
            applicationForm,
            20_000L,
            context.member().getId(),
            matchingRound
        );
        projectApplicationJpaRepository.saveAndFlush(application);
    }

    private String matchingRoundRequest(
        String name,
        String description,
        MatchingType type,
        MatchingPhase phase,
        Long chapterId,
        String startsAt,
        String endsAt,
        String decisionDeadline
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "name", name,
            "description", description,
            "type", type.name(),
            "phase", phase.name(),
            "gisuId", gisuIdForChapter(chapterId),
            "chapterId", chapterId,
            "startsAt", startsAt,
            "endsAt", endsAt,
            "decisionDeadline", decisionDeadline
        ));
    }

    private String matchingRoundRequestWithoutGisu(
        String name,
        String description,
        MatchingType type,
        MatchingPhase phase,
        Long chapterId,
        String startsAt,
        String endsAt,
        String decisionDeadline
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "name", name,
            "description", description,
            "type", type.name(),
            "phase", phase.name(),
            "chapterId", chapterId,
            "startsAt", startsAt,
            "endsAt", endsAt,
            "decisionDeadline", decisionDeadline
        ));
    }

    private Long gisuIdForChapter(Long chapterId) {
        return entityManager.createQuery(
                "select c.gisu.id from Chapter c where c.id = :chapterId", Long.class)
            .setParameter("chapterId", chapterId)
            .getSingleResult();
    }

    private String requestBody(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private record OperatorContext(
        Member member,
        Gisu gisu,
        Chapter chapter
    ) {
    }
}
