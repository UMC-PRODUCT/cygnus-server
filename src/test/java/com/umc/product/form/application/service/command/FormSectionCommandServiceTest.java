package com.umc.product.form.application.service.command;

import static com.umc.product.form.application.service.FormAccessTestFixtures.actor;
import static com.umc.product.form.application.service.FormAccessTestFixtures.owner;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.dto.UpdateFormSectionCommand;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class FormSectionCommandServiceTest {

    @Mock
    LoadFormPort loadFormPort;
    @Mock
    LoadFormSectionPort loadFormSectionPort;
    @Mock
    SaveFormSectionPort saveFormSectionPort;
    @Mock
    SaveQuestionPort saveQuestionPort;
    @Mock
    SaveQuestionOptionPort saveQuestionOptionPort;
    @Mock
    FormOwnershipAccessService ownershipAccessService;

    @InjectMocks
    FormSectionCommandService sut;

    @Test
    @DisplayName("foreign section 수정은 실제 parent form ownership 경계에서 거부한다")
    void foreign_section_수정은_parent_form_ownership으로_거부한다() {
        Form form = Form.createDraft("폼", 10L);
        ReflectionTestUtils.setField(form, "id", 100L);
        FormSection section = FormSection.create(form, "섹션", null, 1L);
        ReflectionTestUtils.setField(section, "id", 20L);
        given(loadFormSectionPort.findById(20L)).willReturn(Optional.of(section));
        willThrow(new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN))
            .given(ownershipAccessService)
            .requireMutation(100L, owner(999L), actor(30L), FormOperation.MANAGE_STRUCTURE);

        assertThatThrownBy(() -> sut.updateSection(
            owner(999L),
            actor(30L),
            UpdateFormSectionCommand.builder().sectionId(20L).title("변경").build()
        ))
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);

        then(saveFormSectionPort).should(never()).save(section);
    }
}
