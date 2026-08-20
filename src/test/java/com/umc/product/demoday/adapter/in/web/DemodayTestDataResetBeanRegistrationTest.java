package com.umc.product.demoday.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

import com.umc.product.demoday.adapter.out.persistence.DemodayTestDataCleanupPersistenceAdapter;
import com.umc.product.demoday.application.service.command.ResetDemodayTestDataCommandService;

@DisplayName("데모데이 테스트 데이터 초기화 빈 등록 조건")
class DemodayTestDataResetBeanRegistrationTest {

    @ParameterizedTest(name = "{0}은 운영 환경 제외 및 활성화 속성 가드를 가진다")
    @ValueSource(classes = {
        DemodayTestDataAdminController.class,
        ResetDemodayTestDataCommandService.class,
        DemodayTestDataCleanupPersistenceAdapter.class
    })
    @DisplayName("모든 초기화 빈은 @Profile(!prod)와 enabled=true 가드를 함께 가진다")
    void resetBeanGuardAnnotationsExist(Class<?> resetBeanClass) {
        Profile profile = resetBeanClass.getAnnotation(Profile.class);
        assertThat(profile)
            .as("%s는 @Profile 가드가 있어야 한다", resetBeanClass.getSimpleName())
            .isNotNull();
        assertThat(profile.value())
            .as("%s의 @Profile은 !prod를 포함해야 한다", resetBeanClass.getSimpleName())
            .contains("!prod");

        ConditionalOnProperty property =
            resetBeanClass.getAnnotation(ConditionalOnProperty.class);
        assertThat(property)
            .as("%s는 @ConditionalOnProperty 가드가 있어야 한다", resetBeanClass.getSimpleName())
            .isNotNull();
        assertThat(property.prefix()).isEqualTo("demoday.test-data-reset");
        assertThat(property.name()).containsExactly("enabled");
        assertThat(property.havingValue()).isEqualTo("true");
        assertThat(property.matchIfMissing()).isFalse();
    }
}
