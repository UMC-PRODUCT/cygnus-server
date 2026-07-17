package com.umc.product.form.application.service.command;

import static com.umc.product.form.application.service.FormAccessTestFixtures.actor;
import static com.umc.product.form.application.service.FormAccessTestFixtures.owner;
import static com.umc.product.form.application.service.FormAccessTestFixtures.ownerFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormCommand;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.SaveAnswerPort;
import com.umc.product.form.application.port.out.SaveFormPort;
import com.umc.product.form.application.port.out.SaveFormResponsePort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.application.service.FormAnswerAttachmentUsageService;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;

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
    @Mock
    FormOwnershipAccessService ownershipAccessService;
    @Mock
    FormAnswerAttachmentUsageService attachmentUsageService;

    @InjectMocks
    FormCommandService sut;

    @Test
    @DisplayName("createDraft는 요청 description을 신규 폼에 저장한다")
    void createDraft_description_저장() {
        FormOwnerReferenceFactory factory = ownerFactory();
        given(saveFormPort.save(any(Form.class))).willAnswer(invocation -> {
            Form form = invocation.getArgument(0);
            ReflectionTestUtils.setField(form, "id", 1L);
            return form;
        });

        Long result = sut.createDraft(factory, actor(10L), CreateDraftFormCommand.builder()
            .title("지원서")
            .description("지원 폼 설명")
            .allowDuplicateResponses(true)
            .build());

        ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
        then(saveFormPort).should().save(captor.capture());
        then(ownershipAccessService).should().registerNewForm(
            1L, factory, actor(10L), FormOperation.MANAGE_STRUCTURE
        );
        assertThat(result).isEqualTo(1L);
        assertThat(captor.getValue().getDescription()).isEqualTo("지원 폼 설명");
    }

    @Test
    @DisplayName("폼 삭제는 Answer attachment usage를 먼저 batch detach한다")
    void deleteForm_detachesAnswerUsageBeforeDelete() {
        sut.deleteForm(owner(1L), actor(10L), DeleteFormCommand.builder().formId(1L).build());

        InOrder order = Mockito.inOrder(attachmentUsageService, saveAnswerPort, saveFormPort);
        order.verify(attachmentUsageService).detachByFormId(1L);
        order.verify(saveAnswerPort).deleteByFormId(1L);
        order.verify(saveFormPort).deleteById(1L);
    }
}
