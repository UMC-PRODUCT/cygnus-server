package com.umc.product.inquiry.adapter.out.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import com.umc.product.inquiry.domain.Inquiry;

import lombok.RequiredArgsConstructor;

/**
 * 운영진 판정 어댑터.
 * <p>
 * 문의(inquiry)가 직접 보유한 target_* 컬럼(gisu/school/chapter)을 맥락으로 삼아, 문의 타겟(InquiryTarget)별로 발신자가 해당 조직의
 * 운영진인지 판정한다(ADR-006 권한 모델).
 * <ul>
 *     <li>CENTRAL — target 기수에서 중앙운영사무국 멤버 여부</li>
 *     <li>PRODUCT_TEAM — 정식 Role 미정. 임시로 중앙 멤버 판정으로 라우팅</li>
 *     <li>SCHOOL  — target 학교의 운영진 여부</li>
 *     <li>CHAPTER — target 지부의 지부장 여부</li>
 * </ul>
 * target_gisu_id가 없으면(기수 컨텍스트 없음) 안전하게 false를 반환한다.
 */
@Component
@RequiredArgsConstructor
public class LoadOperatorStatusAdapter implements LoadOperatorStatusPort {

    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Override
    public boolean isOperator(LoadOperatorStatusContext context) {
        Long senderMemberId = context.memberId();
        Inquiry inquiry = context.inquiry();

        Long gisuId = inquiry.getTargetGisuId();   // 컬럼에서 직접 읽음(활성기수 조회 안 함)
        if (gisuId == null) {
            return false;   // 기수 컨텍스트 없으면 판정 불가 → 안전 false
        }

        return switch (inquiry.getTarget()) {
            case CENTRAL -> getChallengerRoleUseCase.isCentralMemberInGisu(senderMemberId, gisuId);

            // PRODUCT_TEAM: 정식 Role 없음 → 임시로 중앙운영사무국 멤버 전체로 라우팅(ADR-006).
            // TODO: PRODUCT_TEAM 정식 매핑(별도 ADR/이슈). 현재는 중앙 멤버 판정으로 대체.
            case PRODUCT_TEAM -> getChallengerRoleUseCase.isCentralMemberInGisu(senderMemberId, gisuId);

            case SCHOOL -> {
                Long schoolId = inquiry.getTargetSchoolId();
                if (schoolId == null) {
                    yield false;   // SCHOOL인데 대상 학교 미지정 → 판정 불가 → false
                }
                yield getChallengerRoleUseCase.isSchoolAdminInGisu(senderMemberId, gisuId, schoolId);
            }

            case CHAPTER -> {
                Long chapterId = inquiry.getTargetChapterId();
                if (chapterId == null) {
                    yield false;   // CHAPTER인데 대상 지부 미지정 → 판정 불가 → false
                }
                yield getChallengerRoleUseCase.isChapterPresidentInGisu(senderMemberId, gisuId, chapterId);
            }
        };
    }
}
