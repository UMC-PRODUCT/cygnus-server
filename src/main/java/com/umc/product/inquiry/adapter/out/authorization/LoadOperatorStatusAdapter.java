package com.umc.product.inquiry.adapter.out.authorization;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 운영진 판정 어댑터.
 * <p>
 * 활성 기수를 맥락으로 삼아, 문의 타겟(InquiryTarget)별로 발신자가 해당 조직의 운영진인지 판정한다.
 * <ul>
 *     <li>CENTRAL — 활성 기수에서 중앙운영사무국 멤버 여부</li>
 *     <li>CHAPTER — 활성 기수에서 지부장 여부</li>
 *     <li>SCHOOL  — 문의 작성자(고객)의 소속 학교 기준, 발신자가 그 학교의 운영진인지</li>
 *     <li>PRODUCT_TEAM — 대응 역할 미정(별도 과제). 보수적으로 false</li>
 * </ul>
 * 활성 기수가 없는 시점(휴지기 등)에는 안전하게 false를 반환한다.
 */
@Component
@RequiredArgsConstructor
public class LoadOperatorStatusAdapter implements LoadOperatorStatusPort {

    private final GetGisuUseCase getGisuUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @Override
    public boolean isOperator(LoadOperatorStatusContext context) {
        Long senderMemberId = context.memberId();
        Inquiry inquiry = context.inquiry();

        // 활성 기수 없으면(휴지기 등) 운영진 판정 불가 → false (안전 기본값)
        Optional<GisuInfo> activeGisu = getGisuUseCase.findActiveGisu();
        if (activeGisu.isEmpty()) {
            return false;
        }
        Long gisuId = activeGisu.get().gisuId();

        return switch (inquiry.getTarget()) {
            case CENTRAL -> getChallengerRoleUseCase.isCentralMemberInGisu(senderMemberId, gisuId);
            case CHAPTER -> getChallengerRoleUseCase.isChapterPresidentInGisu(senderMemberId, gisuId);
            case SCHOOL -> {
                // 학교 = 문의 작성자(고객)의 소속 학교. 발신자가 "그 학교"의 운영진인지 판정.
                Long authorSchoolId = getMemberUseCase.getById(inquiry.getAuthorMemberId()).schoolId();
                if (authorSchoolId == null) {
                    yield false;   // 작성자 학교 미배정 → 판정 불가 → false
                }
                yield getChallengerRoleUseCase.isSchoolAdminInGisu(senderMemberId, gisuId, authorSchoolId);
            }
            case PRODUCT_TEAM -> false;   // 대응 역할 미정 — 별도 과제. 보수적으로 false.
        };
    }
}
