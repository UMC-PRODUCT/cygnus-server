package com.umc.product.member.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

/**
 * 스키마 {@code Member}(Node) 타입 응답. memberId/schoolId는 배치 로딩용 raw ID로 스키마에 노출하지 않는다.
 */
public record MemberGraphQlResponse(
    Long memberId,
    String name,
    String nickname,
    String email,
    Long schoolId,
    String profileImageLink,
    MemberStatus status
) implements RelayNode {

    public static MemberGraphQlResponse privateFrom(MemberInfo memberInfo) {
        return new MemberGraphQlResponse(
            memberInfo.id(),
            memberInfo.name(),
            memberInfo.nickname(),
            memberInfo.email(),
            memberInfo.schoolId(),
            memberInfo.profileImageLink(),
            memberInfo.status()
        );
    }

    public static MemberGraphQlResponse publicFrom(MemberInfo memberInfo) {
        return new MemberGraphQlResponse(
            memberInfo.id(),
            memberInfo.name(),
            memberInfo.nickname(),
            null,
            memberInfo.schoolId(),
            memberInfo.profileImageLink(),
            null
        );
    }

    @Override
    public String id() {
        return GlobalId.encode(GlobalIdTypes.MEMBER, memberId);
    }
}
