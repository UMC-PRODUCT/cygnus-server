package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveAnswerPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;

@ExtendWith(MockitoExtension.class)
class QuestionCommandServiceTest {

    @Mock
    LoadFormSectionPort loadFormSectionPort;
    @Mock
    LoadQuestionPort loadQuestionPort;
    @Mock
    SaveQuestionPort saveQuestionPort;
    @Mock
    SaveQuestionOptionPort saveQuestionOptionPort;
    @Mock
    SaveAnswerPort saveAnswerPort;

    @InjectMocks
    QuestionCommandService sut;

    @Test
    @DisplayName("createQuestion은 요청 description을 신규 질문에 저장한다")
    void createQuestion_description_저장() {
        Form form = Form.createDraft("지원서", 10L, true);
        FormSection section = FormSection.create(form, "공통", null, 1L);
        ReflectionTestUtils.setField(section, "id", 20L);
        given(loadFormSectionPort.findById(20L)).willReturn(Optional.of(section));
        given(loadQuestionPort.listBySectionId(20L)).willReturn(List.of());
        given(saveQuestionPort.save(any(Question.class))).willAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            ReflectionTestUtils.setField(question, "id", 30L);
            return question;
        });

        Long result = sut.createQuestion(CreateQuestionCommand.builder()
            .sectionId(20L)
            .requesterMemberId(99L)
            .type(QuestionType.SHORT_TEXT)
            .title("자기소개")
            .description("자기소개를 입력해주세요")
            .isRequired(true)
            .build());

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        then(saveQuestionPort).should().save(captor.capture());
        assertThat(result).isEqualTo(30L);
        assertThat(captor.getValue().getDescription()).isEqualTo("자기소개를 입력해주세요");
    }
}
