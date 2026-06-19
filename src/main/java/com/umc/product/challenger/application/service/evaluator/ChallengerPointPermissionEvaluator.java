package com.umc.product.challenger.application.service.evaluator;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerPointUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerPointInfo;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChallengerPointPermissionEvaluator implements ResourcePermissionEvaluator {

    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerPointUseCase getChallengerPointUseCase;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.CHALLENGER_POINT;
    }

    // resourceId:
    // 생성 시: 부여하고자 하는 챌린저 ID 전달받음
    // 수정/삭제 시: 부여받은 챌린저 포인트 ID 전달받음

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case WRITE -> canCreate(subjectAttributes, resourcePermission);
            case EDIT -> canUpdate(subjectAttributes, resourcePermission);
            case DELETE -> canDelete(subjectAttributes, resourcePermission);
            default -> throw new CommonException(CommonErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED);
        };
    }

    private ChallengerInfo getGrantedChallengerInfo(Long challengerId) {
        return getChallengerUseCase.getById(challengerId);
    }

    /**
     * 상벌점 부여 권한 검증
     * <p>
     * 회원 (요청 한 사람), 대상 (상벌점을 부여받는 사람) 대상의 기수에, 회원이 중앙운영사무국 소속이거나, 회원이 해당 학교 Core(회장/부회장)여야 함
     */
    private boolean canCreate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        ChallengerInfo grantedChallengerInfo = getGrantedChallengerInfo(resourcePermission.getResourceIdAsLong());

        Long targetGisuId = grantedChallengerInfo.gisuId();
        Long targetSchoolId = getMemberUseCase.getById(grantedChallengerInfo.memberId()).schoolId();
        AuthoritySnapshot snapshot = subjectAttributes.toAuthoritySnapshot();

        // 대상의 기수에서 요청자가 중앙운영사무국 소속인지 확인
        if (snapshot.isCentralMemberInGisu(targetGisuId)) {
            return true;
        }

        // 대상의 기수에서 요청자가 같은 학교의 Core(회장/부회장)인지 확인
        if (targetSchoolId != null) {
            return snapshot.isSchoolCoreInGisu(targetGisuId, targetSchoolId);
        }

        return false;
    }

    private boolean canUpdate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        // resourceId에는 챌린저 포인트 ID가 담겨있음

        // 챌린저 포인트 정보를 가져옴
        ChallengerPointInfo challengerPointInfo = getChallengerPointUseCase.getById(
            resourcePermission.getResourceIdAsLong());

        // 거기에 있는 challengerId 값으로 사용자 조회
        ChallengerInfo grantedChallengerInfo = getGrantedChallengerInfo(challengerPointInfo.challengerId());

        Long targetGisuId = grantedChallengerInfo.gisuId();
        Long targetSchoolId = getMemberUseCase.getById(grantedChallengerInfo.memberId()).schoolId();
        AuthoritySnapshot snapshot = subjectAttributes.toAuthoritySnapshot();

        // 대상의 기수에서 요청자가 중앙운영사무국 소속인지 확인
        if (snapshot.isCentralMemberInGisu(targetGisuId)) {
            return true;
        }

        // 대상의 기수에서 요청자가 같은 학교의 Core(회장/부회장)인지 확인
        if (targetSchoolId != null) {
            return snapshot.isSchoolCoreInGisu(targetGisuId, targetSchoolId);
        }

        return false;
    }

    private boolean canDelete(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        ChallengerPointInfo challengerPointInfo = getChallengerPointUseCase.getById(
            resourcePermission.getResourceIdAsLong());
        ChallengerInfo grantedChallengerInfo = getGrantedChallengerInfo(challengerPointInfo.challengerId());

        // 중앙운영사무국 총괄단만 가능함
        return subjectAttributes.toAuthoritySnapshot().isCentralCoreInGisu(grantedChallengerInfo.gisuId());
    }
}
