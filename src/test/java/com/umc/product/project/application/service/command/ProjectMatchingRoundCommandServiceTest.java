package com.umc.product.project.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.port.in.command.dto.CreateProjectMatchingRoundCommand;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectMatchingRoundCommand;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.SaveProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.ScheduleMatchingRoundDeadlinePort;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

@ExtendWith(MockitoExtension.class)
class ProjectMatchingRoundCommandServiceTest {

    private static final Long ROUND_ID = 1L;
    private static final Long EXECUTOR_MEMBER_ID = 999L;

    @Mock
    LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;
    @Mock
    SaveProjectMatchingRoundPort saveProjectMatchingRoundPort;
    @Mock
    LoadProjectApplicationPort loadProjectApplicationPort;
    @Mock
    ScheduleMatchingRoundDeadlinePort scheduleMatchingRoundDeadlinePort;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @Mock
    ProjectPolicyAuthorizationService projectPolicyAuthorizationService;
    @Mock
    PolicyDecision allowedDecision;
    @Mock
    PolicyDecision deniedDecision;

    ProjectMatchingRoundCommandService sut;

    @Nested
    class gisuInvariant {

        @Test
        @DisplayName("기수 시작 정각에 시작하는 매칭 차수는 생성할 수 있다")
        void 기수_시작_정각에_시작하는_매칭_차수는_생성할_수_있다() {
            CreateProjectMatchingRoundCommand command = createCommand(
                1L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST,
                "2026-05-10T00:00:00Z", "2026-05-15T00:00:00Z", "2026-05-17T00:00:00Z"
            );
            given(getGisuUseCase.getById(5L)).willReturn(gisuInfo(
                "2026-05-10T00:00:00Z", "2026-05-20T00:00:00Z"));
            given(saveProjectMatchingRoundPort.save(any())).willReturn(futureRound(MatchingType.PLAN_DESIGN));

            sut.create(command);

            then(saveProjectMatchingRoundPort).should().save(any());
        }

        @Test
        @DisplayName("결정 마감이 기수 종료 정각이면 생성할 수 없다")
        void 결정_마감이_기수_종료_정각이면_생성할_수_없다() {
            CreateProjectMatchingRoundCommand command = createCommand(
                1L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST,
                "2026-05-10T00:00:00Z", "2026-05-15T00:00:00Z", "2026-05-17T00:00:00Z"
            );
            given(getGisuUseCase.getById(5L)).willReturn(gisuInfo(
                "2026-05-01T00:00:00Z", "2026-05-17T00:00:00Z"));

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_OUTSIDE_GISU_PERIOD);
            then(saveProjectMatchingRoundPort).should(never()).save(any());
        }

        @Test
        @DisplayName("지부가 기수에 속하지 않으면 저장 전에 거부한다")
        void 지부가_기수에_속하지_않으면_저장_전에_거부한다() {
            CreateProjectMatchingRoundCommand command = createCommand(99L);

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_GISU_CHAPTER_MISMATCH);
            then(saveProjectMatchingRoundPort).should(never()).save(any());
        }
    }

    @BeforeEach
    void setUpPolicyMock() {
        sut = sutWithMinPhaseIntervalMinutes(1);
        lenient().when(allowedDecision.effect()).thenReturn(PolicyEffect.ALLOW);
        lenient().when(deniedDecision.effect()).thenReturn(PolicyEffect.DENY);
        stubPolicyDecision(ProjectPolicyAction.MATCHING_CREATE, null, 5L, 1L, allowedDecision);
        stubPolicyDecision(ProjectPolicyAction.MATCHING_UPDATE, ROUND_ID, 5L, 1L, allowedDecision);
        stubPolicyDecision(ProjectPolicyAction.MATCHING_DELETE, ROUND_ID, 5L, 1L, allowedDecision);
        lenient().when(getChapterUseCase.belongsToGisu(1L, 5L)).thenReturn(true);
        lenient().when(getGisuUseCase.getById(5L)).thenReturn(new GisuInfo(
            5L,
            5L,
            Instant.parse("2020-01-01T00:00:00Z"),
            Instant.parse("2035-01-01T00:00:00Z"),
            false
        ));
    }

    @Nested
    class accessPolicy {

        @Test
        @DisplayName("다른 기수이거나 만료된 중앙 운영진으로 판정되어 생성 정책이 거부되면 저장하지 않는다")
        void 다른_기수이거나_만료된_중앙_운영진은_생성할_수_없다() {
            CreateProjectMatchingRoundCommand command = createCommand(1L);
            stubPolicyDecision(ProjectPolicyAction.MATCHING_CREATE, null, 5L, 1L, deniedDecision);

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_ACCESS_DENIED);
            then(saveProjectMatchingRoundPort).should(never()).save(any());
        }

        @Test
        @DisplayName("다른 지부의 지부장으로 판정되어 수정 정책이 거부되면 차수를 변경하지 않는다")
        void 다른_지부의_지부장은_수정할_수_없다() {
            ProjectMatchingRound existing = futureRound(MatchingType.PLAN_DESIGN);
            UpdateProjectMatchingRoundCommand command = updateCommand(ROUND_ID);
            given(loadProjectMatchingRoundPort.getById(ROUND_ID)).willReturn(existing);
            stubPolicyDecision(ProjectPolicyAction.MATCHING_UPDATE, ROUND_ID, 5L, 1L, deniedDecision);

            assertThatThrownBy(() -> sut.update(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_ACCESS_DENIED);
            then(scheduleMatchingRoundDeadlinePort).should(never()).schedule(any());
        }

        @Test
        @DisplayName("삭제 정책이 거부되면 연관 지원서를 조회하거나 차수를 삭제하지 않는다")
        void 삭제_정책이_거부되면_삭제하지_않는다() {
            ProjectMatchingRound existing = futureRound(MatchingType.PLAN_DESIGN);
            given(loadProjectMatchingRoundPort.getById(ROUND_ID)).willReturn(existing);
            stubPolicyDecision(ProjectPolicyAction.MATCHING_DELETE, ROUND_ID, 5L, 1L, deniedDecision);

            assertThatThrownBy(() -> sut.delete(ROUND_ID, EXECUTOR_MEMBER_ID))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_ACCESS_DENIED);
            then(loadProjectApplicationPort).should(never()).existsByAppliedMatchingRoundId(any());
            then(saveProjectMatchingRoundPort).should(never()).delete(any());
            then(scheduleMatchingRoundDeadlinePort).should(never()).cancel(any());
        }
    }

    @Nested
    class lifecycleHooks {

        @Test
        void create는_저장_후_scheduleMatchingRoundDeadlinePort에_schedule_호출한다() {
            CreateProjectMatchingRoundCommand command = createCommand(1L);
            ProjectMatchingRound saved = futureRound(MatchingType.PLAN_DESIGN);
            given(loadProjectMatchingRoundPort.listOverlapping(any(), any(), any())).willReturn(List.of());
            given(saveProjectMatchingRoundPort.save(any())).willReturn(saved);

            sut.create(command);

            then(scheduleMatchingRoundDeadlinePort).should().schedule(saved);
        }

        @Test
        void update는_도메인_갱신_후_scheduleMatchingRoundDeadlinePort에_schedule_호출한다() {
            ProjectMatchingRound existing = futureRound(MatchingType.PLAN_DESIGN);
            UpdateProjectMatchingRoundCommand command = updateCommand(ROUND_ID);
            given(loadProjectMatchingRoundPort.getById(ROUND_ID)).willReturn(existing);
            given(loadProjectMatchingRoundPort.listOverlappingExceptId(any(), any(), any(), any()))
                .willReturn(List.of());

            sut.update(command);

            then(scheduleMatchingRoundDeadlinePort).should().schedule(existing);
        }

        @Test
        void delete는_도메인_삭제_후_scheduleMatchingRoundDeadlinePort에_cancel_호출한다() {
            ProjectMatchingRound existing = futureRound(MatchingType.PLAN_DESIGN);
            given(loadProjectMatchingRoundPort.getById(ROUND_ID)).willReturn(existing);
            given(loadProjectApplicationPort.existsByAppliedMatchingRoundId(ROUND_ID)).willReturn(false);

            sut.delete(ROUND_ID, EXECUTOR_MEMBER_ID);

            then(scheduleMatchingRoundDeadlinePort).should().cancel(ROUND_ID);
        }

        @Test
        void delete는_연관_지원서가_있으면_예외_발생하고_cancel_호출되지_않는다() {
            ProjectMatchingRound existing = futureRound(MatchingType.PLAN_DESIGN);
            given(loadProjectMatchingRoundPort.getById(ROUND_ID)).willReturn(existing);
            given(loadProjectApplicationPort.existsByAppliedMatchingRoundId(ROUND_ID)).willReturn(true);

            assertThatThrownBy(() -> sut.delete(ROUND_ID, EXECUTOR_MEMBER_ID))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_DELETE_CONFLICT);
            then(scheduleMatchingRoundDeadlinePort).should(never()).cancel(any());
        }

        @Test
        @DisplayName("이전 차수 decisionDeadline 이후 다음 차수 startsAt까지 1분 미만이면 생성할 수 없다")
        void 이전_차수와_다음_차수_간격이_1분_미만이면_생성할_수_없다() {
            ProjectMatchingRound firstRound = matchingRound(
                10L,
                MatchingType.PLAN_DESIGN,
                MatchingPhase.FIRST,
                1L,
                "2026-05-10T00:00:00Z",
                "2026-05-12T00:00:00Z",
                "2026-05-13T00:00:00Z"
            );
            CreateProjectMatchingRoundCommand command = createCommand(
                1L,
                MatchingType.PLAN_DESIGN,
                MatchingPhase.SECOND,
                "2026-05-13T00:00:30Z",
                "2026-05-15T00:00:00Z",
                "2026-05-16T00:00:00Z"
            );
            given(loadProjectMatchingRoundPort.listOverlapping(any(), any(), any())).willReturn(List.of());
            given(loadProjectMatchingRoundPort.listByChapterId(1L)).willReturn(List.of(firstRound));

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting(e -> ((ProjectDomainException) e).getBaseCode().getCode())
                .isEqualTo("PROJECT-0309");
            then(saveProjectMatchingRoundPort).should(never()).save(any());
        }

        @Test
        @DisplayName("설정된 분 단위 간격보다 짧으면 생성할 수 없다")
        void 설정된_분_단위_간격보다_짧으면_생성할_수_없다() {
            sut = sutWithMinPhaseIntervalMinutes(2);
            ProjectMatchingRound firstRound = matchingRound(
                10L,
                MatchingType.PLAN_DESIGN,
                MatchingPhase.FIRST,
                1L,
                "2026-05-10T00:00:00Z",
                "2026-05-12T00:00:00Z",
                "2026-05-13T00:00:00Z"
            );
            CreateProjectMatchingRoundCommand command = createCommand(
                1L,
                MatchingType.PLAN_DESIGN,
                MatchingPhase.SECOND,
                "2026-05-13T00:01:30Z",
                "2026-05-15T00:00:00Z",
                "2026-05-16T00:00:00Z"
            );
            given(loadProjectMatchingRoundPort.listOverlapping(any(), any(), any())).willReturn(List.of());
            given(loadProjectMatchingRoundPort.listByChapterId(1L)).willReturn(List.of(firstRound));

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting(e -> ((ProjectDomainException) e).getBaseCode().getCode())
                .isEqualTo("PROJECT-0309");
            then(saveProjectMatchingRoundPort).should(never()).save(any());
        }

        @Test
        @DisplayName("같은 지부와 타입에 동일한 차수가 이미 있으면 생성할 수 없다")
        void 같은_지부와_타입에_동일한_차수가_이미_있으면_생성할_수_없다() {
            ProjectMatchingRound existingRound = matchingRound(
                10L,
                MatchingType.PLAN_DESIGN,
                MatchingPhase.FIRST,
                1L,
                "2026-05-10T00:00:00Z",
                "2026-05-12T00:00:00Z",
                "2026-05-13T00:00:00Z"
            );
            CreateProjectMatchingRoundCommand command = createCommand(
                1L,
                MatchingType.PLAN_DESIGN,
                MatchingPhase.FIRST,
                "2026-05-20T00:00:00Z",
                "2026-05-22T00:00:00Z",
                "2026-05-23T00:00:00Z"
            );
            given(loadProjectMatchingRoundPort.listOverlapping(any(), any(), any())).willReturn(List.of());
            given(loadProjectMatchingRoundPort.listByChapterId(1L)).willReturn(List.of(existingRound));

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting(e -> ((ProjectDomainException) e).getBaseCode().getCode())
                .isEqualTo("PROJECT-0309");
            then(saveProjectMatchingRoundPort).should(never()).save(any());
        }

        @Test
        @DisplayName("이전 차수 decisionDeadline 이후 다음 차수 startsAt까지 1분 미만이면 수정할 수 없다")
        void 이전_차수와_다음_차수_간격이_1분_미만이면_수정할_수_없다() {
            ProjectMatchingRound firstRound = matchingRound(
                10L,
                MatchingType.PLAN_DESIGN,
                MatchingPhase.FIRST,
                1L,
                "2026-05-10T00:00:00Z",
                "2026-05-12T00:00:00Z",
                "2026-05-13T00:00:00Z"
            );
            ProjectMatchingRound secondRound = matchingRound(
                ROUND_ID,
                MatchingType.PLAN_DESIGN,
                MatchingPhase.SECOND,
                1L,
                "2026-05-20T00:00:00Z",
                "2026-05-22T00:00:00Z",
                "2026-05-23T00:00:00Z"
            );
            UpdateProjectMatchingRoundCommand command = UpdateProjectMatchingRoundCommand.builder()
                .matchingRoundId(ROUND_ID)
                .requesterMemberId(EXECUTOR_MEMBER_ID)
                .startsAt(Instant.parse("2026-05-13T00:00:30Z"))
                .endsAt(Instant.parse("2026-05-15T00:00:00Z"))
                .decisionDeadline(Instant.parse("2026-05-16T00:00:00Z"))
                .build();
            given(loadProjectMatchingRoundPort.getById(ROUND_ID)).willReturn(secondRound);
            given(loadProjectMatchingRoundPort.listOverlappingExceptId(any(), any(), any(), any()))
                .willReturn(List.of());
            given(loadProjectMatchingRoundPort.listByChapterId(1L)).willReturn(List.of(firstRound, secondRound));

            assertThatThrownBy(() -> sut.update(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting(e -> ((ProjectDomainException) e).getBaseCode().getCode())
                .isEqualTo("PROJECT-0309");
            then(scheduleMatchingRoundDeadlinePort).should(never()).schedule(any());
        }

        @Test
        @DisplayName("같은 지부와 타입의 기존 차수와 동일한 차수로 수정할 수 없다")
        void 같은_지부와_타입의_기존_차수와_동일한_차수로_수정할_수_없다() {
            ProjectMatchingRound existingRound = matchingRound(
                10L,
                MatchingType.PLAN_DESIGN,
                MatchingPhase.FIRST,
                1L,
                "2026-05-10T00:00:00Z",
                "2026-05-12T00:00:00Z",
                "2026-05-13T00:00:00Z"
            );
            ProjectMatchingRound secondRound = matchingRound(
                ROUND_ID,
                MatchingType.PLAN_DESIGN,
                MatchingPhase.SECOND,
                1L,
                "2026-05-20T00:00:00Z",
                "2026-05-22T00:00:00Z",
                "2026-05-23T00:00:00Z"
            );
            UpdateProjectMatchingRoundCommand command = UpdateProjectMatchingRoundCommand.builder()
                .matchingRoundId(ROUND_ID)
                .requesterMemberId(EXECUTOR_MEMBER_ID)
                .phase(MatchingPhase.FIRST)
                .build();
            given(loadProjectMatchingRoundPort.getById(ROUND_ID)).willReturn(secondRound);
            given(loadProjectMatchingRoundPort.listOverlappingExceptId(any(), any(), any(), any()))
                .willReturn(List.of());
            given(loadProjectMatchingRoundPort.listByChapterId(1L)).willReturn(List.of(existingRound, secondRound));

            assertThatThrownBy(() -> sut.update(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting(e -> ((ProjectDomainException) e).getBaseCode().getCode())
                .isEqualTo("PROJECT-0309");
            then(scheduleMatchingRoundDeadlinePort).should(never()).schedule(any());
        }
    }

    private CreateProjectMatchingRoundCommand createCommand(Long chapterId) {
        Instant startsAt = Instant.now().plusSeconds(86_400);
        Instant endsAt = startsAt.plusSeconds(86_400);
        Instant decisionDeadline = endsAt.plusSeconds(86_400);
        return CreateProjectMatchingRoundCommand.builder()
            .name("기획-디자인 1차")
            .description(null)
            .type(MatchingType.PLAN_DESIGN)
            .phase(MatchingPhase.FIRST)
            .gisuId(5L)
            .chapterId(chapterId)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .decisionDeadline(decisionDeadline)
            .requesterMemberId(EXECUTOR_MEMBER_ID)
            .build();
    }

    private CreateProjectMatchingRoundCommand createCommand(
        Long chapterId,
        MatchingType type,
        MatchingPhase phase,
        String startsAt,
        String endsAt,
        String decisionDeadline
    ) {
        return CreateProjectMatchingRoundCommand.builder()
            .name("기획-디자인 2차")
            .description(null)
            .type(type)
            .phase(phase)
            .gisuId(5L)
            .chapterId(chapterId)
            .startsAt(Instant.parse(startsAt))
            .endsAt(Instant.parse(endsAt))
            .decisionDeadline(Instant.parse(decisionDeadline))
            .requesterMemberId(EXECUTOR_MEMBER_ID)
            .build();
    }

    private UpdateProjectMatchingRoundCommand updateCommand(Long roundId) {
        return UpdateProjectMatchingRoundCommand.builder()
            .matchingRoundId(roundId)
            .name("이름 수정")
            .description(null)
            .type(null)
            .phase(null)
            .startsAt(null)
            .endsAt(null)
            .decisionDeadline(null)
            .requesterMemberId(EXECUTOR_MEMBER_ID)
            .build();
    }

    private ProjectMatchingRound futureRound(MatchingType type) {
        ProjectMatchingRound round = ProjectMatchingRound.create(
            "테스트 매칭", null,
            type, MatchingPhase.FIRST, 5L, 1L,
            Instant.now().plusSeconds(86_400),
            Instant.now().plusSeconds(172_800),
            Instant.now().plusSeconds(259_200)
        );
        ReflectionTestUtils.setField(round, "id", ROUND_ID);
        return round;
    }

    private ProjectMatchingRound matchingRound(
        Long id,
        MatchingType type,
        MatchingPhase phase,
        Long chapterId,
        String startsAt,
        String endsAt,
        String decisionDeadline
    ) {
        ProjectMatchingRound round = ProjectMatchingRound.create(
            "테스트 매칭",
            null,
            type,
            phase,
            5L,
            chapterId,
            Instant.parse(startsAt),
            Instant.parse(endsAt),
            Instant.parse(decisionDeadline)
        );
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }

    private ProjectMatchingRoundCommandService sutWithMinPhaseIntervalMinutes(long minPhaseIntervalMinutes) {
        return new ProjectMatchingRoundCommandService(
            loadProjectMatchingRoundPort,
            saveProjectMatchingRoundPort,
            loadProjectApplicationPort,
            scheduleMatchingRoundDeadlinePort,
            getGisuUseCase,
            getChapterUseCase,
            new ProjectMatchingRoundProperties(minPhaseIntervalMinutes),
            projectPolicyAuthorizationService
        );
    }

    private void stubPolicyDecision(
        ProjectPolicyAction action,
        Long matchingRoundId,
        Long gisuId,
        Long chapterId,
        PolicyDecision decision
    ) {
        lenient().when(projectPolicyAuthorizationService.evaluate(
            eq(EXECUTOR_MEMBER_ID),
            eq(action),
            argThat(resource -> matchesMatchingRoundResource(
                resource, matchingRoundId, gisuId, chapterId))
        )).thenReturn(decision);
    }

    private boolean matchesMatchingRoundResource(
        ProjectPolicyResourceContext resource,
        Long matchingRoundId,
        Long gisuId,
        Long chapterId
    ) {
        return resource != null
            && resource.matchingRoundId().equals(Optional.ofNullable(matchingRoundId))
            && resource.gisuId().equals(Optional.of(gisuId))
            && resource.chapterId().equals(Optional.of(chapterId));
    }

    private GisuInfo gisuInfo(String startAt, String endAt) {
        return new GisuInfo(5L, 5L, Instant.parse(startAt), Instant.parse(endAt), false);
    }

}
