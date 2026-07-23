package com.umc.product.challenger.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.challenger.adapter.in.graphql.dto.ChallengerFilterGraphQlRequest;
import com.umc.product.challenger.adapter.in.graphql.dto.ChallengerGraphQlResponse;
import com.umc.product.challenger.adapter.in.graphql.dto.ChallengerPageGraphQlResponse;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.SearchChallengerUseCase;
import com.umc.product.global.graphql.dto.PageGraphQlRequest;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChallengerGraphQlController {

    private final GetChallengerUseCase getChallengerUseCase;
    private final SearchChallengerUseCase searchChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetGisuUseCase getGisuUseCase;

    @QueryMapping
    public ChallengerGraphQlResponse challenger(
        @CurrentMember MemberPrincipal principal,
        @Argument Long id
    ) {
        return ChallengerGraphQlResponse.from(getChallengerUseCase.getById(id));
    }

    @QueryMapping
    public List<ChallengerGraphQlResponse> myChallengers(@CurrentMember MemberPrincipal principal) {
        return getChallengerUseCase.getAllByMemberId(principal.getMemberId()).stream()
            .map(ChallengerGraphQlResponse::from)
            .toList();
    }

    @QueryMapping
    public ChallengerPageGraphQlResponse challengers(
        @CurrentMember MemberPrincipal principal,
        @Argument ChallengerFilterGraphQlRequest filter,
        @Argument PageGraphQlRequest page
    ) {
        ChallengerFilterGraphQlRequest actualFilter = filter == null
            ? ChallengerFilterGraphQlRequest.empty()
            : filter;
        return ChallengerPageGraphQlResponse.from(
            searchChallengerUseCase.searchV2(
                actualFilter.toQuery(),
                PageGraphQlRequest.defaultIfNull(page).toPageableWithMaxOffset(10_000L)
            )
        );
    }

    @BatchMapping(typeName = "Challenger", field = "member")
    public Map<ChallengerGraphQlResponse, MemberPublicInfo> members(List<ChallengerGraphQlResponse> challengers) {
        Set<Long> memberIds = challengers.stream()
            .map(ChallengerGraphQlResponse::memberId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberPublicInfo> membersById = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        return challengers.stream().collect(Collectors.toMap(
            Function.identity(),
            challenger -> membersById.get(challenger.memberId()),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    @BatchMapping(typeName = "Challenger", field = "gisu")
    public Map<ChallengerGraphQlResponse, GisuGraphQlResponse> gisus(List<ChallengerGraphQlResponse> challengers) {
        Set<Long> gisuIds = challengers.stream()
            .map(ChallengerGraphQlResponse::gisuId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, GisuInfo> gisusById = getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toMap(GisuInfo::gisuId, Function.identity()));
        return challengers.stream().collect(Collectors.toMap(
            Function.identity(),
            challenger -> GisuGraphQlResponse.from(gisusById.get(challenger.gisuId())),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }
}
