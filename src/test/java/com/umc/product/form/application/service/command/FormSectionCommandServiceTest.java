package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.dto.ReorderFormSectionsCommand;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class FormSectionCommandServiceTest {

    private static final Long FORM_ID = 100L;
    private static final Long MEMBER_ID = 500L;

    @Mock
    LoadFormPort loadFormPort;
    @Mock
    LoadFormSectionPort loadFormSectionPort;
    @Mock
    LoadQuestionPort loadQuestionPort;
    @Mock
    LoadQuestionOptionPort loadQuestionOptionPort;
    @Mock
    SaveFormSectionPort saveFormSectionPort;
    @Mock
    SaveQuestionPort saveQuestionPort;
    @Mock
    SaveQuestionOptionPort saveQuestionOptionPort;

    @InjectMocks
    FormSectionCommandService sut;

    @Test
    @DisplayName("reorderSections: 재배치 결과 옵션의 nextSectionId 가 back-edge 가 되면 거부")
    void reorderSections_backEdge_거부() {
        // S1 → S3 forward edge, 재배치로 S3 이 앞으로 가면 back-edge 됨
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);
        FormSection s3 = section(3L, 3L);
        Question q = radioQuestion(10L, s1);
        QuestionOption opt = optionWithNextSection(100L, q, s3.getId());

        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2, s3));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(q));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(10L))).willReturn(List.of(opt));

        // 재배치: S3 을 맨 앞으로 (S3, S2, S1)
        assertThatThrownBy(() -> sut.reorderSections(ReorderFormSectionsCommand.builder()
            .formId(FORM_ID)
            .requesterMemberId(MEMBER_ID)
            .orderedSectionIds(List.of(3L, 2L, 1L))
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.INVALID_NEXT_SECTION_BACKWARD);

        then(saveFormSectionPort).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("reorderSections: forward-only 유지되는 재배치는 성공")
    void reorderSections_forwardOnly_유지_성공() {
        // S1 → S3 forward edge, 재배치가 여전히 S1 이 S3 보다 앞이면 통과
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);
        FormSection s3 = section(3L, 3L);
        Question q = radioQuestion(10L, s1);
        QuestionOption opt = optionWithNextSection(100L, q, s3.getId());

        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2, s3));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(q));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(10L))).willReturn(List.of(opt));

        // 재배치: S2 만 뒤로 (S1, S3, S2) — S1 → S3 은 여전히 forward
        sut.reorderSections(ReorderFormSectionsCommand.builder()
            .formId(FORM_ID)
            .requesterMemberId(MEMBER_ID)
            .orderedSectionIds(List.of(1L, 3L, 2L))
            .build());

        then(saveFormSectionPort).should().saveAll(any());
    }

    @Test
    @DisplayName("reorderSections: nextSectionId 옵션 없으면 그래프 검증 통과")
    void reorderSections_nextSectionId_없으면_통과() {
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);

        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of());

        sut.reorderSections(ReorderFormSectionsCommand.builder()
            .formId(FORM_ID)
            .requesterMemberId(MEMBER_ID)
            .orderedSectionIds(List.of(2L, 1L))
            .build());

        then(saveFormSectionPort).should().saveAll(any());
        then(loadQuestionOptionPort).should(never()).listByQuestionIdIn(anySet());
    }

    @Test
    @DisplayName("reorderSections: 재배치 요청의 섹션 ID 셋이 실제와 다르면 거부")
    void reorderSections_섹션ID셋_불일치_거부() {
        FormSection s1 = section(1L, 1L);
        FormSection s2 = section(2L, 2L);
        given(loadFormSectionPort.listByFormId(FORM_ID)).willReturn(List.of(s1, s2));

        assertThatThrownBy(() -> sut.reorderSections(ReorderFormSectionsCommand.builder()
            .formId(FORM_ID)
            .requesterMemberId(MEMBER_ID)
            .orderedSectionIds(List.of(1L, 999L))
            .build()))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);
    }

    private FormSection section(Long id, Long orderNo) {
        Form form = Form.createDraft("폼", 1L, false);
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        FormSection section = FormSection.create(form, "섹션 " + id, null, orderNo);
        ReflectionTestUtils.setField(section, "id", id);
        return section;
    }

    private Question radioQuestion(Long id, FormSection section) {
        Question q = Question.create("Q" + id, QuestionType.RADIO, false, 1L);
        q.assignTo(section);
        ReflectionTestUtils.setField(q, "id", id);
        return q;
    }

    private QuestionOption optionWithNextSection(Long id, Question question, Long nextSectionId) {
        QuestionOption opt = QuestionOption.create("옵션 " + id, 1L, false, nextSectionId);
        ReflectionTestUtils.setField(opt, "id", id);
        ReflectionTestUtils.setField(opt, "question", question);
        return opt;
    }
}
