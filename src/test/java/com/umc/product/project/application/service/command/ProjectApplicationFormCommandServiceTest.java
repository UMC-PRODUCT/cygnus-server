package com.umc.product.project.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionOptionUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.CreateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.ForkQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderFormSectionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionOptionCommand;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.Option;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.QuestionWithOptions;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.SectionWithQuestions;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand.ApplicationFormSectionEntry;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand.ApplicationQuestionEntry;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand.ApplicationQuestionOptionEntry;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationFormCommandServiceTest {

    @Mock
    LoadProjectPort loadProjectPort;
    @Mock
    LoadProjectMatchingRoundPort loadMatchingRoundPort;
    @Mock
    LoadProjectApplicationFormPort loadApplicationFormPort;
    @Mock
    SaveProjectApplicationFormPort saveApplicationFormPort;
    @Mock
    LoadProjectApplicationFormPolicyPort loadPolicyPort;
    @Mock
    SaveProjectApplicationFormPolicyPort savePolicyPort;
    @Mock
    ManageFormUseCase manageFormUseCase;
    @Mock
    ManageFormSectionUseCase manageFormSectionUseCase;
    @Mock
    ManageQuestionUseCase manageQuestionUseCase;
    @Mock
    ManageQuestionOptionUseCase manageQuestionOptionUseCase;
    @Mock
    GetFormUseCase getFormUseCase;

    @InjectMocks
    ProjectApplicationFormCommandService sut;

    @BeforeEach
    void setUp() {
        lenient().when(loadMatchingRoundPort.listOpenAt(any(), any(Instant.class))).thenReturn(List.of());
    }

    @Test
    @DisplayName("Form 생성과 consumer mirror 저장은 REQUIRED transaction 경계에 있다")
    void 지원_Form_생성과_consumer_mirror_저장은_REQUIRED_transaction이다() {
        Transactional transactional = ProjectApplicationFormCommandService.class
            .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRED);
    }

    /* =====================================================
     * 폼 라이프사이클 (C6 회귀 보장)
     * ===================================================== */

    @Nested
    class formLifecycle {

        @Test
        void 폼이_없으면_createDraft_호출_후_save() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.empty());
            given(manageFormUseCase.createDraft(any(), any(), any())).willReturn(500L);

            ProjectApplicationForm savedForm = createApplicationForm(project, 100L, 500L);
            given(saveApplicationFormPort.save(any())).willReturn(savedForm);
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(emptyStructure());
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of());

            sut.upsert(emptyCommand(42L));

            ArgumentCaptor<FormOwnerReferenceFactory> factoryCaptor =
                ArgumentCaptor.forClass(FormOwnerReferenceFactory.class);
            ArgumentCaptor<FormActorContext> actorCaptor = ArgumentCaptor.forClass(FormActorContext.class);
            ArgumentCaptor<CreateDraftFormCommand> captor = ArgumentCaptor.forClass(CreateDraftFormCommand.class);
            InOrder order = inOrder(manageFormUseCase, saveApplicationFormPort);
            order.verify(manageFormUseCase).createDraft(
                factoryCaptor.capture(), actorCaptor.capture(), captor.capture()
            );
            order.verify(saveApplicationFormPort).save(any());
            assertThat(factoryCaptor.getValue().create(500L).namespace())
                .isEqualTo("project.application-form");
            assertThat(factoryCaptor.getValue().create(500L).ownerResourceKey()).isEqualTo("42");
            assertThat(factoryCaptor.getValue().create(500L).slot()).isEqualTo("default");
            assertThat(actorCaptor.getValue().authenticatedMemberId()).contains(99L);
            assertThat(captor.getValue().title()).isEqualTo("Triple");
            assertThat(captor.getValue().allowDuplicateResponses()).isTrue();
            then(manageFormUseCase).should(never()).updateForm(any(), any(), any());
        }

        @Test
        void 본문_title이_null이고_Project_name도_null이면_default_title_사용() {
            Project project = createProject(42L, ProjectStatus.DRAFT, null);
            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.empty());
            given(manageFormUseCase.createDraft(any(), any(), any())).willReturn(500L);
            given(saveApplicationFormPort.save(any())).willReturn(createApplicationForm(project, 100L, 500L));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(emptyStructure());
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of());

            sut.upsert(emptyCommand(42L));

            ArgumentCaptor<CreateDraftFormCommand> captor = ArgumentCaptor.forClass(CreateDraftFormCommand.class);
            then(manageFormUseCase).should().createDraft(any(), any(), captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("프로젝트 지원서");
        }

        @Test
        @DisplayName("ownership 등록 실패 시 consumer formId mirror를 저장하지 않는다")
        void ownership_등록_실패_시_formId_mirror를_저장하지_않는다() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.empty());
            given(manageFormUseCase.createDraft(any(), any(), any()))
                .willThrow(new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN));

            assertThatThrownBy(() -> sut.upsert(emptyCommand(42L)))
                .isInstanceOf(FormDomainException.class);

            then(saveApplicationFormPort).should(never()).save(any());
            then(manageFormSectionUseCase).shouldHaveNoInteractions();
        }

        @Test
        void 폼이_있고_meta가_같으면_updateForm_호출_안_함() {
            stubExistingFormWithEmptyStructure(42L, 100L, 500L, "Triple", "팀 소개");

            UpsertApplicationFormCommand cmd = UpsertApplicationFormCommand.builder()
                .projectId(42L).requesterMemberId(99L)
                .title("Triple").description("팀 소개")
                .sections(List.of())
                .build();

            sut.upsert(cmd);

            then(manageFormUseCase).should(never()).updateForm(any(), any(), any());
        }

        @Test
        void 폼이_있고_title이_바뀌면_updateForm_호출() {
            stubExistingFormWithEmptyStructure(42L, 100L, 500L, "예전 제목", null);

            UpsertApplicationFormCommand cmd = UpsertApplicationFormCommand.builder()
                .projectId(42L).requesterMemberId(99L)
                .title("새 제목").description(null)
                .sections(List.of())
                .build();

            sut.upsert(cmd);

            ArgumentCaptor<UpdateFormCommand> captor = ArgumentCaptor.forClass(UpdateFormCommand.class);
            then(manageFormUseCase).should().updateForm(any(), any(), captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("새 제목");
        }

        @Test
        void 폼_description이_null이면_기존_description_삭제를_요청한다() {
            stubExistingFormWithEmptyStructure(42L, 100L, 500L, "Triple", "기존 설명");

            UpsertApplicationFormCommand cmd = UpsertApplicationFormCommand.builder()
                .projectId(42L).requesterMemberId(99L)
                .title("Triple").description(null)
                .sections(List.of())
                .build();

            sut.upsert(cmd);

            ArgumentCaptor<UpdateFormCommand> captor = ArgumentCaptor.forClass(UpdateFormCommand.class);
            then(manageFormUseCase).should().updateForm(any(), any(), captor.capture());
            assertThat(captor.getValue().description()).isNull();
            assertThat(captor.getValue().clearDescription()).isTrue();
        }
    }

    /* =====================================================
     * 상태 가드
     * ===================================================== */

    @Nested
    class stateGuards {

        @Test
        void Project가_COMPLETED면_PROJECT_INVALID_STATE() {
            Project project = createProject(42L, ProjectStatus.COMPLETED, "Triple");
            given(loadProjectPort.getById(42L)).willReturn(project);

            assertThatThrownBy(() -> sut.upsert(emptyCommand(42L)))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_STATE);
        }

        @Test
        void Project가_IN_PROGRESS이고_활성_차수_있으면_PROJECT_INVALID_STATE() {
            Project project = createProject(42L, ProjectStatus.IN_PROGRESS, "Triple");
            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadMatchingRoundPort.listOpenAt(any(), any(Instant.class)))
                .willReturn(List.of(mock(com.umc.product.project.domain.ProjectMatchingRound.class)));

            assertThatThrownBy(() -> sut.upsert(emptyCommand(42L)))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_STATE);

            then(manageFormUseCase).should(never()).createDraft(any(), any(), any());
        }
    }

    /* =====================================================
     * fork 시나리오 (차수 사이 질문 수정)
     * ===================================================== */

    @Nested
    class forkScenario {

        @Test
        @DisplayName("IN_PROGRESS 차수 사이에 질문 메타 변경 시 fork 후 새 질문에 요청 메타를 반영한다")
        void IN_PROGRESS_차수_사이_질문_meta_변경시_fork_후_새_질문에_요청_meta를_반영한다() {
            Project project = createProject(42L, ProjectStatus.IN_PROGRESS, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any()))
                .willReturn(structure(List.of(
                    existingSection(1000L, "공통", null, 1L, List.of(
                        existingQuestion(2000L, QuestionType.SHORT_TEXT, "기존 질문", null, true, 1L, List.of())
                    ))
                )))
                .willReturn(structure(List.of(
                    existingSection(1000L, "공통", null, 1L, List.of(
                        existingQuestion(9999L, QuestionType.SHORT_TEXT, "변경된 질문", null, true, 1L, List.of())
                    ))
                )));
            given(loadPolicyPort.listByApplicationFormId(100L))
                .willReturn(List.of(ProjectApplicationFormPolicy.createCommon(form, 1000L)));
            given(manageQuestionUseCase.forkQuestion(any(), any(), any())).willReturn(9999L);

            sut.upsert(commandWithSections(42L, List.of(
                section(1000L, FormSectionType.COMMON, Set.of(), "공통", 1, List.of(
                    ApplicationQuestionEntry.builder()
                        .questionId(2000L)
                        .type(QuestionType.LONG_TEXT)
                        .title("변경된 질문")
                        .description("변경된 설명")
                        .isRequired(false)
                        .orderNo(1)
                        .options(List.of())
                        .build()
                ))
            )));

            ArgumentCaptor<ForkQuestionCommand> captor = ArgumentCaptor.forClass(ForkQuestionCommand.class);
            then(manageQuestionUseCase).should().forkQuestion(any(), any(), captor.capture());
            assertThat(captor.getValue().originQuestionId()).isEqualTo(2000L);

            ArgumentCaptor<UpdateQuestionCommand> updateCaptor =
                ArgumentCaptor.forClass(UpdateQuestionCommand.class);
            then(manageQuestionUseCase).should().updateQuestion(any(), any(), updateCaptor.capture());
            assertThat(updateCaptor.getValue().questionId()).isEqualTo(9999L);
            assertThat(updateCaptor.getValue().type()).isEqualTo(QuestionType.LONG_TEXT);
            assertThat(updateCaptor.getValue().title()).isEqualTo("변경된 질문");
            assertThat(updateCaptor.getValue().description()).isEqualTo("변경된 설명");
            assertThat(updateCaptor.getValue().isRequired()).isFalse();
        }
    }

    /* =====================================================
     * 섹션 / 질문 / 옵션 diff
     * ===================================================== */

    @Nested
    class sectionDiff {

        @Test
        void 신규_섹션_생성시_createSection_정책_저장_질문_생성_reorder_호출() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(emptyStructure());
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of());
            given(manageFormSectionUseCase.createSection(any(), any(), any())).willReturn(1000L);
            given(manageQuestionUseCase.createQuestion(any(), any(), any())).willReturn(2000L);
            given(manageQuestionOptionUseCase.createOption(any(), any(), any())).willReturn(3000L, 3001L);

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(null, FormSectionType.PART, Set.of(ChallengerPart.WEB), "프론트엔드", 1, List.of(
                    radioQuestion(null, "선호 프레임워크", List.of(
                        option(null, "React"),
                        option(null, "Vue")
                    ))
                ))
            ));

            sut.upsert(cmd);

            ArgumentCaptor<CreateFormSectionCommand> sectionCaptor = ArgumentCaptor.forClass(CreateFormSectionCommand.class);
            then(manageFormSectionUseCase).should().createSection(any(), any(), sectionCaptor.capture());
            assertThat(sectionCaptor.getValue().title()).isEqualTo("프론트엔드");

            ArgumentCaptor<ProjectApplicationFormPolicy> policyCaptor =
                ArgumentCaptor.forClass(ProjectApplicationFormPolicy.class);
            then(savePolicyPort).should().save(policyCaptor.capture());
            assertThat(policyCaptor.getValue().getType()).isEqualTo(FormSectionType.PART);
            assertThat(policyCaptor.getValue().getAllowedParts()).containsExactly(ChallengerPart.WEB);

            then(manageQuestionUseCase).should().createQuestion(any(), any(), any(CreateQuestionCommand.class));
            then(manageQuestionOptionUseCase).should(times(2))
                .createOption(any(), any(), any(CreateQuestionOptionCommand.class));

            ArgumentCaptor<ReorderFormSectionsCommand> reorderCaptor =
                ArgumentCaptor.forClass(ReorderFormSectionsCommand.class);
            then(manageFormSectionUseCase).should().reorderSections(any(), any(), reorderCaptor.capture());
            assertThat(reorderCaptor.getValue().orderedSectionIds()).containsExactly(1000L);
        }

        @Test
        void 기존_섹션_meta_변경_없으면_updateSection_호출_안_함_idempotent() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));

            // 기존: 섹션 1 (COMMON) — title="공통"
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(structure(List.of(
                existingSection(1000L, "공통", null, 1L, List.of())
            )));
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(
                ProjectApplicationFormPolicy.createCommon(form, 1000L)
            ));

            // 본문 동일
            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(1000L, FormSectionType.COMMON, Set.of(), "공통", 1, List.of())
            ));

            sut.upsert(cmd);

            then(manageFormSectionUseCase).should(never()).updateSection(any(), any(), any());
            then(savePolicyPort).should(never()).save(any());
            then(manageFormSectionUseCase).should(never()).deleteSection(any(), any(), any());
        }

        @Test
        void 기존_섹션_title_변경시_updateSection_호출() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(structure(List.of(
                existingSection(1000L, "예전 이름", null, 1L, List.of())
            )));
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(
                ProjectApplicationFormPolicy.createCommon(form, 1000L)
            ));

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(1000L, FormSectionType.COMMON, Set.of(), "새 이름", 1, List.of())
            ));

            sut.upsert(cmd);

            ArgumentCaptor<UpdateFormSectionCommand> captor = ArgumentCaptor.forClass(UpdateFormSectionCommand.class);
            then(manageFormSectionUseCase).should().updateSection(any(), any(), captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("새 이름");
        }

        @Test
        void 섹션_description이_null이면_기존_description_삭제를_요청한다() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(structure(List.of(
                existingSection(1000L, "공통", "기존 설명", 1L, List.of())
            )));
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(
                ProjectApplicationFormPolicy.createCommon(form, 1000L)
            ));

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(1000L, FormSectionType.COMMON, Set.of(), "공통", 1, List.of())
            ));

            sut.upsert(cmd);

            ArgumentCaptor<UpdateFormSectionCommand> captor = ArgumentCaptor.forClass(UpdateFormSectionCommand.class);
            then(manageFormSectionUseCase).should().updateSection(any(), any(), captor.capture());
            assertThat(captor.getValue().description()).isNull();
            assertThat(captor.getValue().clearDescription()).isTrue();
        }

        @Test
        void 정책_타입이_바뀌면_savePolicy_호출_COMMON_to_PART() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);
            ProjectApplicationFormPolicy existingPolicy =
                ProjectApplicationFormPolicy.createCommon(form, 1000L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(structure(List.of(
                existingSection(1000L, "공통", null, 1L, List.of())
            )));
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(existingPolicy));

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(1000L, FormSectionType.PART, Set.of(ChallengerPart.WEB), "공통", 1, List.of())
            ));

            sut.upsert(cmd);

            then(savePolicyPort).should().save(existingPolicy);
            assertThat(existingPolicy.getType()).isEqualTo(FormSectionType.PART);
            assertThat(existingPolicy.getAllowedParts()).containsExactly(ChallengerPart.WEB);
        }

        @Test
        void 본문에서_빠진_섹션은_deleteSection_및_정책_삭제() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(structure(List.of(
                existingSection(1000L, "공통", null, 1L, List.of()),
                existingSection(1001L, "지울 섹션", null, 2L, List.of())
            )));
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(
                ProjectApplicationFormPolicy.createCommon(form, 1000L),
                ProjectApplicationFormPolicy.createCommon(form, 1001L)
            ));

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(1000L, FormSectionType.COMMON, Set.of(), "공통", 1, List.of())
            ));

            sut.upsert(cmd);

            ArgumentCaptor<DeleteFormSectionCommand> deleteCaptor =
                ArgumentCaptor.forClass(DeleteFormSectionCommand.class);
            then(manageFormSectionUseCase).should().deleteSection(any(), any(), deleteCaptor.capture());
            assertThat(deleteCaptor.getValue().sectionId()).isEqualTo(1001L);
            then(savePolicyPort).should().deleteByFormSectionId(1001L);
        }
    }

    @Nested
    class questionAndOptionDiff {

        @Test
        void 기존_질문_삭제시_deleteQuestion_호출() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(structure(List.of(
                existingSection(1000L, "공통", null, 1L, List.of(
                    existingQuestion(2000L, QuestionType.SHORT_TEXT, "남길 질문", null, true, 1L, List.of()),
                    existingQuestion(2001L, QuestionType.SHORT_TEXT, "지울 질문", null, false, 2L, List.of())
                ))
            )));
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(
                ProjectApplicationFormPolicy.createCommon(form, 1000L)
            ));

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(1000L, FormSectionType.COMMON, Set.of(), "공통", 1, List.of(
                    shortTextQuestion(2000L, "남길 질문", true)
                ))
            ));

            sut.upsert(cmd);

            ArgumentCaptor<DeleteQuestionCommand> captor = ArgumentCaptor.forClass(DeleteQuestionCommand.class);
            then(manageQuestionUseCase).should().deleteQuestion(any(), any(), captor.capture());
            assertThat(captor.getValue().questionId()).isEqualTo(2001L);
        }

        @Test
        void 질문_description이_null이면_기존_description_삭제를_요청한다() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(structure(List.of(
                existingSection(1000L, "공통", null, 1L, List.of(
                    existingQuestion(2000L, QuestionType.SHORT_TEXT, "자기소개", "기존 설명", true, 1L, List.of())
                ))
            )));
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(
                ProjectApplicationFormPolicy.createCommon(form, 1000L)
            ));

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(1000L, FormSectionType.COMMON, Set.of(), "공통", 1, List.of(
                    shortTextQuestion(2000L, "자기소개", true)
                ))
            ));

            sut.upsert(cmd);

            ArgumentCaptor<UpdateQuestionCommand> captor = ArgumentCaptor.forClass(UpdateQuestionCommand.class);
            then(manageQuestionUseCase).should().updateQuestion(any(), any(), captor.capture());
            assertThat(captor.getValue().description()).isNull();
            assertThat(captor.getValue().clearDescription()).isTrue();
        }

        @Test
        void 기존_옵션_content_변경시_updateOption_호출() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(structure(List.of(
                existingSection(1000L, "공통", null, 1L, List.of(
                    existingQuestion(2000L, QuestionType.RADIO, "선호도", null, true, 1L, List.of(
                        existingOption(3000L, "예전 답안", 1L, false)
                    ))
                ))
            )));
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(
                ProjectApplicationFormPolicy.createCommon(form, 1000L)
            ));

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(1000L, FormSectionType.COMMON, Set.of(), "공통", 1, List.of(
                    radioQuestion(2000L, "선호도", List.of(option(3000L, "새 답안")))
                ))
            ));

            sut.upsert(cmd);

            ArgumentCaptor<UpdateQuestionOptionCommand> captor =
                ArgumentCaptor.forClass(UpdateQuestionOptionCommand.class);
            then(manageQuestionOptionUseCase).should().updateOption(any(), any(), captor.capture());
            assertThat(captor.getValue().content()).isEqualTo("새 답안");
        }

        @Test
        void 옵션_삭제시_deleteOption_호출() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(structure(List.of(
                existingSection(1000L, "공통", null, 1L, List.of(
                    existingQuestion(2000L, QuestionType.RADIO, "선호도", null, true, 1L, List.of(
                        existingOption(3000L, "남길 옵션", 1L, false),
                        existingOption(3001L, "지울 옵션", 2L, false)
                    ))
                ))
            )));
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(
                ProjectApplicationFormPolicy.createCommon(form, 1000L)
            ));

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(1000L, FormSectionType.COMMON, Set.of(), "공통", 1, List.of(
                    radioQuestion(2000L, "선호도", List.of(option(3000L, "남길 옵션")))
                ))
            ));

            sut.upsert(cmd);

            ArgumentCaptor<DeleteQuestionOptionCommand> captor =
                ArgumentCaptor.forClass(DeleteQuestionOptionCommand.class);
            then(manageQuestionOptionUseCase).should().deleteOption(any(), any(), captor.capture());
            assertThat(captor.getValue().optionId()).isEqualTo(3001L);
        }
    }

    /* =====================================================
     * 검증
     * ===================================================== */

    @Nested
    class validation {

        @Test
        void choice_타입에_옵션_0개면_OPTIONS_REQUIRED() {
            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(null, FormSectionType.COMMON, Set.of(), "섹션", 1, List.of(
                    radioQuestion(null, "선택지 없음", List.of())
                ))
            ));

            assertThatThrownBy(() -> sut.upsert(cmd))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.APPLICATION_FORM_OPTIONS_REQUIRED);

            then(loadProjectPort).should(never()).getById(anyLong());
        }

        @Test
        void non_choice_타입에_옵션_있으면_OPTIONS_NOT_ALLOWED() {
            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(null, FormSectionType.COMMON, Set.of(), "섹션", 1, List.of(
                    ApplicationQuestionEntry.builder()
                        .questionId(null).type(QuestionType.SHORT_TEXT).title("주관식")
                        .description(null).isRequired(true).orderNo(1)
                        .options(List.of(option(null, "있을 수 없는 옵션")))
                        .build()
                ))
            ));

            assertThatThrownBy(() -> sut.upsert(cmd))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.APPLICATION_FORM_OPTIONS_NOT_ALLOWED);
        }

        @Test
        void 존재하지_않는_sectionId면_INVALID_SECTION_ID() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(emptyStructure());
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of());

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(9999L, FormSectionType.COMMON, Set.of(), "잘못된 섹션", 1, List.of())
            ));

            assertThatThrownBy(() -> sut.upsert(cmd))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.APPLICATION_FORM_INVALID_SECTION_ID);
        }

        @Test
        void 신규_섹션에_questionId가_있으면_INVALID_QUESTION_ID() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(emptyStructure());
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of());

            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(null, FormSectionType.COMMON, Set.of(), "섹션", 1, List.of(
                    shortTextQuestion(9999L, "이상한 질문", true)
                ))
            ));

            assertThatThrownBy(() -> sut.upsert(cmd))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.APPLICATION_FORM_INVALID_QUESTION_ID);
        }

        @Test
        void 다른_섹션의_questionId면_INVALID_QUESTION_ID() {
            Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
            ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

            given(loadProjectPort.getById(42L)).willReturn(project);
            given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
            given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(structure(List.of(
                existingSection(1000L, "A", null, 1L, List.of(
                    existingQuestion(2000L, QuestionType.SHORT_TEXT, "Q1", null, true, 1L, List.of())
                )),
                existingSection(1001L, "B", null, 2L, List.of(
                    existingQuestion(2001L, QuestionType.SHORT_TEXT, "Q2", null, true, 1L, List.of())
                ))
            )));
            given(loadPolicyPort.listByApplicationFormId(100L)).willReturn(List.of(
                ProjectApplicationFormPolicy.createCommon(form, 1000L),
                ProjectApplicationFormPolicy.createCommon(form, 1001L)
            ));

            // 1000 섹션에 1001 섹션의 질문 ID(2001)를 끼워넣음
            UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
                section(1000L, FormSectionType.COMMON, Set.of(), "A", 1, List.of(
                    shortTextQuestion(2001L, "잘못된 질문", true)
                ))
            ));

            assertThatThrownBy(() -> sut.upsert(cmd))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.APPLICATION_FORM_INVALID_QUESTION_ID);
        }
    }

    /* =====================================================
     * 응답 조립
     * ===================================================== */

    @Test
    void upsert_후_응답에_업데이트된_구조가_담겨야() {
        Project project = createProject(42L, ProjectStatus.DRAFT, "Triple");
        ProjectApplicationForm form = createApplicationForm(project, 100L, 500L);

        given(loadProjectPort.getById(42L)).willReturn(project);
        given(loadApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));
        // service 가 두 번 호출 — 1) syncFormMetaIfChanged + applyDiff 시점, 2) assembleResponse 시점
        given(getFormUseCase.getFormWithStructure(any(), any()))
            .willReturn(emptyStructure())
            .willReturn(structure(List.of(existingSection(1000L, "공통", null, 1L, List.of()))));
        given(loadPolicyPort.listByApplicationFormId(100L))
            .willReturn(List.of())
            .willReturn(List.of(ProjectApplicationFormPolicy.createCommon(form, 1000L)));
        given(manageFormSectionUseCase.createSection(any(), any(), any())).willReturn(1000L);

        UpsertApplicationFormCommand cmd = commandWithSections(42L, List.of(
            section(null, FormSectionType.COMMON, Set.of(), "공통", 1, List.of())
        ));

        ApplicationFormInfo result = sut.upsert(cmd);

        assertThat(result.applicationFormId()).isEqualTo(100L);
        assertThat(result.sections()).hasSize(1);
        assertThat(result.sections().get(0).type()).isEqualTo(FormSectionType.COMMON);
    }

    /* =====================================================
     * Helpers
     * ===================================================== */

    private UpsertApplicationFormCommand emptyCommand(Long projectId) {
        return UpsertApplicationFormCommand.builder()
            .projectId(projectId).requesterMemberId(99L)
            .title(null).description(null)
            .sections(List.of())
            .build();
    }

    private UpsertApplicationFormCommand commandWithSections(Long projectId, List<ApplicationFormSectionEntry> sections) {
        return UpsertApplicationFormCommand.builder()
            .projectId(projectId).requesterMemberId(99L)
            .title("Triple").description(null)
            .sections(sections)
            .build();
    }

    private ApplicationFormSectionEntry section(
        Long sectionId, FormSectionType type, Set<ChallengerPart> allowedParts,
        String title, int orderNo, List<ApplicationQuestionEntry> questions
    ) {
        return ApplicationFormSectionEntry.builder()
            .sectionId(sectionId).type(type).allowedParts(allowedParts)
            .title(title).description(null).orderNo(orderNo).questions(questions)
            .build();
    }

    private ApplicationQuestionEntry radioQuestion(Long id, String title, List<ApplicationQuestionOptionEntry> opts) {
        return ApplicationQuestionEntry.builder()
            .questionId(id).type(QuestionType.RADIO).title(title)
            .description(null).isRequired(true).orderNo(1).options(opts)
            .build();
    }

    private ApplicationQuestionEntry shortTextQuestion(Long id, String title, boolean required) {
        return ApplicationQuestionEntry.builder()
            .questionId(id).type(QuestionType.SHORT_TEXT).title(title)
            .description(null).isRequired(required).orderNo(1).options(List.of())
            .build();
    }

    private ApplicationQuestionOptionEntry option(Long id, String content) {
        return ApplicationQuestionOptionEntry.builder()
            .optionId(id).content(content).orderNo(1).isOther(false)
            .build();
    }

    private FormWithStructureInfo emptyStructure() {
        return structure(List.of());
    }

    private FormWithStructureInfo structure(List<SectionWithQuestions> sections) {
        return FormWithStructureInfo.builder()
            .formId(500L).title("Triple").description(null)
            .status(FormStatus.DRAFT).isAnonymous(false)
            .sections(sections)
            .build();
    }

    private SectionWithQuestions existingSection(
        Long id, String title, String description, Long orderNo, List<QuestionWithOptions> questions
    ) {
        return SectionWithQuestions.builder()
            .sectionId(id).title(title).description(description).orderNo(orderNo)
            .questions(questions)
            .build();
    }

    private QuestionWithOptions existingQuestion(
        Long id, QuestionType type, String title, String description,
        boolean isRequired, Long orderNo, List<Option> options
    ) {
        return QuestionWithOptions.builder()
            .questionId(id).type(type).title(title).description(description)
            .isRequired(isRequired).orderNo(orderNo).options(options)
            .build();
    }

    private Option existingOption(Long id, String content, Long orderNo, boolean isOther) {
        return Option.builder()
            .optionId(id).content(content).orderNo(orderNo).isOther(isOther)
            .build();
    }

    private void stubExistingFormWithEmptyStructure(
        Long projectId, Long formRowId, Long formFormId, String formTitle, String formDescription
    ) {
        Project project = createProject(projectId, ProjectStatus.DRAFT, "Triple");
        ProjectApplicationForm form = createApplicationForm(project, formRowId, formFormId);

        given(loadProjectPort.getById(projectId)).willReturn(project);
        given(loadApplicationFormPort.findByProjectId(projectId)).willReturn(Optional.of(form));
        given(getFormUseCase.getFormWithStructure(any(), any()))
            .willReturn(structureWithMeta(formFormId, formTitle, formDescription, List.of()));
        given(loadPolicyPort.listByApplicationFormId(formRowId)).willReturn(List.of());
    }

    private FormWithStructureInfo structureWithMeta(
        Long formId, String title, String description, List<SectionWithQuestions> sections
    ) {
        return FormWithStructureInfo.builder()
            .formId(formId).title(title).description(description)
            .status(FormStatus.DRAFT).isAnonymous(false)
            .sections(sections)
            .build();
    }

    private Project createProject(Long id, ProjectStatus status, String name) {
        Project project;
        try {
            var constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            project = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(project, "id", id);
        ReflectionTestUtils.setField(project, "gisuId", 1L);
        ReflectionTestUtils.setField(project, "chapterId", 1L);
        ReflectionTestUtils.setField(project, "status", status);
        ReflectionTestUtils.setField(project, "name", name);
        ReflectionTestUtils.setField(project, "productOwnerMemberId", 99L);
        ReflectionTestUtils.setField(project, "creatorMemberId", 99L);
        return project;
    }

    private ProjectApplicationForm createApplicationForm(Project project, Long id, Long formId) {
        ProjectApplicationForm form = ProjectApplicationForm.create(project, formId);
        ReflectionTestUtils.setField(form, "id", id);
        return form;
    }
}
