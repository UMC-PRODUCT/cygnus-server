package com.umc.product.demoday.adapter.in.web.support;

import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.umc.product.demoday.adapter.in.web.security.CurrentDemodayParticipant;
import com.umc.product.demoday.adapter.in.web.security.DemodayParticipationPrincipal;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantResolver;
import com.umc.product.global.security.MemberPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * SecurityContext의 principal 타입(MemberPrincipal / DemodayParticipationPrincipal)을 보고
 * 알맞은 {@link DemodayParticipantResolver}로 위임한다. 두 principal은 서로 무관한 타입이다.
 * 기존 회원 인증 코어(MemberPrincipal)를 게스트가 공유하도록 고치는 대신 완전히 별도 타입으로
 * 분리했기 때문에, 컨트롤러 파라미터 슬롯 하나로 받으려면 이 디스패치가 필요하다.
 * <p>
 * {@code @Component}가 아니라 {@link DemodayParticipantResolverConfig}의 {@code @Bean}으로만
 * 등록한다. 이 클래스는 {@code HandlerMethodArgumentResolver}를 구현하는데, 이 인터페이스는
 * {@code @WebMvcTest} 슬라이스가 자동으로 스캔해 포함시키는 타입이라, {@code @Component}로 두면
 * 무관한 슬라이스 테스트에서도 이 빈이 즉시 생성되면서 생성자 의존성(양쪽 {@link DemodayParticipantResolver}) 부재로 컨텍스트 로딩이 깨진다.
 * {@code MaintenanceFilter}·{@code DemodayParticipationCookieFilter}와 같은 이유다.
 */
@RequiredArgsConstructor
public class CurrentDemodayParticipantArgumentResolver implements HandlerMethodArgumentResolver {

    private final DemodayParticipantResolver<MemberPrincipal> memberDemodayParticipantResolver;
    private final DemodayParticipantResolver<DemodayParticipationPrincipal> guestDemodayParticipantResolver;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(CurrentDemodayParticipant.class);
        boolean hasParticipantType = DemodayParticipant.class.isAssignableFrom(parameter.getParameterType());

        return hasAnnotation && hasParticipantType;
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (principal instanceof MemberPrincipal memberPrincipal) {
            return memberDemodayParticipantResolver.resolve(memberPrincipal);
        }
        if (principal instanceof DemodayParticipationPrincipal guestPrincipal) {
            return guestDemodayParticipantResolver.resolve(guestPrincipal);
        }

        throw new AccessDeniedException("로그인 또는 게스트 참여 인증이 필요해요.");
    }
}
