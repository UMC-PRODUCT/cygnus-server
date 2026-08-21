package com.umc.product.test.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

import com.umc.product.test.adapter.out.persistence.MemberSystemRoleSeedPersistenceAdapter;
import com.umc.product.test.application.service.ChallengerSeedService;
import com.umc.product.test.application.service.CurriculumSeedService;
import com.umc.product.test.application.service.DummyCurriculumFactory;
import com.umc.product.test.application.service.DummyMemberFactory;
import com.umc.product.test.application.service.DummyNoticeFactory;
import com.umc.product.test.application.service.MemberSeedService;
import com.umc.product.test.application.service.MemberSystemRoleSeedService;
import com.umc.product.test.application.service.NoticeSeedService;
import com.umc.product.test.application.service.PartAssignmentPolicy;
import com.umc.product.test.application.service.ProjectApplicationSeedService;
import com.umc.product.test.application.service.ProjectSeedDataCleanupService;
import com.umc.product.test.application.service.ProjectSeedService;
import com.umc.product.test.application.service.SeedProperties;

/**
 * 시딩 관련 빈들의 가드 어노테이션이 회귀 없이 유지되는지 검증한다.
 * <p>
 * prod 환경에서 시딩 API 가 노출되지 않으려면 모든 시딩 빈이
 * {@code @Profile("!prod") + @ConditionalOnProperty("app.seed.enabled" = "true")} 를
 * 동시에 가져야 한다. 이 테스트는 가드가 누락된 클래스를 컴파일 단계와 가까운 시점에 잡아낸다.
 */
class SeedControllerBeanRegistrationTest {

    @ParameterizedTest(name = "{0} 는 시딩 가드 어노테이션을 모두 가진다")
    @ValueSource(classes = {
        SeedController.class,
        MemberSeedService.class,
        MemberSystemRoleSeedService.class,
        MemberSystemRoleSeedPersistenceAdapter.class,
        ChallengerSeedService.class,
        ProjectSeedService.class,
        ProjectSeedDataCleanupService.class,
        ProjectApplicationSeedService.class,
        CurriculumSeedService.class,
        NoticeSeedService.class,
        DummyMemberFactory.class,
        DummyCurriculumFactory.class,
        DummyNoticeFactory.class,
        PartAssignmentPolicy.class,
        SeedProperties.class
    })
    @DisplayName("모든 시딩 빈은 @Profile(!prod) + @ConditionalOnProperty(app.seed.enabled=true) 가드를 가진다")
    void seedBeanGuardAnnotationsExist(Class<?> seedBeanClass) {
        Profile profile = seedBeanClass.getAnnotation(Profile.class);
        assertThat(profile)
            .as("%s 는 @Profile 가드가 있어야 한다", seedBeanClass.getSimpleName())
            .isNotNull();
        assertThat(profile.value())
            .as("%s 의 @Profile 은 !prod 를 포함해야 한다", seedBeanClass.getSimpleName())
            .contains("!prod");

        ConditionalOnProperty property = seedBeanClass.getAnnotation(ConditionalOnProperty.class);
        assertThat(property)
            .as("%s 는 @ConditionalOnProperty 가드가 있어야 한다", seedBeanClass.getSimpleName())
            .isNotNull();
        assertThat(property.prefix()).isEqualTo("app.seed");
        assertThat(property.name()).contains("enabled");
        assertThat(property.havingValue()).isEqualTo("true");
    }
}
