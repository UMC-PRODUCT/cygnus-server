package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.dto.CloseFormCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.UnpublishFormCommand;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.port.out.SaveAnswerPort;
import com.umc.product.form.application.port.out.SaveFormPort;
import com.umc.product.form.application.port.out.SaveFormResponsePort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class FormCommandServiceTest {

    @Mock
    LoadFormPort loadFormPort;
    @Mock
    LoadFormResponsePort loadFormResponsePort;
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

    @Test
    @DisplayName("응답이 없는 게시 Form은 DRAFT로 되돌린다")
    void unpublishFormWithoutResponses() {
        Form form = publishedForm();
        given(loadFormPort.findById(1L)).willReturn(Optional.of(form));

        sut.unpublishForm(UnpublishFormCommand.builder().formId(1L).requesterMemberId(10L).build());

        assertThat(form.getStatus()).isEqualTo(FormStatus.DRAFT);
        then(saveFormPort).should().save(form);
    }

    @Test
    @DisplayName("응답이 있는 게시 Form은 DRAFT로 되돌릴 수 없다")
    void rejectUnpublishFormWithResponses() {
        Form form = publishedForm();
        given(loadFormPort.findById(1L)).willReturn(Optional.of(form));
        given(loadFormResponsePort.existsByFormId(1L)).willReturn(true);

        assertThatThrownBy(() -> sut.unpublishForm(UnpublishFormCommand.builder().formId(1L).build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_HAS_RESPONSES);
    }

    @Test
    @DisplayName("게시 Form을 CLOSED로 종료한다")
    void closePublishedForm() {
        Form form = publishedForm();
        given(loadFormPort.findById(1L)).willReturn(Optional.of(form));

        sut.closeForm(CloseFormCommand.builder().formId(1L).build());

        assertThat(form.getStatus()).isEqualTo(FormStatus.CLOSED);
        then(saveFormPort).should().save(form);
    }

    private Form publishedForm() {
        Form form = Form.createDraft("지원서", 10L);
        ReflectionTestUtils.setField(form, "id", 1L);
        form.publish();
        return form;
    }
}
