package com.umc.product.authentication.adapter.in.graphql;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.authentication.application.port.in.query.CheckCredentialAvailabilityUseCase;
import com.umc.product.authentication.application.port.in.query.GetMemberOAuthUseCase;
import com.umc.product.authentication.application.port.in.query.dto.MemberOAuthInfo;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthenticationGraphQlController {

    private final GetMemberOAuthUseCase getMemberOAuthUseCase;
    private final CheckCredentialAvailabilityUseCase checkCredentialAvailabilityUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @QueryMapping
    public List<MemberOAuthInfo> myOAuthConnections(@CurrentMember MemberPrincipal principal) {
        return getMemberOAuthUseCase.getOAuthList(principal.getMemberId());
    }

    @QueryMapping
    public boolean credentialEmailAvailable(@Argument String email) {
        return checkCredentialAvailabilityUseCase.isEmailAvailable(email);
    }

    @BatchMapping(typeName = "MemberOAuth", field = "member")
    public Map<MemberOAuthInfo, MemberPublicInfo> members(List<MemberOAuthInfo> connections) {
        Set<Long> memberIds = connections.stream()
            .map(MemberOAuthInfo::memberId)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> membersById = getMemberUseCase.findAllByIds(memberIds);

        return connections.stream().collect(Collectors.toMap(
            Function.identity(),
            connection -> MemberPublicInfo.from(membersById.get(connection.memberId()))
        ));
    }
}
