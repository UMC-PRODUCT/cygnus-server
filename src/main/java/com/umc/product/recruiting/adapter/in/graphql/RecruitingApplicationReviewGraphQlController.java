package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.global.graphql.relay.ConnectionArguments;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.OffsetPageRequest;
import com.umc.product.global.graphql.relay.RelayConnection;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationReviewDetailGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationReviewGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationSearchGraphQlRequest;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingApplicationUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingApplicationReviewGraphQlController {

    private final SearchRecruitingApplicationUseCase searchApplicationUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public RelayConnection<RecruitingApplicationReviewGraphQlResponse> recruitingRoundApplications(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String roundId,
        @Argument RecruitingApplicationSearchGraphQlRequest input,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        Long decodedRoundId = GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        RecruitingApplicationSearchGraphQlRequest actualInput = input == null
            ? new RecruitingApplicationSearchGraphQlRequest(null, null)
            : input;
        var pageable = arguments.toPageable(() -> searchApplicationUseCase.search(
            actualInput.toQuery(decodedRoundId, requesterMemberId, new OffsetPageRequest(0, 1))
        ).getTotalElements());
        return RelayConnection.fromPage(
            searchApplicationUseCase.search(
                actualInput.toQuery(decodedRoundId, requesterMemberId, pageable)
            ),
            arguments,
            RecruitingApplicationReviewGraphQlResponse::from
        );
    }

    @QueryMapping
    public RecruitingApplicationReviewDetailGraphQlResponse recruitingRoundApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String roundId,
        @Argument String applicationId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationReviewDetailGraphQlResponse.from(
            searchApplicationUseCase.getDetail(
                GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND),
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                requesterMemberId
            )
        );
    }
}
