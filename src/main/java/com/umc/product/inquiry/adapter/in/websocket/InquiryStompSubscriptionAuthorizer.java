package com.umc.product.inquiry.adapter.in.websocket;

import org.springframework.stereotype.Component;

import com.umc.product.global.websocket.application.port.in.StompSubscriptionAuthorizer;
import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.exception.InquiryDomainException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code /topic/inquiry/{inquiryId}} 구독 인가 어댑터.
 *
 * <p>공통 {@link com.umc.product.global.websocket.application.service.StompSubscriptionAuthorizerRegistry}가
 * SUBSCRIBE 프레임을 위임하면, destination에서 inquiryId를 파싱한 뒤 다음 조건을 검증한다.
 * <ol>
 *     <li>문의가 실제로 존재한다.</li>
 *     <li>요청 멤버가 작성자이거나 해당 문의 대상의 운영진이다.</li>
 * </ol>
 *
 * <p>파싱 실패나 문의 미존재, 권한 없음은 모두 {@code false}를 반환한다(호출자가 fail-closed 처리).
 *
 * @see com.umc.product.global.websocket.interceptor.StompAuthChannelInterceptor
 * @see <a href="file:../../../../../../../../../../docs/adr/011-inquiry-domain-with-websocket-stomp.md">ADR-011 §2</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InquiryStompSubscriptionAuthorizer implements StompSubscriptionAuthorizer {

    // ADR-011 §2 확정 destination: /topic/inquiry/{inquiryId}
    private static final String DESTINATION_PREFIX = "/topic/inquiry/";

    private final LoadInquiryPort loadInquiryPort;
    private final LoadOperatorStatusPort loadOperatorStatusPort;

    @Override
    public boolean supports(String destination) {
        if (destination == null) {
            return false;
        }
        if (!destination.startsWith(DESTINATION_PREFIX)) {
            return false;
        }
        // prefix 이후에 숫자 이외의 경로 세그먼트가 붙은 경우(/topic/inquiry/1/extra 등)는 이 authorizer 소유가 아님
        String suffix = destination.substring(DESTINATION_PREFIX.length());
        return !suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit);
    }

    @Override
    public boolean isAuthorized(Long memberId, String destination) {
        Long inquiryId = parseInquiryId(destination);
        if (inquiryId == null) {
            return false;
        }

        try {
            Inquiry inquiry = loadInquiryPort.getById(inquiryId);
            boolean isOperator = loadOperatorStatusPort.isOperator(
                LoadOperatorStatusContext.of(memberId, inquiry));
            return inquiry.isAccessibleBy(memberId, isOperator);
        } catch (InquiryDomainException e) {
            log.debug("STOMP 구독 인가 실패 — 문의 조회 불가: inquiryId={}, memberId={}", inquiryId, memberId);
            return false;
        }
    }

    /**
     * destination에서 inquiryId를 파싱한다.
     *
     * @return 파싱 성공 시 Long inquiryId, 실패 시 null
     */
    private Long parseInquiryId(String destination) {
        try {
            String suffix = destination.substring(DESTINATION_PREFIX.length());
            return Long.parseLong(suffix);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
