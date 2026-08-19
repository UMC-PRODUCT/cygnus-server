package com.umc.product.demoday.adapter.in.web.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 회원(Bearer)·게스트(Cookie) 어느 쪽으로 인증됐든 공통 {@code DemodayParticipant}로 받고 싶은
 * 컨트롤러 파라미터에 붙인다. 실제 principal 타입 분기는
 * {@code CurrentDemodayParticipantArgumentResolver}가 담당한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentDemodayParticipant {
}
