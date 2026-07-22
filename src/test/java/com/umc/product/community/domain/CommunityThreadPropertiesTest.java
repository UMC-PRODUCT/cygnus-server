package com.umc.product.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

@DisplayName("CommunityThreadProperties")
class CommunityThreadPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("설정이 없으면 최대 멤버 수는 100이다")
    void defaultValue_100을_사용한다() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CommunityThreadProperties.class).maxMembers()).isEqualTo(100);
        });
    }

    @Test
    @DisplayName("최대 멤버 수는 2 이상 100 이하만 허용한다")
    void validation_2에서_100만_허용한다() {
        contextRunner.withPropertyValues("app.community.thread.max-members=1")
            .run(context -> assertThat(context).hasFailed());
        contextRunner.withPropertyValues("app.community.thread.max-members=101")
            .run(context -> assertThat(context).hasFailed());
        contextRunner.withPropertyValues("app.community.thread.max-members=2")
            .run(context -> assertThat(context).hasNotFailed());
        contextRunner.withPropertyValues("app.community.thread.max-members=100")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("비기본 최대값을 생성 초대 팬아웃 경계에 동일하게 적용한다")
    void nonDefaultValue_모든_용량_경계에_적용한다() {
        contextRunner.withPropertyValues("app.community.thread.max-members=3")
            .run(context -> {
                CommunityThreadProperties properties = context.getBean(CommunityThreadProperties.class);

                assertThat(properties.allowsCreateInviteCount(2)).isTrue();
                assertThat(properties.allowsCreateInviteCount(3)).isFalse();
                assertThat(properties.allowsInvite(2, 1)).isTrue();
                assertThat(properties.allowsInvite(2, 2)).isFalse();
                assertThat(properties.allowsFanOutRecipientCount(3)).isTrue();
                assertThat(properties.allowsFanOutRecipientCount(4)).isFalse();
            });
    }

    @Test
    @DisplayName("남은 정원은 0 아래로 내려가지 않고 음수 active count는 거부한다")
    void remainingCapacityIsClamped() {
        CommunityThreadProperties properties = new CommunityThreadProperties(3);

        assertThat(properties.remainingCapacity(1)).isEqualTo(2);
        assertThat(properties.remainingCapacity(4)).isZero();
        assertThatThrownBy(() -> properties.remainingCapacity(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CommunityThreadProperties.class)
    static class TestConfiguration {
    }
}
