package com.umc.product.feedback.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateOwnershipUseCase;
import com.umc.product.feedback.application.port.in.query.dto.FeedbackTemplateOwnershipInfo;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;

@DisplayName("FeedbackTemplateOwnerPolicy")
class FeedbackTemplateOwnerPolicyTest {

    private static final Long TEMPLATE_ID = 7L;
    private static final Long FORM_ID = 42L;
    private static final FormActorContext ACTOR = FormActorContext.authenticated(100L);

    private GetUserFeedbackTemplateOwnershipUseCase ownershipQuery;
    private FeedbackTemplateOwnerPolicy sut;

    @BeforeEach
    void setUp() {
        ownershipQuery = mock(GetUserFeedbackTemplateOwnershipUseCase.class);
        sut = new FeedbackTemplateOwnerPolicy(
            ownershipQuery,
            new FeedbackTemplateOwnerReferenceFactory()
        );
    }

    @Test
    @DisplayName("활성 템플릿의 canonical owner는 인증 actor의 READ와 RESPOND를 허용한다")
    void allowsActiveTemplateReadAndRespond() {
        FormOwnerReference owner = owner();
        given(ownershipQuery.findOwnershipById(TEMPLATE_ID))
            .willReturn(Optional.of(info(TEMPLATE_ID, FORM_ID, true)));

        assertThat(sut.allows(owner, FormOperation.READ, ACTOR)).isTrue();
        assertThat(sut.allows(owner, FormOperation.RESPOND, ACTOR)).isTrue();
    }

    @Test
    @DisplayName("anonymous actor와 관리 operation은 feedback policy에서 거부한다")
    void deniesAnonymousAndManagementOperations() {
        FormOwnerReference owner = owner();
        given(ownershipQuery.findOwnershipById(TEMPLATE_ID))
            .willReturn(Optional.of(info(TEMPLATE_ID, FORM_ID, true)));

        assertThat(sut.allows(owner, FormOperation.READ, FormActorContext.anonymous())).isFalse();
        assertThat(sut.allows(owner, FormOperation.RESPOND, FormActorContext.anonymous())).isFalse();
        assertThat(sut.allows(owner, FormOperation.MANAGE_STRUCTURE, ACTOR)).isFalse();
        assertThat(sut.allows(owner, FormOperation.READ_RESPONSES, ACTOR)).isFalse();
    }

    @Test
    @DisplayName("owner tuple 또는 loaded template scalar가 다르면 fail-closed한다")
    void deniesWrongOwnerAndLoadedScalarMismatch() {
        FormOwnerReference wrongFormOwner = FormOwnerReference.of(
            99L,
            FeedbackTemplateOwnerReferenceFactory.NAMESPACE,
            TEMPLATE_ID.toString(),
            FeedbackTemplateOwnerReferenceFactory.SLOT
        );
        given(ownershipQuery.findOwnershipById(TEMPLATE_ID))
            .willReturn(Optional.of(info(TEMPLATE_ID, FORM_ID, true)));

        assertThat(sut.allows(wrongFormOwner, FormOperation.READ, ACTOR)).isFalse();

        FormOwnerReference owner = owner();
        given(ownershipQuery.findOwnershipById(TEMPLATE_ID))
            .willReturn(Optional.of(info(999L, FORM_ID, true)));
        assertThat(sut.allows(owner, FormOperation.READ, ACTOR)).isFalse();

        given(ownershipQuery.findOwnershipById(TEMPLATE_ID))
            .willReturn(Optional.of(info(TEMPLATE_ID, 999L, true)));
        assertThat(sut.allows(owner, FormOperation.READ, ACTOR)).isFalse();
    }

    @Test
    @DisplayName("malformed owner key 또는 inactive/missing template은 query 전후 모두 거부한다")
    void deniesMalformedInactiveAndMissingTemplate() {
        FormOwnerReference malformed = FormOwnerReference.of(
            FORM_ID,
            FeedbackTemplateOwnerReferenceFactory.NAMESPACE,
            "template-7",
            FeedbackTemplateOwnerReferenceFactory.SLOT
        );
        assertThat(sut.allows(malformed, FormOperation.READ, ACTOR)).isFalse();
        verify(ownershipQuery, never()).findOwnershipById(TEMPLATE_ID);

        FormOwnerReference owner = owner();
        given(ownershipQuery.findOwnershipById(TEMPLATE_ID)).willReturn(Optional.empty());
        assertThat(sut.allows(owner, FormOperation.READ, ACTOR)).isFalse();

        given(ownershipQuery.findOwnershipById(TEMPLATE_ID))
            .willReturn(Optional.of(info(TEMPLATE_ID, FORM_ID, false)));
        assertThat(sut.allows(owner, FormOperation.READ, ACTOR)).isFalse();
    }

    @Test
    @DisplayName("consumer ownership query 예외도 Form policy 경계에서 false로 수렴한다")
    void deniesWhenOwnershipQueryFails() {
        given(ownershipQuery.findOwnershipById(TEMPLATE_ID))
            .willThrow(new IllegalStateException("query unavailable"));

        assertThat(sut.allows(owner(), FormOperation.READ, ACTOR)).isFalse();
    }

    private FormOwnerReference owner() {
        return new FeedbackTemplateOwnerReferenceFactory().create(TEMPLATE_ID, FORM_ID);
    }

    private FeedbackTemplateOwnershipInfo info(Long templateId, Long formId, boolean active) {
        return FeedbackTemplateOwnershipInfo.builder()
            .templateId(templateId)
            .formId(formId)
            .active(active)
            .build();
    }
}
