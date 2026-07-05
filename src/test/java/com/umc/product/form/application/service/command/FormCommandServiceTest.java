package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.SaveAnswerPort;
import com.umc.product.form.application.port.out.SaveFormPort;
import com.umc.product.form.application.port.out.SaveFormResponsePort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.domain.Form;

@ExtendWith(MockitoExtension.class)
class FormCommandServiceTest {

    @Mock
    LoadFormPort loadFormPort;
    @Mock
    SaveFormPort saveFormPort;
    @Mock
    SaveFormSectionPort saveFormSectionPort;
    @Mock
    SaveQuestionPort saveQuestionPort;
    @Mock
    SaveQuestionOptionPort saveQuestionOptionPort;
    @Mock
    SaveFormResponsePort saveFormResponsePort;
    @Mock
    SaveAnswerPort saveAnswerPort;

    @InjectMocks
    FormCommandService sut;

    @Test
    @DisplayName("createDraft는 요청 description을 신규 폼에 저장한다")
    void createDraft_description_저장() {
        given(saveFormPort.save(any(Form.class))).willAnswer(invocation -> {
            Form form = invocation.getArgument(0);
            ReflectionTestUtils.setField(form, "id", 1L);
            return form;
        });

        Long result = sut.createDraft(CreateDraftFormCommand.builder()
            .createdMemberId(10L)
            .title("지원서")
            .description("지원 폼 설명")
            .allowDuplicateResponses(true)
            .build());

        ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
        then(saveFormPort).should().save(captor.capture());
        assertThat(result).isEqualTo(1L);
        assertThat(captor.getValue().getDescription()).isEqualTo("지원 폼 설명");
    }
}
