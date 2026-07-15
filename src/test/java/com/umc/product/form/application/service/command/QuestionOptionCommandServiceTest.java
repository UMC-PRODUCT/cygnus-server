package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
import com.umc.product.form.domain.Form;
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
            .requesterMemberId(99L)
            .content("남자")
            .isOther(false)
            .nextSectionId(20L)
            .build();

        assertThatThrownBy(() -> sut.createOption(command))
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
            .requesterMemberId(99L)
            .nextSectionId(20L)
            .build();

        assertThatThrownBy(() -> sut.updateOption(command))
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
            .requesterMemberId(99L)
            .content("남자")
            .isOther(false)
            .nextSectionId(21L)
            .build();

        sut.createOption(command);
    }
}
