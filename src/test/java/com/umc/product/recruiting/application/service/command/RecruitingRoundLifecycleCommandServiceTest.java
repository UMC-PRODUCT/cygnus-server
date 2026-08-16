package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpsertRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloneRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RestoreRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingRoundLifecycleCommandServiceTest {

    @Mock LoadRecruitingRoundPort loadRoundPort;
    @Mock SaveRecruitingRoundPort saveRoundPort;
    @Mock LoadRecruitingApplicationPort loadApplicationPort;
    @Mock LoadRecruitingApplicationFormPort loadApplicationFormPort;
    @Mock LoadRecruitingFormSectionPolicyPort loadPolicyPort;
    @Mock LoadRecruitingRoundInterviewQuestionPort loadQuestionPort;
    @Mock SaveRecruitingRoundInterviewQuestionPort saveQuestionPort;
    @Mock GetFormUseCase getFormUseCase;
    @Mock GetFormResponseUseCase getFormResponseUseCase;
    @Mock CreateRecruitingRoundUseCase createRoundUseCase;
    @Mock UpsertRecruitingApplicationFormUseCase upsertFormUseCase;
    @Mock AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    static final Instant NOW = Instant.parse("2026-08-16T00:00:00Z");

    RecruitingRoundLifecycleCommandService sut;
    RecruitingRound source;

    @BeforeEach
    void setUp() {
        sut = new RecruitingRoundLifecycleCommandService(
            loadRoundPort,
            saveRoundPort,
            loadApplicationPort,
            loadApplicationFormPort,
            loadPolicyPort,
            loadQuestionPort,
            saveQuestionPort,
            getFormUseCase,
            getFormResponseUseCase,
            createRoundUseCase,
            upsertFormUseCase,
            authorizeManagementUseCase,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
        source = round(10L, 20L, true, 999L, 1999L);
    }

    @Test
    @DisplayName("응답이 존재하는 DRAFT Round는 삭제되지 않는다")
    void rejectDeleteWhenFormResponseExists() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(source, 100L);
        given(loadRoundPort.getByIdForUpdate(20L)).willReturn(source);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(form));
        given(getFormResponseUseCase.listByFormId(100L)).willReturn(List.of(
            FormResponseInfo.builder().id(1L).formId(100L).build()
        ));

        assertThatThrownBy(() -> sut.deleteRound(DeleteRecruitingRoundCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .requesterMemberId(99L)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_DELETE_CONFLICT);

        then(saveRoundPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서와 응답이 없는 DRAFT Round는 삭제 시각만 기록하고 하위 데이터는 남긴다")
    void softDeleteDraftRound() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(source, 100L);
        ReflectionTestUtils.setField(form, "id", 200L);
        given(loadRoundPort.getByIdForUpdate(20L)).willReturn(source);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(form));
        given(getFormResponseUseCase.listByFormId(100L)).willReturn(List.of());

        sut.deleteRound(DeleteRecruitingRoundCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .requesterMemberId(99L)
            .build());

        assertThat(source.isDeleted()).isTrue();
        assertThat(source.getDeletedAt()).isEqualTo(NOW);
        then(saveRoundPort).should().save(source);
    }

    @Test
    @DisplayName("삭제된 Round 복구는 삭제 시각을 비우고 저장한다")
    void restoreDeletedRound() {
        source.delete(NOW);
        given(loadRoundPort.getByIdForUpdateIncludingDeleted(20L)).willReturn(source);
        given(loadRoundPort.existsBySeasonIdAndTypeAndRoundNo(10L, source.getType(), source.getRoundNo()))
            .willReturn(false);
        given(loadRoundPort.existsBySeasonIdAndTitleIgnoreCase(10L, source.getTitle())).willReturn(false);

        sut.restoreRound(restoreCommand());

        assertThat(source.isDeleted()).isFalse();
        assertThat(source.getDeletedAt()).isNull();
        then(saveRoundPort).should().save(source);
    }

    @Test
    @DisplayName("삭제 상태가 아닌 Round는 복구할 수 없다")
    void rejectRestoreWhenRoundIsNotDeleted() {
        given(loadRoundPort.getByIdForUpdateIncludingDeleted(20L)).willReturn(source);

        assertThatThrownBy(() -> sut.restoreRound(restoreCommand()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_NOT_DELETED);

        then(saveRoundPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("삭제된 사이에 같은 차수 번호가 선점되면 복구를 거절한다")
    void rejectRestoreWhenRoundNoIsTaken() {
        source.delete(NOW);
        given(loadRoundPort.getByIdForUpdateIncludingDeleted(20L)).willReturn(source);
        given(loadRoundPort.existsBySeasonIdAndTypeAndRoundNo(10L, source.getType(), source.getRoundNo()))
            .willReturn(true);

        assertThatThrownBy(() -> sut.restoreRound(restoreCommand()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_RESTORE_CONFLICT);

        assertThat(source.isDeleted()).isTrue();
        then(saveRoundPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("삭제된 사이에 같은 제목이 선점되면 복구를 거절한다")
    void rejectRestoreWhenTitleIsTaken() {
        source.delete(NOW);
        given(loadRoundPort.getByIdForUpdateIncludingDeleted(20L)).willReturn(source);
        given(loadRoundPort.existsBySeasonIdAndTypeAndRoundNo(10L, source.getType(), source.getRoundNo()))
            .willReturn(false);
        given(loadRoundPort.existsBySeasonIdAndTitleIgnoreCase(10L, source.getTitle())).willReturn(true);

        assertThatThrownBy(() -> sut.restoreRound(restoreCommand()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_RESTORE_CONFLICT);

        then(saveRoundPort).shouldHaveNoInteractions();
    }

    private RestoreRecruitingRoundCommand restoreCommand() {
        return RestoreRecruitingRoundCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .requesterMemberId(99L)
            .build();
    }

    @Test
    @DisplayName("Round 복제는 원본과 대상 Season 권한을 검증하고 availability 매핑을 초기화한다")
    void cloneRoundAuthorizesBothSeasonsAndClearsAvailabilityForm() {
        RecruitingRound cloned = round(30L, 40L, true, null, null);
        given(loadRoundPort.getById(20L)).willReturn(source);
        given(createRoundUseCase.createRound(any())).willReturn(40L);
        given(loadRoundPort.getById(40L)).willReturn(cloned);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.empty());
        given(loadQuestionPort.listActiveByRoundId(20L)).willReturn(List.of());

        Long result = sut.cloneRound(CloneRecruitingRoundCommand.builder()
            .sourceSeasonId(10L)
            .sourceRoundId(20L)
            .targetSeasonId(30L)
            .title("15기 추가모집")
            .type(RecruitingRoundType.ADDITIONAL)
            .requesterMemberId(99L)
            .build());

        assertThat(result).isEqualTo(40L);
        then(authorizeManagementUseCase).should().authorizeSeasonManagement(99L, 10L);
        then(authorizeManagementUseCase).should().authorizeSeasonManagement(99L, 30L);
        ArgumentCaptor<CreateRecruitingRoundCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingRoundCommand.class);
        then(createRoundUseCase).should().createRound(captor.capture());
        assertThat(captor.getValue().configuration().availabilityFormId()).isNull();
        assertThat(captor.getValue().configuration().availabilityScheduleQuestionId()).isNull();
        assertThat(captor.getValue().configuration().announcement()).isEqualTo("공고");
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("Round 복제는 Form 구조와 조건부 이동을 새 client key로 재매핑한다")
    void cloneRoundRemapsFormStructure() {
        RecruitingRound cloned = round(30L, 40L, false, null, null);
        RecruitingApplicationForm sourceForm = RecruitingApplicationForm.create(source, 100L);
        ReflectionTestUtils.setField(sourceForm, "id", 200L);
        given(loadRoundPort.getById(20L)).willReturn(source);
        given(createRoundUseCase.createRound(any())).willReturn(40L);
        given(loadRoundPort.getById(40L)).willReturn(cloned);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(sourceForm));
        given(getFormUseCase.getFormWithStructure(100L)).willReturn(FormWithStructureInfo.builder()
            .formId(100L)
            .description("지원서")
            .sections(List.of(
                FormWithStructureInfo.SectionWithQuestions.builder()
                    .sectionId(1L)
                    .title("공통")
                    .questions(List.of(FormWithStructureInfo.QuestionWithOptions.builder()
                        .questionId(11L)
                        .title("분기")
                        .type(QuestionType.RADIO)
                        .options(List.of(FormWithStructureInfo.Option.builder()
                            .optionId(111L)
                            .content("계속")
                            .nextSectionId(2L)
                            .build()))
                        .build()))
                    .build(),
                FormWithStructureInfo.SectionWithQuestions.builder()
                    .sectionId(2L)
                    .title("기획")
                    .questions(List.of())
                    .build()
            ))
            .build());
        given(loadPolicyPort.listByApplicationFormId(200L)).willReturn(List.of(
            RecruitingFormSectionPolicy.createCommon(sourceForm, 1L),
            RecruitingFormSectionPolicy.createTrack(sourceForm, 2L, ChallengerTrack.PLAN)
        ));
        given(loadQuestionPort.listActiveByRoundId(20L)).willReturn(List.of());

        sut.cloneRound(CloneRecruitingRoundCommand.builder()
            .sourceSeasonId(10L)
            .sourceRoundId(20L)
            .targetSeasonId(30L)
            .title("복제 모집")
            .type(RecruitingRoundType.REGULAR)
            .requesterMemberId(99L)
            .build());

        ArgumentCaptor<com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand>
            captor = ArgumentCaptor.forClass(
                com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand.class
            );
        then(upsertFormUseCase).should().upsert(captor.capture());
        var sections = captor.getValue().sections();
        assertThat(sections).extracting(section -> section.sectionId()).containsOnlyNulls();
        assertThat(sections.getFirst().questions().getFirst().options().getFirst().nextSectionKey())
            .isEqualTo("section-2");
        assertThat(sections.get(1).track()).isEqualTo(ChallengerTrack.PLAN);
    }

    private RecruitingRound round(
        Long seasonId,
        Long roundId,
        boolean interviewRequired,
        Long availabilityFormId,
        Long availabilityScheduleQuestionId
    ) {
        RecruitingSeason season = RecruitingSeason.create(1L, 2L);
        ReflectionTestUtils.setField(season, "id", seasonId);
        RecruitingRound result = RecruitingRound.createRegular(
            season,
            "15기 본모집",
            RecruitingRoundConfiguration.of(
                List.of(ChallengerTrack.PLAN),
                false,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-08T00:00:00Z"),
                Instant.parse("2026-08-10T00:00:00Z"),
                interviewRequired,
                interviewRequired ? Instant.parse("2026-08-11T00:00:00Z") : null,
                interviewRequired ? Instant.parse("2026-08-12T00:00:00Z") : null,
                Instant.parse("2026-08-16T00:00:00Z"),
                availabilityFormId,
                availabilityScheduleQuestionId,
                "공고",
                "연락처"
            )
        );
        ReflectionTestUtils.setField(result, "id", roundId);
        return result;
    }
}
