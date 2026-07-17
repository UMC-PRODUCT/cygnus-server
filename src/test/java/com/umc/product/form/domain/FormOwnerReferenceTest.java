package com.umc.product.form.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FormOwnerReference")
class FormOwnerReferenceTest {

    @Test
    @DisplayName("폼 ID와 owner tuple을 불변 참조로 만든다")
    void 폼_ID와_owner_tuple을_불변_참조로_만든다() {
        FormOwnerReference reference = FormOwnerReference.of(
            10L,
            "project.application-form",
            "10",
            "default"
        );

        assertThat(reference.formId()).isEqualTo(10L);
        assertThat(reference.namespace()).isEqualTo("project.application-form");
        assertThat(reference.ownerResourceKey()).isEqualTo("10");
        assertThat(reference.slot()).isEqualTo("default");
    }

    @Test
    @DisplayName("namespace, resource key, slot grammar 밖의 값은 거부한다")
    void namespace_resource_key_slot_grammar_밖의_값은_거부한다() {
        assertThatIllegalArgumentException().isThrownBy(() -> FormOwnerReference.of(
            10L, "Project.application-form", "10", "default"
        ));
        assertThatIllegalArgumentException().isThrownBy(() -> FormOwnerReference.of(
            10L, "project..application-form", "10", "default"
        ));
        assertThatIllegalArgumentException().isThrownBy(() -> FormOwnerReference.of(
            10L, "project.application-form", "owner/key", "default"
        ));
        assertThatIllegalArgumentException().isThrownBy(() -> FormOwnerReference.of(
            10L, "project.application-form", "10", "Default"
        ));
    }

    @Test
    @DisplayName("동일 폼에 다른 owner를 재바인딩하지 않는다")
    void 동일_폼에_다른_owner를_재바인딩하지_않는다() {
        FormOwnerReference original = FormOwnerReference.of(
            10L, "project.application-form", "10", "default"
        );
        FormOwnership ownership = FormOwnership.from(original);

        assertThat(ownership.matches(original)).isTrue();
        assertThat(ownership.matches(FormOwnerReference.of(
            10L, "project.application-form", "11", "default"
        ))).isFalse();
    }
}
