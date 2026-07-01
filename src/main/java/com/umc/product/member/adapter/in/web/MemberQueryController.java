package com.umc.product.member.adapter.in.web;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.adapter.in.web.assembler.MemberInfoResponseAssembler;
import com.umc.product.member.adapter.in.web.dto.request.SearchMemberRequest;
import com.umc.product.member.adapter.in.web.dto.response.MemberInfoResponse;
import com.umc.product.member.adapter.in.web.dto.response.SearchMemberResponse;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
@Tag(name = "Member | 회원 Query", description = "회원 정보와 검색 결과를 조회합니다.")
public class MemberQueryController {

    private final MemberInfoResponseAssembler assembler;
    private final SearchMemberUseCase searchMemberUseCase;

    @Operation(operationId = "MEMBER-101", summary = "memberId로 회원 정보 조회")
    @GetMapping("profile/{memberId}")
    @CheckAccess(
        resourceType = ResourceType.MEMBER,
        permission = PermissionType.READ
    )
    MemberInfoResponse getMemberProfile(
        @PathVariable Long memberId
    ) {
        return assembler.fromMemberIdToPublic(memberId);
    }

    @Operation(operationId = "MEMBER-102", summary = "내 프로필 조회")
    @GetMapping("me")
    MemberInfoResponse getMyProfile(@CurrentMember MemberPrincipal memberPrincipal) {
        return assembler.fromMemberId(memberPrincipal.getMemberId());
    }

    @Operation(operationId = "MEMBER-103", summary = "회원 검색", description = """
        이름, 닉네임, 이메일, 학교명으로 검색하며 기수/파트/지부/학교별 필터링을 지원합니다.

        검색 결과에는 본인 외 회원이 포함되므로, 로그인 식별자인 이메일은 평문 노출을 피하기 위해
        컨트롤러 단에서 마스킹 처리되어 응답됩니다.
        """)
    @GetMapping("search")
    SearchMemberResponse searchMembers(
        @ParameterObject Pageable pageable,
        @ParameterObject SearchMemberRequest searchRequest,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        return SearchMemberResponse.from(
            searchMemberUseCase.searchBy(searchRequest.toQuery(memberPrincipal.getMemberId()), pageable)
        ).withMaskedEmails();
    }

}
