package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.form.application.port.in.command.dto.CloseFormCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.application.port.in.command.dto.UnpublishFormCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormCommand;
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
@DisplayName("FormCommandService 잔여 경계")
class FormCommandServiceResidualTest {

    @Mock LoadFormPort loadFormPort;
    @Mock LoadFormResponsePort loadFormResponsePort;
    @Mock SaveFormPort saveFormPort;
    @Mock SaveFormSectionPort saveFormSectionPort;
    @Mock SaveQuestionPort saveQuestionPort;
    @Mock SaveQuestionOptionPort saveQuestionOptionPort;
    @Mock SaveFormResponsePort saveFormResponsePort;
    @Mock SaveAnswerPort saveAnswerPort;

    FormCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new FormCommandService(
            loadFormPort, loadFormResponsePort, saveFormPort, saveFormSectionPort,
            saveQuestionPort, saveQuestionOptionPort, saveFormResponsePort, saveAnswerPort
        );
    }

    @Test
    @DisplayName("폼 PATCH는 clearDescription과 중복 응답 정책을 적용하고 저장한다")
    void updates_form_metadata() {
        Form form = Form.createDraft("폼", 1L, "설명", false);
        given(loadFormPort.findById(1L)).willReturn(Optional.of(form));

        sut.updateForm(UpdateFormCommand.builder()
            .formId(1L).title("변경").clearDescription(true)
            .isAnonymous(true).allowDuplicateResponses(true).build());

        assertThat(form.getTitle()).isEqualTo("변경");
        assertThat(form.getDescription()).isNull();
        assertThat(form.isAnonymous()).isTrue();
        assertThat(form.isAllowDuplicateResponses()).isTrue();
        then(saveFormPort).should().save(form);
    }

    @Test
    @DisplayName("폼 발행은 DRAFT를 PUBLISHED로 변경해 저장한다")
    void publishes_form() {
        Form form = Form.createDraft("폼", 1L);
        given(loadFormPort.findById(1L)).willReturn(Optional.of(form));

        sut.publishForm(PublishFormCommand.builder().formId(1L).build());

        assertThat(form.getStatus()).isEqualTo(FormStatus.PUBLISHED);
        then(saveFormPort).should().save(form);
    }

    @Test
    @DisplayName("수정·발행·발행취소·종료 대상이 없으면 동일한 FORM_NOT_FOUND를 반환한다")
    void missing_form_paths() {
        given(loadFormPort.findById(404L)).willReturn(Optional.empty());

        assertError(() -> sut.updateForm(UpdateFormCommand.builder().formId(404L).build()));
        assertError(() -> sut.publishForm(PublishFormCommand.builder().formId(404L).build()));
        assertError(() -> sut.unpublishForm(UnpublishFormCommand.builder().formId(404L).build()));
        assertError(() -> sut.closeForm(CloseFormCommand.builder().formId(404L).build()));
    }

    @Test
    @DisplayName("폼 삭제는 응답 트리와 구조를 자식부터 제거한 뒤 폼을 삭제한다")
    void deletes_full_form_tree_in_dependency_order() {
        sut.deleteForm(DeleteFormCommand.builder().formId(1L).build());

        var order = inOrder(
            saveAnswerPort, saveFormResponsePort, saveQuestionOptionPort,
            saveQuestionPort, saveFormSectionPort, saveFormPort
        );
        order.verify(saveAnswerPort).deleteByFormId(1L);
        order.verify(saveFormResponsePort).deleteByFormId(1L);
        order.verify(saveQuestionOptionPort).deleteByFormId(1L);
        order.verify(saveQuestionPort).deleteByFormId(1L);
        order.verify(saveFormSectionPort).deleteByFormId(1L);
        order.verify(saveFormPort).deleteById(1L);
    }

    private void assertError(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(FormErrorCode.FORM_NOT_FOUND));
    }
}
