package com.umc.product.challenger.adapter.in.web.v2;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.challenger.adapter.in.web.v2.dto.response.ChallengerSearchV2Response;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.adapter.in.web.dto.request.SearchMemberRequest;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * /api/v2/challenger 의 Query 엔드포인트.
 * <p>
 * 회원 검색이 아닌 "챌린저 검색"이 목적이며, 같은 회원이 여러 기수에 참여했다면 기수별로 별도 row를 반환합니다.
 * 검색 조건은 회원 검색과 동일한 키워드/필터를 사용합니다 (SearchMemberQuery).
 * <p>
 * 권한 정책: v1과 동일하게 인증된 사용자가 호출 가능. 별도 권한 강화는 후속 PR.
 */
@RestController
@RequestMapping("/api/v2/challenger")
@RequiredArgsConstructor
@Tag(name = "Challenger V2 | 챌린저 Query", description = "챌린저 단위 검색 결과를 제공합니다.")
public class ChallengerSearchV2Controller {

    private final SearchMemberUseCase searchMemberUseCase;

    @Operation(
        operationId = "CHALLENGER-201",
        summary = "챌린저 검색 v2",
        description = """
            챌린저 단위 페이지네이션 검색입니다.

            - 같은 회원이 여러 기수에 참여했다면 기수별 챌린저가 각각 별도 row로 반환됩니다.
            - v1 회원 검색 응답에 더해 다음 두 필드를 제공합니다.
              - `challengerStatus`: 해당 행 챌린저의 상태 (ACTIVE/GRADUATED/EXPELLED/WITHDRAWN)
              - `isAdminInActiveGisu`: 회원이 현재 활성 기수에 운영진 ChallengerRole을 하나라도 보유하는지
            - 검색 결과에는 본인 외 회원이 포함되므로, 로그인 식별자인 이메일은 평문 노출을 피하기 위해
              컨트롤러 단에서 마스킹 처리되어 응답됩니다.
            - 회원 단위로 묶인 검색이 필요하다면 `/api/v2/member/search` 를 사용해 주세요.
            """
    )
    @GetMapping("search")
    public ChallengerSearchV2Response searchChallengersV2(
        @ParameterObject Pageable pageable,
        @ParameterObject SearchMemberRequest searchRequest,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        return ChallengerSearchV2Response.from(
            searchMemberUseCase.searchChallengersByV2(searchRequest.toQuery(memberPrincipal.getMemberId()), pageable)
        ).withMaskedEmails();
    }
}
