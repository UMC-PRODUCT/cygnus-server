package com.umc.product.feedback.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.feedback.application.port.in.command.dto.CreateUserFeedbackTemplateCommand;
import com.umc.product.feedback.application.port.in.command.dto.FeedbackTemplateQuestionEntry;
import com.umc.product.feedback.application.port.in.command.dto.FeedbackTemplateQuestionOptionEntry;
import com.umc.product.feedback.application.port.in.command.dto.FeedbackTemplateSectionEntry;
import com.umc.product.feedback.application.port.in.command.dto.UpdateUserFeedbackTemplateCommand;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.application.port.out.SaveUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.feedback.domain.exception.FeedbackDomainException;
import com.umc.product.feedback.domain.exception.FeedbackErrorCode;
import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionOptionUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionCommand;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;

@ExtendWith(MockitoExtension.class)
class UserFeedbackTemplateCommandServiceTest {

    private static final Long REQUESTER_ID = 99L;
    private static final Long TEMPLATE_ID = 100L;
    private static final Long FORM_ID = 500L;
    private static final Long SECTION_ID = 1000L;
    private static final Long QUESTION_ID = 2000L;
    private static final Long OPTION_ID = 3000L;

    @Mock
    LoadUserFeedbackTemplatePort loadTemplatePort;
    @Mock
    SaveUserFeedbackTemplatePort saveTemplatePort;
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
    @Mock
    GetFormResponseUseCase getFormResponseUseCase;

    @InjectMocks
    UserFeedbackTemplateCommandService sut;

    @Test
    @DisplayName("create는 폼을 생성하고 발행한 뒤 활성 템플릿을 저장한다")
    void create는_폼을_생성하고_발행한_뒤_활성_템플릿을_저장한다() {
        given(loadTemplatePort.existsActiveByContextAndTargetType(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER
        )).willReturn(false);
        given(manageFormUseCase.createDraft(any(CreateDraftFormCommand.class))).willReturn(FORM_ID);
        given(manageFormSectionUseCase.createSection(any())).willReturn(SECTION_ID);
        given(manageQuestionUseCase.createQuestion(any())).willReturn(QUESTION_ID);
        given(manageQuestionOptionUseCase.createOption(any())).willReturn(OPTION_ID);
        given(saveTemplatePort.save(any(UserFeedbackTemplate.class))).willAnswer(invocation -> {
            UserFeedbackTemplate template = invocation.getArgument(0);
            ReflectionTestUtils.setField(template, "id", TEMPLATE_ID);
            return template;
        });
        given(getFormUseCase.getFormWithStructure(FORM_ID)).willReturn(structure("새 폼"));

        sut.create(CreateUserFeedbackTemplateCommand.builder()
            .requesterMemberId(REQUESTER_ID)
            .context(UserFeedbackContext.APPLICATION_SUBMITTED)
            .targetType(UserFeedbackTargetType.NEW_CHALLENGER)
            .title("새 폼")
            .description("설명")
            .isAnonymous(false)
            .allowDuplicateResponses(false)
            .sections(List.of(section(null, "섹션", List.of(
                radioQuestion(null, "질문", List.of(option(null, "A")))
            ))))
            .build());

        then(manageFormUseCase).should().publishForm(any(PublishFormCommand.class));

        ArgumentCaptor<UserFeedbackTemplate> captor = ArgumentCaptor.forClass(UserFeedbackTemplate.class);
        then(saveTemplatePort).should().save(captor.capture());
        assertThat(captor.getValue().getContext()).isEqualTo(UserFeedbackContext.APPLICATION_SUBMITTED);
        assertThat(captor.getValue().getTargetType()).isEqualTo(UserFeedbackTargetType.NEW_CHALLENGER);
        assertThat(captor.getValue().getFormId()).isEqualTo(FORM_ID);
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("update는 기존 formId를 유지하고 폼 구조를 diff로 저장한다")
    void update는_기존_formId를_유지하고_폼_구조를_diff로_저장한다() {
        UserFeedbackTemplate template = template();
        given(loadTemplatePort.getById(TEMPLATE_ID)).willReturn(template);
        given(loadTemplatePort.existsActiveByContextAndTargetTypeExcludingId(
            UserFeedbackContext.MATCHING_COMPLETED,
            UserFeedbackTargetType.EXPERIENCED_CHALLENGER,
            TEMPLATE_ID
        )).willReturn(false);
        given(getFormUseCase.getFormWithStructure(FORM_ID))
            .willReturn(structure("기존 폼"))
            .willReturn(structure("수정 폼"));
        given(getFormResponseUseCase.listByFormId(FORM_ID)).willReturn(List.of());

        sut.update(UpdateUserFeedbackTemplateCommand.builder()
            .templateId(TEMPLATE_ID)
            .requesterMemberId(REQUESTER_ID)
            .context(UserFeedbackContext.MATCHING_COMPLETED)
            .targetType(UserFeedbackTargetType.EXPERIENCED_CHALLENGER)
            .title("수정 폼")
            .description(null)
            .isAnonymous(false)
            .allowDuplicateResponses(true)
            .sections(List.of(section(SECTION_ID, "수정 섹션", List.of(
                radioQuestion(QUESTION_ID, "수정 질문", List.of(option(OPTION_ID, "B")))
            ))))
            .build());

        assertThat(template.getFormId()).isEqualTo(FORM_ID);
        assertThat(template.getContext()).isEqualTo(UserFeedbackContext.MATCHING_COMPLETED);
        assertThat(template.getTargetType()).isEqualTo(UserFeedbackTargetType.EXPERIENCED_CHALLENGER);
        then(manageFormUseCase).should().updateForm(any(UpdateFormCommand.class));
        then(manageQuestionUseCase).should().updateQuestion(any(UpdateQuestionCommand.class));
        then(manageFormUseCase).should(never()).createDraft(any());
    }

    @Test
    @DisplayName("응답이 있는 폼에서 기존 질문 삭제는 거부한다")
    void 응답이_있는_폼에서_기존_질문_삭제는_거부한다() {
        given(loadTemplatePort.getById(TEMPLATE_ID)).willReturn(template());
        given(loadTemplatePort.existsActiveByContextAndTargetTypeExcludingId(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER,
            TEMPLATE_ID
        )).willReturn(false);
        given(getFormUseCase.getFormWithStructure(FORM_ID)).willReturn(structure("기존 폼"));
        given(getFormResponseUseCase.listByFormId(FORM_ID)).willReturn(List.of(
            FormResponseInfo.builder().id(1L).formId(FORM_ID).build()
        ));

        assertThatThrownBy(() -> sut.update(UpdateUserFeedbackTemplateCommand.builder()
            .templateId(TEMPLATE_ID)
            .requesterMemberId(REQUESTER_ID)
            .context(UserFeedbackContext.APPLICATION_SUBMITTED)
            .targetType(UserFeedbackTargetType.NEW_CHALLENGER)
            .title("기존 폼")
            .isAnonymous(false)
            .allowDuplicateResponses(false)
            .sections(List.of(section(SECTION_ID, "섹션", List.of())))
            .build()))
            .isInstanceOf(FeedbackDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FeedbackErrorCode.FEEDBACK_TEMPLATE_DESTRUCTIVE_CHANGE_NOT_ALLOWED);

        then(manageQuestionUseCase).should(never()).deleteQuestion(any(DeleteQuestionCommand.class));
    }

    private UserFeedbackTemplate template() {
        UserFeedbackTemplate template = UserFeedbackTemplate.create(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER,
            FORM_ID
        );
        ReflectionTestUtils.setField(template, "id", TEMPLATE_ID);
        return template;
    }

    private FormWithStructureInfo structure(String title) {
        return FormWithStructureInfo.builder()
            .formId(FORM_ID)
            .createdMemberId(REQUESTER_ID)
            .title(title)
            .description("설명")
            .status(FormStatus.PUBLISHED)
            .isAnonymous(false)
            .allowDuplicateResponses(false)
            .sections(List.of(FormWithStructureInfo.SectionWithQuestions.builder()
                .sectionId(SECTION_ID)
                .title("섹션")
                .orderNo(1L)
                .questions(List.of(FormWithStructureInfo.QuestionWithOptions.builder()
                    .questionId(QUESTION_ID)
                    .type(QuestionType.RADIO)
                    .title("질문")
                    .isRequired(true)
                    .orderNo(1L)
                    .options(List.of(FormWithStructureInfo.Option.builder()
                        .optionId(OPTION_ID)
                        .content("A")
                        .orderNo(1L)
                        .isOther(false)
                        .build()))
                    .build()))
                .build()))
            .build();
    }

    private FeedbackTemplateSectionEntry section(
        Long sectionId,
        String title,
        List<FeedbackTemplateQuestionEntry> questions
    ) {
        return FeedbackTemplateSectionEntry.builder()
            .sectionId(sectionId)
            .title(title)
            .orderNo(1L)
            .questions(questions)
            .build();
    }

    private FeedbackTemplateQuestionEntry radioQuestion(
        Long questionId,
        String title,
        List<FeedbackTemplateQuestionOptionEntry> options
    ) {
        return FeedbackTemplateQuestionEntry.builder()
            .questionId(questionId)
            .type(QuestionType.RADIO)
            .title(title)
            .isRequired(true)
            .orderNo(1L)
            .options(options)
            .build();
    }

    private FeedbackTemplateQuestionOptionEntry option(Long optionId, String content) {
        return FeedbackTemplateQuestionOptionEntry.builder()
            .optionId(optionId)
            .content(content)
            .orderNo(1L)
            .isOther(false)
            .build();
    }
}
