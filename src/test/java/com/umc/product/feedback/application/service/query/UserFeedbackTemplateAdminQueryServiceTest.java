package com.umc.product.feedback.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateDetailInfo;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateSummaryInfo;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class UserFeedbackTemplateAdminQueryServiceTest {

    private static final Long TEMPLATE_ID = 1L;
    private static final Long FORM_ID = 10L;

    @Mock
    LoadUserFeedbackTemplatePort loadTemplatePort;

    @Mock
    GetFormUseCase getFormUseCase;

    @InjectMocks
    UserFeedbackTemplateAdminQueryService sut;

    @Test
    @DisplayName("상세 조회는 폼 구조를 한 번 조회한다")
    void 상세_조회는_폼_구조를_한_번_조회한다() {
        // given
        UserFeedbackTemplate template = template(TEMPLATE_ID, FORM_ID);
        FormWithStructureInfo form = FormWithStructureInfo.builder()
            .formId(FORM_ID)
            .title("상세 폼")
            .sections(List.of())
            .build();
        given(loadTemplatePort.getById(TEMPLATE_ID)).willReturn(template);
        given(getFormUseCase.getFormWithStructure(FORM_ID)).willReturn(form);

        // when
        UserFeedbackTemplateDetailInfo result = sut.getTemplate(TEMPLATE_ID);

        // then
        assertThat(result.form()).isSameAs(form);
        verify(getFormUseCase, times(1)).getFormWithStructure(FORM_ID);
    }

    @Test
    @DisplayName("여러 템플릿은 폼 메타데이터를 한 번 배치 조회하고 저장소 순서로 반환한다")
    void 여러_템플릿은_폼_메타데이터를_한_번_배치_조회하고_저장소_순서로_반환한다() {
        // given
        UserFeedbackTemplate firstTemplate = template(1L, 101L);
        UserFeedbackTemplate secondTemplate = template(2L, 202L);
        given(loadTemplatePort.listByCondition(null, null, null))
            .willReturn(List.of(firstTemplate, secondTemplate));

        Map<Long, FormInfo> formsById = new LinkedHashMap<>();
        formsById.put(202L, form(202L, "두 번째 폼"));
        formsById.put(101L, form(101L, "첫 번째 폼"));
        given(getFormUseCase.batchGetByIds(List.of(101L, 202L))).willReturn(formsById);

        // when
        List<UserFeedbackTemplateSummaryInfo> result = sut.listTemplates(null, null, null);

        // then
        assertThat(result)
            .extracting(UserFeedbackTemplateSummaryInfo::templateId)
            .containsExactly(1L, 2L);
        assertThat(result)
            .extracting(UserFeedbackTemplateSummaryInfo::title)
            .containsExactly("첫 번째 폼", "두 번째 폼");
        verify(getFormUseCase, times(1)).batchGetByIds(List.of(101L, 202L));
        verify(getFormUseCase, never()).getById(any(Long.class));
    }

    @Test
    @DisplayName("템플릿이 없으면 빈 목록을 반환하고 폼을 조회하지 않는다")
    void 템플릿이_없으면_빈_목록을_반환하고_폼을_조회하지_않는다() {
        // given
        given(loadTemplatePort.listByCondition(null, null, null)).willReturn(List.of());

        // when
        List<UserFeedbackTemplateSummaryInfo> result = sut.listTemplates(null, null, null);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(getFormUseCase);
    }

    @Test
    @DisplayName("폼이 없으면 FORM_NOT_FOUND를 전파하고 목록을 부분 반환하지 않는다")
    void 폼이_없으면_FORM_NOT_FOUND를_전파하고_목록을_부분_반환하지_않는다() {
        // given
        given(loadTemplatePort.listByCondition(null, null, null))
            .willReturn(List.of(template(1L, 101L), template(2L, 202L)));
        FormDomainException formNotFound = new FormDomainException(FormErrorCode.FORM_NOT_FOUND);
        given(getFormUseCase.batchGetByIds(List.of(101L, 202L))).willThrow(formNotFound);

        // when & then
        assertThatThrownBy(() -> sut.listTemplates(null, null, null))
            .isSameAs(formNotFound)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_NOT_FOUND);
        verify(getFormUseCase, times(1)).batchGetByIds(List.of(101L, 202L));
        verify(getFormUseCase, never()).getById(any(Long.class));
    }

    private UserFeedbackTemplate template(Long templateId, Long formId) {
        UserFeedbackTemplate template = UserFeedbackTemplate.create(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.ADMIN,
            formId
        );
        ReflectionTestUtils.setField(template, "id", templateId);
        return template;
    }

    private FormInfo form(Long formId, String title) {
        return FormInfo.builder()
            .id(formId)
            .title(title)
            .build();
    }
}
