package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.DecideApplicationUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectApplicationDraftUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo.QuestionInfo;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo.SectionInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectMatchingRoundInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPartQuotaInfo;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsCommand;
import com.umc.product.test.application.port.in.command.dto.SeedProjectApplicationsResult;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationSeedServiceTest {

    @Mock GetGisuUseCase getGisuUseCase;
    @Mock GetChallengerUseCase getChallengerUseCase;
    @Mock GetProjectMatchingRoundUseCase getProjectMatchingRoundUseCase;
    @Mock GetProjectApplicationFormUseCase getProjectApplicationFormUseCase;
    @Mock SearchProjectUseCase searchProjectUseCase;
    @Mock CreateDraftProjectApplicationUseCase createDraftUseCase;
    @Mock UpdateProjectApplicationDraftUseCase updateDraftUseCase;
    @Mock SubmitProjectApplicationUseCase submitUseCase;
    @Mock DecideApplicationUseCase decideUseCase;

    ProjectApplicationSeedService sut;

    @BeforeEach
    void setUp() {
        sut = spy(new ProjectApplicationSeedService(
            getGisuUseCase,
            getChallengerUseCase,
            getProjectMatchingRoundUseCase,
            getProjectApplicationFormUseCase,
            searchProjectUseCase,
            createDraftUseCase,
            updateDraftUseCase,
            submitUseCase,
            decideUseCase
        ));
    }

    @Test
    @DisplayName("매칭 차수가 지부에 속하지 않으면 시딩을 거절한다")
    void 매칭_차수_소속_검증() {
        given(getProjectMatchingRoundUseCase.list(20L, null)).willReturn(List.of(openRound(99L)));

        assertThatThrownBy(() -> sut.seed(command()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chapterId=20");
    }

    @Test
    @DisplayName("시작 전이거나 종료된 매칭 차수는 시딩을 거절한다")
    void 매칭_차수_OPEN_기간_검증() {
        Instant now = Instant.now();
        ProjectMatchingRoundInfo future = round(10L, MatchingType.PLAN_DEVELOPER,
            now.plusSeconds(60), now.plusSeconds(120));
        ProjectMatchingRoundInfo ended = round(10L, MatchingType.PLAN_DEVELOPER,
            now.minusSeconds(120), now.minusSeconds(60));
        given(getProjectMatchingRoundUseCase.list(20L, null))
            .willReturn(List.of(future), List.of(ended));

        assertThatThrownBy(() -> sut.seed(command()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("OPEN 상태가 아닙니다");
        assertThatThrownBy(() -> sut.seed(command()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("OPEN 상태가 아닙니다");
    }

    @Test
    @DisplayName("매칭 타입에 맞는 ACTIVE 챌린저가 없으면 빈 결과를 반환한다")
    void 대상_챌린저가_없으면_스킵() {
        givenOpenRound(MatchingType.PLAN_DESIGN);
        given(getGisuUseCase.getActiveGisuId()).willReturn(9L);
        given(getChallengerUseCase.getAllByGisuId(9L)).willReturn(List.of(
            challenger(1L, ChallengerPart.WEB, ChallengerStatus.ACTIVE),
            challenger(2L, ChallengerPart.DESIGN, ChallengerStatus.GRADUATED)
        ));

        SeedProjectApplicationsResult result = sut.seed(command());

        assertThat(result.counts().submittedTotal()).isZero();
        assertThat(result.createdApplications()).isEmpty();
        verify(searchProjectUseCase, never()).search(any(), anyLong());
    }

    @Test
    @DisplayName("지원 가능한 프로젝트가 없으면 빈 결과를 반환한다")
    void 대상_프로젝트가_없으면_스킵() {
        givenEligibleChallengers(challenger(1L, ChallengerPart.WEB, ChallengerStatus.ACTIVE));
        given(searchProjectUseCase.search(any(), anyLong())).willReturn(new PageImpl<>(List.of()));

        SeedProjectApplicationsResult result = sut.seed(command());

        assertThat(result.counts().submittedTotal()).isZero();
        verify(createDraftUseCase, never()).create(any());
    }

    @Test
    @DisplayName("지원 단계별 성공과 실패를 격리하고 결과 상태별 집계를 반환한다")
    void 단계별_성공과_실패를_격리한다() {
        ChallengerInfo noProject = challenger(4L, ChallengerPart.IOS, ChallengerStatus.ACTIVE);
        givenEligibleChallengers(
            challenger(1L, ChallengerPart.WEB, ChallengerStatus.ACTIVE),
            challenger(2L, ChallengerPart.WEB, ChallengerStatus.ACTIVE),
            challenger(3L, ChallengerPart.WEB, ChallengerStatus.ACTIVE),
            noProject,
            challenger(5L, ChallengerPart.WEB, ChallengerStatus.ACTIVE),
            challenger(6L, ChallengerPart.WEB, ChallengerStatus.ACTIVE),
            challenger(7L, ChallengerPart.WEB, ChallengerStatus.ACTIVE),
            challenger(8L, ChallengerPart.WEB, ChallengerStatus.ACTIVE)
        );
        given(searchProjectUseCase.search(any(), anyLong()))
            .willReturn(new PageImpl<>(List.of(project(100L, 999L, ChallengerPart.WEB))));
        given(createDraftUseCase.create(any())).willAnswer(invocation -> {
            long memberId = invocation.getArgument(
                0, com.umc.product.project.application.port.in.command.dto.CreateDraftProjectApplicationCommand.class
            ).applicantMemberId();
            if (memberId == 5L) {
                throw new IllegalStateException("draft failure");
            }
            return ProjectApplicationInfo.of(1000L + memberId, ProjectApplicationStatus.DRAFT);
        });

        QuestionInfo required = QuestionInfo.builder().questionId(11L).isRequired(true).build();
        QuestionInfo optional = QuestionInfo.builder().questionId(12L).isRequired(false).build();
        ApplicationFormInfo form = ApplicationFormInfo.builder()
            .sections(List.of(SectionInfo.builder().questions(List.of(required, optional)).build()))
            .build();
        given(getProjectApplicationFormUseCase.findByProjectId(anyLong(), anyLong()))
            .willReturn(Optional.of(form));
        given(updateDraftUseCase.update(any())).willAnswer(invocation -> {
            var update = invocation.getArgument(
                0, com.umc.product.project.application.port.in.command.dto.UpdateProjectApplicationDraftCommand.class
            );
            if (update.requesterMemberId() == 6L) {
                throw new IllegalStateException("fill failure");
            }
            return ProjectApplicationInfo.of(update.applicationId(), ProjectApplicationStatus.DRAFT);
        });
        given(submitUseCase.submit(any())).willAnswer(invocation -> {
            var submit = invocation.getArgument(
                0, com.umc.product.project.application.port.in.command.dto.SubmitProjectApplicationCommand.class
            );
            if (submit.requesterMemberId() == 7L) {
                throw new IllegalStateException("submit failure");
            }
            return ProjectApplicationInfo.of(submit.applicationId(), ProjectApplicationStatus.SUBMITTED);
        });
        given(decideUseCase.decide(anyLong(), any(), any(), anyLong())).willAnswer(invocation -> {
            Long applicationId = invocation.getArgument(0);
            if (applicationId == 1008L) {
                throw new IllegalStateException("decide failure");
            }
            return ProjectApplicationInfo.of(applicationId, ProjectApplicationStatus.APPROVED);
        });
        doReturn(0, 1, 2, 1).when(sut).nextDecisionIndex();

        SeedProjectApplicationsResult result = sut.seed(command());

        assertThat(result.counts().submittedTotal()).isOne();
        assertThat(result.counts().approvedTotal()).isOne();
        assertThat(result.counts().rejectedTotal()).isOne();
        assertThat(result.counts().failedTotal()).isEqualTo(5);
        assertThat(result.createdApplications()).singleElement()
            .satisfies(applications -> assertThat(applications.applications()).hasSize(3));
        assertThat(result.failedApplications())
            .extracting(SeedProjectApplicationsResult.FailedApplication::failedStep)
            .containsExactly("NO_PROJECT", "DRAFT", "FILL", "SUBMIT", "DECIDE");
    }

    @Test
    @DisplayName("지원 폼이 없으면 답변 갱신을 생략하고 제출한다")
    void 지원_폼이_없으면_답변_갱신을_생략한다() {
        givenEligibleChallengers(challenger(1L, ChallengerPart.WEB, ChallengerStatus.ACTIVE));
        given(searchProjectUseCase.search(any(), anyLong()))
            .willReturn(new PageImpl<>(List.of(project(100L, 999L, ChallengerPart.WEB))));
        given(createDraftUseCase.create(any()))
            .willReturn(ProjectApplicationInfo.of(1001L, ProjectApplicationStatus.DRAFT));
        given(getProjectApplicationFormUseCase.findByProjectId(100L, 1L))
            .willReturn(Optional.empty());
        doReturn(0).when(sut).nextDecisionIndex();

        SeedProjectApplicationsResult result = sut.seed(command());

        assertThat(result.counts().submittedTotal()).isOne();
        verify(updateDraftUseCase, never()).update(any());
        verify(submitUseCase).submit(any());
    }

    @Test
    @DisplayName("프로젝트 PO 본인은 지원 후보에서 제외한다")
    void 프로젝트_PO_지원_제외() {
        givenEligibleChallengers(challenger(999L, ChallengerPart.WEB, ChallengerStatus.ACTIVE));
        given(searchProjectUseCase.search(any(), anyLong()))
            .willReturn(new PageImpl<>(List.of(project(100L, 999L, ChallengerPart.WEB))));

        SeedProjectApplicationsResult result = sut.seed(command());

        assertThat(result.failedApplications()).singleElement()
            .satisfies(failure -> assertThat(failure.failedStep()).isEqualTo("NO_PROJECT"));
        verify(createDraftUseCase, never()).create(any());
    }

    @Test
    @DisplayName("무작위 지원 결과 인덱스는 세 상태 범위 안에서 생성된다")
    void 무작위_결과_범위() {
        assertThat(sut.nextDecisionIndex()).isBetween(0, 2);
    }

    private void givenOpenRound(MatchingType type) {
        given(getProjectMatchingRoundUseCase.list(20L, null)).willReturn(List.of(
            round(10L, type, Instant.now().minusSeconds(60), Instant.now().plusSeconds(60))
        ));
    }

    private void givenEligibleChallengers(ChallengerInfo... challengers) {
        givenOpenRound(MatchingType.PLAN_DEVELOPER);
        given(getGisuUseCase.getActiveGisuId()).willReturn(9L);
        given(getChallengerUseCase.getAllByGisuId(9L)).willReturn(List.of(challengers));
    }

    private SeedProjectApplicationsCommand command() {
        return new SeedProjectApplicationsCommand(10L, 20L);
    }

    private ProjectMatchingRoundInfo openRound(Long id) {
        return round(id, MatchingType.PLAN_DEVELOPER,
            Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));
    }

    private ProjectMatchingRoundInfo round(
        Long id, MatchingType type, Instant startsAt, Instant endsAt
    ) {
        return ProjectMatchingRoundInfo.builder()
            .id(id)
            .type(type)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .build();
    }

    private ChallengerInfo challenger(
        Long memberId, ChallengerPart part, ChallengerStatus status
    ) {
        return ChallengerInfo.builder()
            .memberId(memberId)
            .part(part)
            .challengerStatus(status)
            .build();
    }

    private ProjectInfo project(Long id, Long ownerId, ChallengerPart part) {
        return ProjectInfo.builder()
            .id(id)
            .status(ProjectStatus.IN_PROGRESS)
            .productOwnerMemberId(ownerId)
            .partQuotas(List.of(ProjectPartQuotaInfo.of(part, 1, 0)))
            .build();
    }
}
