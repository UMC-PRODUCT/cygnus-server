package com.umc.product.project.application.authorization;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.SearchChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.project.application.authorization.ProjectDirectActionBindings.Binding;
import com.umc.product.project.application.authorization.ProjectDirectActionBindings.Caller;
import com.umc.product.project.application.port.in.command.AutoDecisionActor;
import com.umc.product.project.application.port.in.command.dto.CreateDraftProjectCommand;
import com.umc.product.project.application.port.in.command.dto.CreateProjectMatchingRoundCommand;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectMatchingRoundCommand;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPartQuotaPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationPort;
import com.umc.product.project.application.port.out.SaveProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.SaveProjectMemberPort;
import com.umc.product.project.application.port.out.SaveProjectPartQuotaPort;
import com.umc.product.project.application.port.out.SaveProjectPort;
import com.umc.product.project.application.port.out.ScheduleMatchingRoundDeadlinePort;
import com.umc.product.project.application.service.command.ProjectCommandService;
import com.umc.product.project.application.service.command.ProjectMatchingRoundCommandService;
import com.umc.product.project.application.service.command.ProjectMatchingRoundFinalizationCommandService;
import com.umc.product.project.application.service.command.ProjectMatchingRoundProperties;
import com.umc.product.project.application.service.policy.MatchingDecisionPolicy;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;

class ProjectDirectCommandActionProductionPathContractTest
    extends ProjectDirectActionProductionPathContractSupport {

    private static final Instant STARTS_AT = Instant.parse("2026-07-10T00:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-07-20T00:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-07-25T00:00:00Z");

    private final LoadProjectMatchingRoundPort loadRoundPort = mock(LoadProjectMatchingRoundPort.class);
    private final GetGisuUseCase getGisuUseCase = mock(GetGisuUseCase.class);
    private final GetChapterUseCase getChapterUseCase = mock(GetChapterUseCase.class);
    private final GetChallengerUseCase getChallengerUseCase = mock(GetChallengerUseCase.class);
    private final GetMemberUseCase getMemberUseCase = mock(GetMemberUseCase.class);

    private final ProjectCommandService projectCommandService = projectCommandService();
    private final ProjectMatchingRoundCommandService matchingCommandService = matchingCommandService();
    private final ProjectMatchingRoundFinalizationCommandService finalizationService = finalizationService();

    private ProjectMatchingRound round;

    @BeforeEach
    void setUpResources() {
        round = ProjectMatchingRound.create(
            "1차 매칭", null, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST,
            GISU_ID, CHAPTER_ID, STARTS_AT, ENDS_AT, DEADLINE);
        ReflectionTestUtils.setField(round, "id", ROUND_ID);
        given(loadRoundPort.getById(ROUND_ID)).willReturn(round);
        given(loadRoundPort.getByIdForUpdate(ROUND_ID)).willReturn(round);
        given(getChapterUseCase.belongsToGisu(CHAPTER_ID, GISU_ID)).willReturn(true);
        given(getGisuUseCase.getById(GISU_ID)).willReturn(new GisuInfo(
            GISU_ID,
            10L,
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-08-01T00:00:00Z"),
            true
        ));
        given(getChallengerUseCase.getByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(
            ChallengerInfo.builder()
                .challengerId(1L)
                .memberId(MEMBER_ID)
                .gisuId(GISU_ID)
                .part(ChallengerPart.PLAN)
                .build());
        given(getMemberUseCase.getById(MEMBER_ID)).willReturn(MemberInfo.builder()
            .id(MEMBER_ID)
            .schoolId(100L)
            .build());
        given(getChapterUseCase.byGisuAndSchool(GISU_ID, 100L)).willReturn(
            new ChapterInfo(CHAPTER_ID, "중앙"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("commandBindings")
    @DisplayName("command의 direct action은 실제 service에서 exact policy action으로 평가된다")
    void direct_command_action이_production_path에서_exact_action으로_평가된다(Binding binding) {
        assertExactPolicyAction(binding.action(), () -> invoke(binding.action()));
    }

    private void invoke(ProjectPolicyAction action) {
        switch (action) {
            case PROJECT_CREATE -> projectCommandService.create(CreateDraftProjectCommand.builder()
                .gisuId(GISU_ID).productOwnerMemberId(MEMBER_ID).requesterMemberId(MEMBER_ID).build());
            case MATCHING_CREATE -> matchingCommandService.create(createRoundCommand());
            case MATCHING_UPDATE -> matchingCommandService.update(UpdateProjectMatchingRoundCommand.builder()
                .matchingRoundId(ROUND_ID).requesterMemberId(MEMBER_ID).build());
            case MATCHING_DELETE -> matchingCommandService.delete(ROUND_ID, MEMBER_ID);
            case MATCHING_HUMAN_AUTO_DECIDE -> finalizationService.autoDecide(
                ROUND_ID, new AutoDecisionActor.Member(MEMBER_ID));
            case MATCHING_SYSTEM_AUTO_DECIDE -> finalizationService.autoDecide(
                ROUND_ID, AutoDecisionActor.matchingRoundScheduler());
            default -> throw new IllegalArgumentException("command direct action이 아닙니다: " + action);
        }
    }

    private ProjectCommandService projectCommandService() {
        return new ProjectCommandService(
            mock(LoadProjectPort.class), mock(SaveProjectPort.class), mock(LoadProjectApplicationFormPort.class),
            mock(LoadProjectPartQuotaPort.class), mock(LoadProjectMemberPort.class),
            mock(LoadProjectApplicationPort.class), mock(SaveProjectMemberPort.class),
            mock(SaveProjectPartQuotaPort.class), mock(SaveProjectApplicationFormPort.class),
            mock(SaveProjectApplicationFormPolicyPort.class), getMemberUseCase, getChallengerUseCase,
            getGisuUseCase, getChapterUseCase, mock(ManageFormUseCase.class), policyService
        );
    }

    private ProjectMatchingRoundCommandService matchingCommandService() {
        return new ProjectMatchingRoundCommandService(
            loadRoundPort,
            mock(SaveProjectMatchingRoundPort.class),
            mock(LoadProjectApplicationPort.class),
            mock(ScheduleMatchingRoundDeadlinePort.class),
            getGisuUseCase,
            getChapterUseCase,
            new ProjectMatchingRoundProperties(1L),
            policyService
        );
    }

    private ProjectMatchingRoundFinalizationCommandService finalizationService() {
        return new ProjectMatchingRoundFinalizationCommandService(
            loadRoundPort,
            mock(LoadProjectApplicationPort.class),
            mock(SaveProjectApplicationPort.class),
            mock(LoadProjectPort.class),
            mock(LoadProjectPartQuotaPort.class),
            mock(LoadProjectMemberPort.class),
            mock(SaveProjectMemberPort.class),
            List.<MatchingDecisionPolicy>of(),
            new Random(42L),
            getChallengerUseCase,
            mock(SearchChallengerUseCase.class),
            policyService
        );
    }

    private CreateProjectMatchingRoundCommand createRoundCommand() {
        return CreateProjectMatchingRoundCommand.builder()
            .requesterMemberId(MEMBER_ID)
            .name("1차 매칭")
            .type(MatchingType.PLAN_DESIGN)
            .phase(MatchingPhase.FIRST)
            .gisuId(GISU_ID)
            .chapterId(CHAPTER_ID)
            .startsAt(STARTS_AT)
            .endsAt(ENDS_AT)
            .decisionDeadline(DEADLINE)
            .build();
    }

    private static Stream<Binding> commandBindings() {
        return ProjectDirectActionBindings.valuesFor(
            Caller.PROJECT_COMMAND,
            Caller.MATCHING_COMMAND,
            Caller.MATCHING_FINALIZATION
        ).stream();
    }
}
