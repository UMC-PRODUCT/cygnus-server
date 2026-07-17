package com.umc.product.feedback.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.form.domain.FormOwnerReference;

@DisplayName("FeedbackTemplateOwnerReferenceFactory")
class FeedbackTemplateOwnerReferenceFactoryTest {

    private final FeedbackTemplateOwnerReferenceFactory factory = new FeedbackTemplateOwnerReferenceFactory();

    @Test
    @DisplayName("template ID를 owner key로 사용한 canonical feedback binding을 만든다")
    void createsCanonicalFeedbackBinding() {
        FormOwnerReference reference = factory.create(7L, 42L);

        assertThat(reference).isEqualTo(FormOwnerReference.of(
            42L,
            FeedbackTemplateOwnerReferenceFactory.NAMESPACE,
            "7",
            FeedbackTemplateOwnerReferenceFactory.SLOT
        ));
    }

    @Test
    @DisplayName("template 또는 Form ID가 유효하지 않으면 binding을 만들지 않는다")
    void rejectsMalformedIds() {
        assertThatThrownBy(() -> factory.create(null, 42L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.create(7L, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.create(0L, 42L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
