package com.umc.product.project.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
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
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    ProjectMatchingRoundCommandService sut;

    @BeforeEach
    void setUpRoleMock() {
        sut = sutWithMinPhaseIntervalMinutes(1);
        given(getChallengerRoleUseCase.findAllByMemberId(EXECUTOR_MEMBER_ID))
            .willReturn(List.of(centralCoreRole()));
    }

    @Nested
    class lifecycleHooks {

        @Test
        void system_role_super_admin은_challenger_role_없이_차수를_생성한다() {
            reset(getChallengerRoleUseCase);
            CreateProjectMatchingRoundCommand command = createCommand(1L);
            ProjectMatchingRound saved = futureRound(MatchingType.PLAN_DESIGN);
            given(getChallengerRoleUseCase.isSuperAdmin(EXECUTOR_MEMBER_ID)).willReturn(true);
            given(loadProjectMatchingRoundPort.listOverlapping(any(), any(), any())).willReturn(List.of());
            given(saveProjectMatchingRoundPort.save(any())).willReturn(saved);

            sut.create(command);

            then(getChallengerRoleUseCase).should(never()).findAllByMemberId(EXECUTOR_MEMBER_ID);
            then(saveProjectMatchingRoundPort).should().save(any());
        }

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

        @Test
        @DisplayName("수정 기간이 다른 차수와 겹치면 거부한다")
        void 수정_기간_중첩_거부() {
            ProjectMatchingRound current = futureRound(MatchingType.PLAN_DESIGN);
            given(loadProjectMatchingRoundPort.getById(ROUND_ID)).willReturn(current);
            given(loadProjectMatchingRoundPort.listOverlappingExceptId(any(), any(), any(), any()))
                .willReturn(List.of(matchingRound(
                    2L, MatchingType.PLAN_DESIGN, MatchingPhase.SECOND, 1L,
                    "2026-05-20T00:00:00Z", "2026-05-21T00:00:00Z", "2026-05-22T00:00:00Z")));

            assertThatThrownBy(() -> sut.update(updateCommand(ROUND_ID)))
                .isInstanceOf(ProjectDomainException.class)
                .extracting(e -> ((ProjectDomainException) e).getBaseCode())
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_PERIOD_OVERLAPPED);
        }

        @Test
        @DisplayName("다음 차수 시작 직전까지 이전 차수 결정 기한이 이어지면 생성할 수 없다")
        void 다음_차수와_이전_차수_결정기한_간격_부족() {
            ProjectMatchingRound thirdRound = matchingRound(
                10L, MatchingType.PLAN_DESIGN, MatchingPhase.THIRD, 1L,
                "2026-05-20T00:00:00Z", "2026-05-22T00:00:00Z", "2026-05-23T00:00:00Z");
            CreateProjectMatchingRoundCommand command = createCommand(
                1L, MatchingType.PLAN_DESIGN, MatchingPhase.SECOND,
                "2026-05-15T00:00:00Z", "2026-05-17T00:00:00Z", "2026-05-19T23:59:30Z");
            given(loadProjectMatchingRoundPort.listOverlapping(any(), any(), any())).willReturn(List.of());
            given(loadProjectMatchingRoundPort.listByChapterId(1L)).willReturn(List.of(thirdRound));

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting(e -> ((ProjectDomainException) e).getBaseCode())
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_PHASE_SEQUENCE_INVALID);
        }

        @Test
        @DisplayName("차수 사이 최소 간격을 만족하면 다음 차수를 생성한다")
        void 차수_최소_간격_충족() {
            ProjectMatchingRound firstRound = matchingRound(
                10L, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, 1L,
                "2026-05-10T00:00:00Z", "2026-05-12T00:00:00Z", "2026-05-13T00:00:00Z");
            CreateProjectMatchingRoundCommand command = createCommand(
                1L, MatchingType.PLAN_DESIGN, MatchingPhase.SECOND,
                "2026-05-13T00:01:00Z", "2026-05-15T00:00:00Z", "2026-05-16T00:00:00Z");
            ProjectMatchingRound saved = matchingRound(
                20L, MatchingType.PLAN_DESIGN, MatchingPhase.SECOND, 1L,
                "2026-05-13T00:01:00Z", "2026-05-15T00:00:00Z", "2026-05-16T00:00:00Z");
            given(loadProjectMatchingRoundPort.listOverlapping(any(), any(), any())).willReturn(List.of());
            given(loadProjectMatchingRoundPort.listByChapterId(1L)).willReturn(List.of(firstRound));
            given(saveProjectMatchingRoundPort.save(any())).willReturn(saved);

            sut.create(command);

            then(saveProjectMatchingRoundPort).should().save(any());
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
            type, MatchingPhase.FIRST, 1L,
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
            getChallengerRoleUseCase,
            new ProjectMatchingRoundProperties(minPhaseIntervalMinutes)
        );
    }

    private ChallengerRoleInfo centralCoreRole() {
        return ChallengerRoleInfo.builder()
            .roleType(ChallengerRoleType.CENTRAL_PRESIDENT)
            .organizationType(OrganizationType.CENTRAL)
            .organizationId(null)
            .gisuId(5L)
            .build();
    }
}
