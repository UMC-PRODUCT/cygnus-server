package com.umc.product.support.fixture;

import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.dto.MemberSystemRoleInfo;

public final class AuthorizationFixture {

    private AuthorizationFixture() {
    }

    public static ChallengerRole 중앙_역할(ChallengerRoleType roleType, Long gisuId) {
        return ChallengerRole.create(10L, roleType, null, null, gisuId);
    }

    public static ChallengerRole 학교_역할(
        ChallengerRoleType roleType,
        Long schoolId,
        Long gisuId
    ) {
        return ChallengerRole.create(10L, roleType, schoolId, null, gisuId);
    }

    public static ChallengerRole 학교_파트장(Long schoolId, Long gisuId, ChallengerPart part) {
        return ChallengerRole.create(
            10L,
            ChallengerRoleType.SCHOOL_PART_LEADER,
            schoolId,
            part,
            gisuId
        );
    }

    public static ChallengerRole 지부장(Long chapterId, Long gisuId) {
        return ChallengerRole.create(
            10L,
            ChallengerRoleType.CHAPTER_PRESIDENT,
            chapterId,
            null,
            gisuId
        );
    }

    public static RoleAttribute 역할_속성(
        ChallengerRoleType roleType,
        Long organizationId,
        Long gisuId
    ) {
        return new RoleAttribute(
            roleType,
            roleType.organizationType(),
            organizationId,
            null,
            gisuId
        );
    }

    public static MemberSystemRoleInfo 슈퍼_관리자(Long memberId) {
        return new MemberSystemRoleInfo(memberId, "SUPER_ADMIN");
    }

    public static MemberSystemRoleInfo 일반_시스템_역할(Long memberId) {
        return new MemberSystemRoleInfo(memberId, "MEMBER");
    }

    public static RoleAttribute 학교_역할_속성(
        ChallengerRoleType roleType,
        Long schoolId,
        Long gisuId
    ) {
        return new RoleAttribute(roleType, OrganizationType.SCHOOL, schoolId, null, gisuId);
    }
}
