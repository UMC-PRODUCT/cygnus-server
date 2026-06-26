package com.umc.product.inquiry.adapter.out.authorization;

import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import org.springframework.stereotype.Component;

/**
 * 운영진 판정 임시 스텁 구현.
 */
@Component
public class StubLoadOperatorStatusAdapter implements LoadOperatorStatusPort {

    @Override
    public boolean isOperator(LoadOperatorStatusContext context) {
        // TODO: 운영진 판정 미구현. 별도 이슈에서 GetChallengerRoleUseCase 기반으로 교체.
        //  - gisuId 결정(조직 도메인 조회), InquiryTarget↔ChallengerRoleType 매핑, PRODUCT_TEAM 예외 처리.
        //  - 현재는 안전하게 false 고정(아무도 운영진 아님 → 잘못된 상태 전환 방지).
        return false;
    }
}
