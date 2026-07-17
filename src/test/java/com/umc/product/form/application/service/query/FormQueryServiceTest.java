package com.umc.product.form.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;

@ExtendWith(MockitoExtension.class)
class FormQueryServiceTest {

    @Mock
    LoadFormPort loadFormPort;
    @Mock
    LoadFormSectionPort loadFormSectionPort;
    @Mock
    LoadQuestionPort loadQuestionPort;
    @Mock
    LoadQuestionOptionPort loadQuestionOptionPort;

    @InjectMocks
    FormQueryService sut;

    // ============================================================
    //          getFormWithStructureByQuestionIds 테스트
    // ============================================================

    @Test
    @DisplayName("getFormWithStructureByQuestionIds_answeredQuestionIds에_해당하는_질문이_포함됨")
    void getFormWithStructureByQuestionIds_답변된_질문_포함() {
        // given
        Form form = createForm(7L);
        FormSection section = createSection(1L, 7L, 1L);
        Question question = createQuestion(10L, section, 1L);

        given(loadFormPort.findById(7L)).willReturn(java.util.Optional.of(form));
        given(loadQuestionPort.listByIdIn(Set.of(10L))).willReturn(List.of(question));
        given(loadFormSectionPort.listByFormId(7L)).willReturn(List.of(section));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(10L))).willReturn(List.of());

        // when
        FormWithStructureInfo result = sut.getFormWithStructureByQuestionIds(7L, Set.of(10L));

        // then
        assertThat(result.sections()).hasSize(1);
        assertThat(result.sections().get(0).questions()).hasSize(1);
        assertThat(result.sections().get(0).questions().get(0).questionId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getFormWithStructureByQuestionIds_answeredQuestionIds에_없는_질문은_포함되지_않음")
    void getFormWithStructureByQuestionIds_답변_없는_질문_제외() {
        // given — questionId=10만 answeredQuestionIds에 포함, questionId=20은 미포함
        Form form = createForm(7L);
        FormSection section = createSection(1L, 7L, 1L);
        Question question10 = createQuestion(10L, section, 1L);

        given(loadFormPort.findById(7L)).willReturn(java.util.Optional.of(form));
        // listByIdIn은 Set.of(10L)만 받아 question10만 반환
        given(loadQuestionPort.listByIdIn(Set.of(10L))).willReturn(List.of(question10));
        given(loadFormSectionPort.listByFormId(7L)).willReturn(List.of(section));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(10L))).willReturn(List.of());

        // when
        FormWithStructureInfo result = sut.getFormWithStructureByQuestionIds(7L, Set.of(10L));

        // then — questionId=20은 조회 요청 자체가 없었으므로 결과에도 없음
        assertThat(result.sections().get(0).questions())
            .extracting(FormWithStructureInfo.QuestionWithOptions::questionId)
            .containsExactly(10L);
    }

    @Test
    @DisplayName("getFormWithStructureByQuestionIds_answeredQuestionIds가_비어있으면_질문_없는_섹션_구조_반환")
    void getFormWithStructureByQuestionIds_빈_questionIds() {
        // given
        Form form = createForm(7L);
        FormSection section = createSection(1L, 7L, 1L);

        given(loadFormPort.findById(7L)).willReturn(java.util.Optional.of(form));
        given(loadFormSectionPort.listByFormId(7L)).willReturn(List.of(section));

        // when
        FormWithStructureInfo result = sut.getFormWithStructureByQuestionIds(7L, Set.of());

        // then — 섹션은 있지만 질문은 없음
        assertThat(result.sections()).hasSize(1);
        assertThat(result.sections().get(0).questions()).isEmpty();
    }

    @Test
    @DisplayName("getFormWithStructureByQuestionIds_fork된_비활성_질문도_questionIds에_있으면_포함됨")
    void getFormWithStructureByQuestionIds_비활성_질문_포함() {
        // given — isActive=false인 구 질문(fork된 원본)
        Form form = createForm(7L);
        FormSection section = createSection(1L, 7L, 1L);
        Question inactiveQuestion = createQuestion(10L, section, 1L);
        ReflectionTestUtils.setField(inactiveQuestion, "isActive", false);

        given(loadFormPort.findById(7L)).willReturn(java.util.Optional.of(form));
        given(loadQuestionPort.listByIdIn(Set.of(10L))).willReturn(List.of(inactiveQuestion));
        given(loadFormSectionPort.listByFormId(7L)).willReturn(List.of(section));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(10L))).willReturn(List.of());

        // when
        FormWithStructureInfo result = sut.getFormWithStructureByQuestionIds(7L, Set.of(10L));

        // then — isActive=false여도 questionIds에 포함되어 있으면 구조에 포함됨
        assertThat(result.sections().get(0).questions())
            .extracting(FormWithStructureInfo.QuestionWithOptions::questionId)
            .containsExactly(10L);
    }

    // ============================================================
    //          getFormWithStructure 회귀 방지 테스트
    // ============================================================

    @Test
    @DisplayName("getFormWithStructure_isActive_true인_질문만_반환됨")
    void getFormWithStructure_활성_질문만_반환() {
        // given
        Form form = createForm(7L);
        FormSection section = createSection(1L, 7L, 1L);
        // listBySectionIdIn은 isActive=true 필터를 어댑터 레벨에서 적용 — 여기서는 활성 질문만 반환되는 상황 모킹
        Question activeQuestion = createQuestion(20L, section, 1L);

        given(loadFormPort.findById(7L)).willReturn(java.util.Optional.of(form));
        given(loadFormSectionPort.listByFormId(7L)).willReturn(List.of(section));
        given(loadQuestionPort.listBySectionIdIn(Set.of(1L))).willReturn(List.of(activeQuestion));
        given(loadQuestionOptionPort.listByQuestionIdIn(Set.of(20L))).willReturn(List.of());

        // when
        FormWithStructureInfo result = sut.getFormWithStructure(7L);

        // then — 활성 질문만 포함됨 (어댑터가 isActive 필터 책임)
        assertThat(result.sections().get(0).questions())
            .extracting(FormWithStructureInfo.QuestionWithOptions::questionId)
            .containsExactly(20L);
    }

    @Test
    @DisplayName("batchGetByIds_중복_ID는_첫_등장_순서로_제거하고_메타데이터만_반환함")
    void batchGetByIds_중복_ID_첫_등장_순서_메타데이터_반환() {
        // given
        Form form20 = createForm(20L);
        Form form10 = createForm(10L);
        Form form30 = createForm(30L);
        List<Long> requestedIds = List.of(20L, 10L, 20L, 30L, 10L);
        given(loadFormPort.batchGetByIds(List.of(20L, 10L, 30L)))
            .willReturn(List.of(form10, form30, form20));
        GetFormUseCase useCase = sut;

        // when
        Map<Long, FormInfo> result = useCase.batchGetByIds(requestedIds);

        // then
        assertThat(result.keySet()).containsExactly(20L, 10L, 30L);
        assertThat(result.get(20L).id()).isEqualTo(20L);
        assertThat(result.get(10L).id()).isEqualTo(10L);
        assertThat(result.get(30L).id()).isEqualTo(30L);
        verify(loadFormPort, times(1)).batchGetByIds(List.of(20L, 10L, 30L));
        verifyNoInteractions(loadFormSectionPort, loadQuestionPort, loadQuestionOptionPort);
    }

    @Test
    @DisplayName("batchGetByIds_null이면_빈_맵을_반환하고_모든_port를_호출하지_않음")
    void batchGetByIds_null_빈_맵_반환_포트_미호출() {
        // when
        Map<Long, FormInfo> result = sut.batchGetByIds(null);

        // then
        assertThat(result).isEqualTo(Map.of());
        verifyNoInteractions(loadFormPort, loadFormSectionPort, loadQuestionPort, loadQuestionOptionPort);
    }

    @Test
    @DisplayName("batchGetByIds_빈_컬렉션이면_빈_맵을_반환하고_모든_port를_호출하지_않음")
    void batchGetByIds_빈_컬렉션_빈_맵_반환_포트_미호출() {
        // when
        Map<Long, FormInfo> result = sut.batchGetByIds(List.of());

        // then
        assertThat(result).isEqualTo(Map.of());
        verifyNoInteractions(loadFormPort, loadFormSectionPort, loadQuestionPort, loadQuestionOptionPort);
    }

    // ============================================================
    //                      Helper Methods
    // ============================================================

    private static <T> T newInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Form createForm(Long id) {
        Form form = newInstance(Form.class);
        ReflectionTestUtils.setField(form, "id", id);
        ReflectionTestUtils.setField(form, "title", "테스트 폼");
        ReflectionTestUtils.setField(form, "isAnonymous", false);
        ReflectionTestUtils.setField(form, "allowDuplicateResponses", false);
        return form;
    }

    private FormSection createSection(Long id, Long formId, Long orderNo) {
        Form form = newInstance(Form.class);
        ReflectionTestUtils.setField(form, "id", formId);

        FormSection section = newInstance(FormSection.class);
        ReflectionTestUtils.setField(section, "id", id);
        ReflectionTestUtils.setField(section, "form", form);
        ReflectionTestUtils.setField(section, "title", "섹션 " + id);
        ReflectionTestUtils.setField(section, "orderNo", orderNo);
        return section;
    }

    private Question createQuestion(Long id, FormSection section, Long orderNo) {
        Question question = newInstance(Question.class);
        ReflectionTestUtils.setField(question, "id", id);
        ReflectionTestUtils.setField(question, "formSection", section);
        ReflectionTestUtils.setField(question, "title", "질문 " + id);
        ReflectionTestUtils.setField(question, "type", QuestionType.SHORT_TEXT);
        ReflectionTestUtils.setField(question, "isRequired", true);
        ReflectionTestUtils.setField(question, "orderNo", orderNo);
        ReflectionTestUtils.setField(question, "isActive", true);
        return question;
    }
}
