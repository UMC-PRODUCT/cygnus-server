package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.dto.CreateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderFormSectionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormSectionCommand;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormSectionCommandService")
class FormSectionCommandServiceTest {

    @Mock LoadFormPort loadFormPort;
    @Mock LoadFormSectionPort loadFormSectionPort;
    @Mock SaveFormSectionPort saveFormSectionPort;
    @Mock SaveQuestionPort saveQuestionPort;
    @Mock SaveQuestionOptionPort saveQuestionOptionPort;

    FormSectionCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new FormSectionCommandService(
            loadFormPort, loadFormSectionPort, saveFormSectionPort,
            saveQuestionPort, saveQuestionOptionPort
        );
    }

    @Test
    @DisplayName("기존 최대 순서 다음 번호로 섹션을 생성한다")
    void creates_after_max_order() {
        Form form = Form.createDraft("폼", 1L);
        FormSection existing = section(form, 10L, 3L);
        given(loadFormPort.findById(1L)).willReturn(Optional.of(form));
        given(loadFormSectionPort.listByFormId(1L)).willReturn(List.of(existing));
        given(saveFormSectionPort.save(any())).willAnswer(invocation -> {
            FormSection created = invocation.getArgument(0);
            ReflectionTestUtils.setField(created, "id", 20L);
            return created;
        });

        Long id = sut.createSection(CreateFormSectionCommand.builder()
            .formId(1L).title("새 섹션").description("설명").build());

        assertThat(id).isEqualTo(20L);
        then(saveFormSectionPort).should().save(org.mockito.ArgumentMatchers.argThat(
            value -> value.getOrderNo() == 4L && value.getForm() == form));
    }

    @Test
    @DisplayName("첫 섹션은 1번이며 없는 폼은 FORM_NOT_FOUND를 반환한다")
    void first_order_and_missing_form() {
        Form form = Form.createDraft("폼", 1L);
        given(loadFormPort.findById(1L)).willReturn(Optional.of(form));
        given(loadFormSectionPort.listByFormId(1L)).willReturn(List.of());
        given(saveFormSectionPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        sut.createSection(CreateFormSectionCommand.builder().formId(1L).title("첫 섹션").build());
        then(saveFormSectionPort).should().save(org.mockito.ArgumentMatchers.argThat(
            value -> value.getOrderNo() == 1L));

        given(loadFormPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.createSection(
            CreateFormSectionCommand.builder().formId(404L).title("없음").build()),
            FormErrorCode.FORM_NOT_FOUND);
    }

    @Test
    @DisplayName("섹션 수정은 clearDescription을 적용하고 없는 섹션은 예외를 반환한다")
    void updates_or_rejects_missing_section() {
        FormSection section = section(Form.createDraft("폼", 1L), 10L, 1L);
        given(loadFormSectionPort.findById(10L)).willReturn(Optional.of(section));
        sut.updateSection(UpdateFormSectionCommand.builder()
            .sectionId(10L).title("변경").clearDescription(true).build());
        assertThat(section.getTitle()).isEqualTo("변경");
        assertThat(section.getDescription()).isNull();
        then(saveFormSectionPort).should().save(section);

        given(loadFormSectionPort.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.updateSection(
            UpdateFormSectionCommand.builder().sectionId(404L).build()),
            FormErrorCode.FORM_NOT_FOUND);
    }

    @Test
    @DisplayName("섹션 삭제는 선택지→질문→섹션 순서로 cascade한다")
    void deletes_children_before_section() {
        sut.deleteSection(DeleteFormSectionCommand.builder().sectionId(10L).build());

        var order = inOrder(saveQuestionOptionPort, saveQuestionPort, saveFormSectionPort);
        order.verify(saveQuestionOptionPort).deleteBySectionId(10L);
        order.verify(saveQuestionPort).deleteBySectionId(10L);
        order.verify(saveFormSectionPort).deleteById(10L);
    }

    @Test
    @DisplayName("재배치는 입력 순서를 1부터 부여하고 저장한다")
    void reorders_all_sections() {
        Form form = Form.createDraft("폼", 1L);
        FormSection first = section(form, 10L, 1L);
        FormSection second = section(form, 20L, 2L);
        given(loadFormSectionPort.listByFormId(1L)).willReturn(List.of(first, second));

        sut.reorderSections(ReorderFormSectionsCommand.builder()
            .formId(1L).orderedSectionIds(List.of(20L, 10L)).build());

        assertThat(second.getOrderNo()).isEqualTo(1L);
        assertThat(first.getOrderNo()).isEqualTo(2L);
        then(saveFormSectionPort).should().saveAll(List.of(first, second));
    }

    @Test
    @DisplayName("재배치의 누락·중복·외부 ID는 모두 거부한다")
    void rejects_invalid_reorder_sets() {
        Form form = Form.createDraft("폼", 1L);
        given(loadFormSectionPort.listByFormId(1L)).willReturn(List.of(
            section(form, 10L, 1L), section(form, 20L, 2L)
        ));

        for (List<Long> invalid : List.of(List.of(10L), List.of(10L, 10L, 20L), List.of(10L, 30L))) {
            assertError(() -> sut.reorderSections(ReorderFormSectionsCommand.builder()
                .formId(1L).orderedSectionIds(invalid).build()), FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);
        }
    }

    private FormSection section(Form form, Long id, Long orderNo) {
        FormSection section = FormSection.create(form, "섹션" + id, "설명", orderNo);
        ReflectionTestUtils.setField(section, "id", id);
        return section;
    }

    private void assertError(Runnable action, FormErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code));
    }
}
