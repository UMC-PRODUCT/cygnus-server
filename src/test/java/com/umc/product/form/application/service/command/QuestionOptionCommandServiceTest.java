package com.umc.product.form.application.service.command;

import static com.umc.product.form.application.service.FormAccessTestFixtures.actor;
import static com.umc.product.form.application.service.FormAccessTestFixtures.owner;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.dto.CreateQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionOptionCommand;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class QuestionOptionCommandServiceTest {

    @Mock
    LoadFormSectionPort loadFormSectionPort;
    @Mock
    LoadQuestionPort loadQuestionPort;
    @Mock
    LoadQuestionOptionPort loadQuestionOptionPort;
    @Mock
    SaveQuestionOptionPort saveQuestionOptionPort;
    @Mock
    FormOwnershipAccessService ownershipAccessService;

    @InjectMocks
    QuestionOptionCommandService sut;

    @Test
    @DisplayName("createOption은 nextSectionId가 현재 섹션과 같으면 self-loop로 거부한다")
    void createOption_nextSectionId가_현재_섹션과_같으면_거부() {
        Form form = Form.createDraft("지원서", 10L, true);
        FormSection section = FormSection.create(form, "공통", null, 1L);
        ReflectionTestUtils.setField(section, "id", 20L);
        Question question = Question.create("자기소개", QuestionType.RADIO, true, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", 30L);

        given(loadQuestionPort.findById(30L)).willReturn(Optional.of(question));
        given(loadQuestionOptionPort.listByQuestionId(30L)).willReturn(List.of());

        CreateQuestionOptionCommand command = CreateQuestionOptionCommand.builder()
            .questionId(30L)
            .content("남자")
            .isOther(false)
            .nextSectionId(20L)
            .build();

        assertThatThrownBy(() -> sut.createOption(owner(1L), actor(99L), command))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.INVALID_NEXT_SECTION_SELF_LOOP);
    }

    @Test
    @DisplayName("updateOption은 nextSectionId가 현재 섹션과 같으면 self-loop로 거부한다")
    void updateOption_nextSectionId가_현재_섹션과_같으면_거부() {
        Form form = Form.createDraft("지원서", 10L, true);
        FormSection section = FormSection.create(form, "공통", null, 1L);
        ReflectionTestUtils.setField(section, "id", 20L);
        Question question = Question.create("자기소개", QuestionType.RADIO, true, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", 30L);
        QuestionOption option = QuestionOption.create("남자", 1L, false, null);
        option.assignTo(question);
        ReflectionTestUtils.setField(option, "id", 40L);

        given(loadQuestionOptionPort.findById(40L)).willReturn(Optional.of(option));

        UpdateQuestionOptionCommand command = UpdateQuestionOptionCommand.builder()
            .optionId(40L)
            .nextSectionId(20L)
            .build();

        assertThatThrownBy(() -> sut.updateOption(owner(1L), actor(99L), command))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.INVALID_NEXT_SECTION_SELF_LOOP);
    }

    @Test
    @DisplayName("createOption은 nextSectionId가 다른 섹션이면 self-loop 검증을 통과한다")
    void createOption_nextSectionId가_다른_섹션이면_통과() {
        Form form = Form.createDraft("지원서", 10L, true);
        ReflectionTestUtils.setField(form, "id", 100L);
        FormSection currentSection = FormSection.create(form, "공통", null, 1L);
        ReflectionTestUtils.setField(currentSection, "id", 20L);
        FormSection targetSection = FormSection.create(form, "심화", null, 2L);
        ReflectionTestUtils.setField(targetSection, "id", 21L);
        Question question = Question.create("자기소개", QuestionType.RADIO, true, 1L);
        question.assignTo(currentSection);
        ReflectionTestUtils.setField(question, "id", 30L);

        given(loadQuestionPort.findById(30L)).willReturn(Optional.of(question));
        given(loadQuestionOptionPort.listByQuestionId(30L)).willReturn(List.of());
        given(loadFormSectionPort.findById(21L)).willReturn(Optional.of(targetSection));
        given(saveQuestionOptionPort.save(any(QuestionOption.class))).willAnswer(invocation -> {
            QuestionOption saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 40L);
            return saved;
        });

        CreateQuestionOptionCommand command = CreateQuestionOptionCommand.builder()
            .questionId(30L)
            .content("남자")
            .isOther(false)
            .nextSectionId(21L)
            .build();

        sut.createOption(owner(100L), actor(99L), command);
    }

    @Test
    @DisplayName("foreign option 수정은 question-section-form parent chain ownership으로 거부한다")
    void foreign_option_수정은_parent_form_ownership으로_거부한다() {
        Form form = Form.createDraft("지원서", 10L);
        ReflectionTestUtils.setField(form, "id", 100L);
        FormSection section = FormSection.create(form, "공통", null, 1L);
        Question question = Question.create("질문", QuestionType.RADIO, true, 1L);
        question.assignTo(section);
        QuestionOption option = QuestionOption.create("선택", 1L, false, null);
        option.assignTo(question);
        ReflectionTestUtils.setField(option, "id", 40L);
        given(loadQuestionOptionPort.findById(40L)).willReturn(Optional.of(option));
        willThrow(new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN))
            .given(ownershipAccessService)
            .requireMutation(100L, owner(999L), actor(99L), FormOperation.MANAGE_STRUCTURE);

        assertThatThrownBy(() -> sut.updateOption(
            owner(999L),
            actor(99L),
            UpdateQuestionOptionCommand.builder().optionId(40L).content("변경").build()
        ))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);

        then(saveQuestionOptionPort).should(never()).save(option);
    }
}
