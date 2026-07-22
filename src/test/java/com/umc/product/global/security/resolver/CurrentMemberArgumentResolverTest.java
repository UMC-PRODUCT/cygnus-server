package com.umc.product.global.security.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;

import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

@DisplayName("CurrentMemberArgumentResolver")
class CurrentMemberArgumentResolverTest {

    @Test
    @DisplayName("@CurrentMember MemberPrincipal 조합만 지원한다")
    void 지원_parameter를_판단한다() throws Exception {
        CurrentMemberArgumentResolver sut = new CurrentMemberArgumentResolver(mock(CurrentMemberProvider.class));

        assertThat(sut.supportsParameter(parameter("valid"))).isTrue();
        assertThat(sut.supportsParameter(parameter("withoutAnnotation"))).isFalse();
        assertThat(sut.supportsParameter(parameter("wrongType"))).isFalse();
    }

    @Test
    @DisplayName("현재 회원 해석은 nullable provider 결과를 그대로 반환한다")
    void 현재_회원을_해석한다() throws Exception {
        CurrentMemberProvider provider = mock(CurrentMemberProvider.class);
        MemberPrincipal principal = new MemberPrincipal(10L);
        given(provider.getNullableCurrentMember()).willReturn(principal);
        CurrentMemberArgumentResolver sut = new CurrentMemberArgumentResolver(provider);

        assertThat(sut.resolveArgument(parameter("valid"), null, null, null)).isSameAs(principal);
    }

    private MethodParameter parameter(String name) throws Exception {
        Method method = Fixture.class.getDeclaredMethod(name, name.equals("wrongType") ? String.class : MemberPrincipal.class);
        return new MethodParameter(method, 0);
    }

    private static final class Fixture {
        void valid(@CurrentMember MemberPrincipal principal) { }
        void withoutAnnotation(MemberPrincipal principal) { }
        void wrongType(@CurrentMember String principal) { }
    }
}
