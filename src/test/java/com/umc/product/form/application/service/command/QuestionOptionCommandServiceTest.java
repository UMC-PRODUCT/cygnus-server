package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Test
    @DisplayName("createOption은 nextSectionId 대상 섹션의 orderNo가 현재보다 작으면 back-edge로 거부한다")
    void createOption_대상_섹션_orderNo가_현재보다_작으면_거부() {
        Form form = Form.createDraft("지원서", 10L, true);
        ReflectionTestUtils.setField(form, "id", 100L);
        FormSection currentSection = FormSection.create(form, "심화", null, 2L);
        ReflectionTestUtils.setField(currentSection, "id", 21L);
        FormSection targetSection = FormSection.create(form, "공통", null, 1L);
        ReflectionTestUtils.setField(targetSection, "id", 20L);
        Question question = Question.create("자기소개", QuestionType.RADIO, true, 1L);
        question.assignTo(currentSection);
        ReflectionTestUtils.setField(question, "id", 30L);

        given(loadQuestionPort.findById(30L)).willReturn(Optional.of(question));
        given(loadQuestionOptionPort.listByQuestionId(30L)).willReturn(List.of());
        given(loadFormSectionPort.findById(20L)).willReturn(Optional.of(targetSection));

        CreateQuestionOptionCommand command = CreateQuestionOptionCommand.builder()
            .questionId(30L)
            .requesterMemberId(99L)
            .content("남자")
            .isOther(false)
            .nextSectionId(20L)
            .build();

        assertThatThrownBy(() -> sut.createOption(command))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.INVALID_NEXT_SECTION_BACKWARD);
    }

    @Test
    @DisplayName("createOption은 nextSectionId 대상 섹션의 orderNo가 현재와 같으면 forward-only 위반으로 거부한다")
    void createOption_대상_섹션_orderNo가_현재와_같으면_거부() {
        Form form = Form.createDraft("지원서", 10L, true);
        ReflectionTestUtils.setField(form, "id", 100L);
        FormSection currentSection = FormSection.create(form, "공통", null, 1L);
        ReflectionTestUtils.setField(currentSection, "id", 20L);
        FormSection targetSection = FormSection.create(form, "공통-병렬", null, 1L);
        ReflectionTestUtils.setField(targetSection, "id", 21L);
        Question question = Question.create("자기소개", QuestionType.RADIO, true, 1L);
        question.assignTo(currentSection);
        ReflectionTestUtils.setField(question, "id", 30L);

        given(loadQuestionPort.findById(30L)).willReturn(Optional.of(question));
        given(loadQuestionOptionPort.listByQuestionId(30L)).willReturn(List.of());
        given(loadFormSectionPort.findById(21L)).willReturn(Optional.of(targetSection));

        CreateQuestionOptionCommand command = CreateQuestionOptionCommand.builder()
            .questionId(30L)
            .requesterMemberId(99L)
            .content("남자")
            .isOther(false)
            .nextSectionId(21L)
            .build();

        assertThatThrownBy(() -> sut.createOption(command))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.INVALID_NEXT_SECTION_BACKWARD);
    }

    @Test
    @DisplayName("createOption은 같은 섹션의 다른 질문이 이미 분기 옵션을 가지면 거부한다")
    void createOption_같은_섹션_다른_질문이_이미_분기중이면_거부() {
        Form form = Form.createDraft("지원서", 10L, true);
        ReflectionTestUtils.setField(form, "id", 100L);
        FormSection currentSection = FormSection.create(form, "공통", null, 1L);
        ReflectionTestUtils.setField(currentSection, "id", 20L);
        FormSection targetSection = FormSection.create(form, "심화", null, 2L);
        ReflectionTestUtils.setField(targetSection, "id", 21L);

        Question ownerQuestion = Question.create("성별", QuestionType.RADIO, true, 1L);
        ownerQuestion.assignTo(currentSection);
        ReflectionTestUtils.setField(ownerQuestion, "id", 30L);

        Question siblingQuestion = Question.create("연령", QuestionType.RADIO, true, 2L);
        siblingQuestion.assignTo(currentSection);
        ReflectionTestUtils.setField(siblingQuestion, "id", 31L);
        QuestionOption siblingBranchingOption = QuestionOption.create("20대", 1L, false, 21L);
        siblingBranchingOption.assignTo(siblingQuestion);
        ReflectionTestUtils.setField(siblingBranchingOption, "id", 50L);

        given(loadQuestionPort.findById(30L)).willReturn(Optional.of(ownerQuestion));
        given(loadQuestionOptionPort.listByQuestionId(30L)).willReturn(List.of());
        given(loadFormSectionPort.findById(21L)).willReturn(Optional.of(targetSection));
        given(loadQuestionPort.listBySectionId(20L)).willReturn(List.of(ownerQuestion, siblingQuestion));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(31L))).willReturn(List.of(siblingBranchingOption));

        CreateQuestionOptionCommand command = CreateQuestionOptionCommand.builder()
            .questionId(30L)
            .requesterMemberId(99L)
            .content("남자")
            .isOther(false)
            .nextSectionId(21L)
            .build();

        assertThatThrownBy(() -> sut.createOption(command))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.MULTIPLE_BRANCHING_QUESTIONS_IN_SECTION);
    }

    @Test
    @DisplayName("createOption은 같은 질문이 이미 분기 옵션을 가지고 있어도 추가 옵션 지정을 허용한다")
    void createOption_같은_질문에_기존_분기_옵션이_있어도_통과() {
        Form form = Form.createDraft("지원서", 10L, true);
        ReflectionTestUtils.setField(form, "id", 100L);
        FormSection currentSection = FormSection.create(form, "공통", null, 1L);
        ReflectionTestUtils.setField(currentSection, "id", 20L);
        FormSection targetSection = FormSection.create(form, "심화", null, 2L);
        ReflectionTestUtils.setField(targetSection, "id", 21L);

        Question ownerQuestion = Question.create("성별", QuestionType.RADIO, true, 1L);
        ownerQuestion.assignTo(currentSection);
        ReflectionTestUtils.setField(ownerQuestion, "id", 30L);
        QuestionOption existingBranchingOption = QuestionOption.create("여성", 1L, false, 21L);
        existingBranchingOption.assignTo(ownerQuestion);
        ReflectionTestUtils.setField(existingBranchingOption, "id", 50L);

        given(loadQuestionPort.findById(30L)).willReturn(Optional.of(ownerQuestion));
        given(loadQuestionOptionPort.listByQuestionId(30L)).willReturn(List.of(existingBranchingOption));
        given(loadFormSectionPort.findById(21L)).willReturn(Optional.of(targetSection));
        given(loadQuestionPort.listBySectionId(20L)).willReturn(List.of(ownerQuestion));
        given(saveQuestionOptionPort.save(any(QuestionOption.class))).willAnswer(invocation -> {
            QuestionOption saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 41L);
            return saved;
        });

        CreateQuestionOptionCommand command = CreateQuestionOptionCommand.builder()
            .questionId(30L)
            .requesterMemberId(99L)
            .content("남성")
            .isOther(false)
            .nextSectionId(21L)
            .build();

        sut.createOption(command);
    }

    @Test
    @DisplayName("updateOption은 대상 섹션 orderNo가 현재보다 작으면 back-edge로 거부한다")
    void updateOption_대상_섹션_orderNo가_현재보다_작으면_거부() {
        Form form = Form.createDraft("지원서", 10L, true);
        ReflectionTestUtils.setField(form, "id", 100L);
        FormSection currentSection = FormSection.create(form, "심화", null, 2L);
        ReflectionTestUtils.setField(currentSection, "id", 21L);
        FormSection targetSection = FormSection.create(form, "공통", null, 1L);
        ReflectionTestUtils.setField(targetSection, "id", 20L);
        Question question = Question.create("자기소개", QuestionType.RADIO, true, 1L);
        question.assignTo(currentSection);
        ReflectionTestUtils.setField(question, "id", 30L);
        QuestionOption option = QuestionOption.create("남자", 1L, false, null);
        option.assignTo(question);
        ReflectionTestUtils.setField(option, "id", 40L);

        given(loadQuestionOptionPort.findById(40L)).willReturn(Optional.of(option));
        given(loadFormSectionPort.findById(20L)).willReturn(Optional.of(targetSection));

        UpdateQuestionOptionCommand command = UpdateQuestionOptionCommand.builder()
            .optionId(40L)
            .requesterMemberId(99L)
            .nextSectionId(20L)
            .build();

        assertThatThrownBy(() -> sut.updateOption(command))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.INVALID_NEXT_SECTION_BACKWARD);
    }

    @Test
    @DisplayName("updateOption은 같은 섹션의 다른 질문이 이미 분기 옵션을 가지면 거부한다")
    void updateOption_같은_섹션_다른_질문이_이미_분기중이면_거부() {
        Form form = Form.createDraft("지원서", 10L, true);
        ReflectionTestUtils.setField(form, "id", 100L);
        FormSection currentSection = FormSection.create(form, "공통", null, 1L);
        ReflectionTestUtils.setField(currentSection, "id", 20L);
        FormSection targetSection = FormSection.create(form, "심화", null, 2L);
        ReflectionTestUtils.setField(targetSection, "id", 21L);

        Question ownerQuestion = Question.create("성별", QuestionType.RADIO, true, 1L);
        ownerQuestion.assignTo(currentSection);
        ReflectionTestUtils.setField(ownerQuestion, "id", 30L);
        QuestionOption option = QuestionOption.create("남성", 1L, false, null);
        option.assignTo(ownerQuestion);
        ReflectionTestUtils.setField(option, "id", 40L);

        Question siblingQuestion = Question.create("연령", QuestionType.RADIO, true, 2L);
        siblingQuestion.assignTo(currentSection);
        ReflectionTestUtils.setField(siblingQuestion, "id", 31L);
        QuestionOption siblingBranchingOption = QuestionOption.create("20대", 1L, false, 21L);
        siblingBranchingOption.assignTo(siblingQuestion);
        ReflectionTestUtils.setField(siblingBranchingOption, "id", 50L);

        given(loadQuestionOptionPort.findById(40L)).willReturn(Optional.of(option));
        given(loadFormSectionPort.findById(21L)).willReturn(Optional.of(targetSection));
        given(loadQuestionPort.listBySectionId(20L)).willReturn(List.of(ownerQuestion, siblingQuestion));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(31L))).willReturn(List.of(siblingBranchingOption));

        UpdateQuestionOptionCommand command = UpdateQuestionOptionCommand.builder()
            .optionId(40L)
            .requesterMemberId(99L)
            .nextSectionId(21L)
            .build();

        assertThatThrownBy(() -> sut.updateOption(command))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.MULTIPLE_BRANCHING_QUESTIONS_IN_SECTION);
    }
}
