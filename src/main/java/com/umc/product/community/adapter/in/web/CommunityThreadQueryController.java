package com.umc.product.community.adapter.in.web;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.adapter.in.web.dto.response.CommunityThreadDetailResponse;
import com.umc.product.community.adapter.in.web.dto.response.CommunityThreadInvitablePageResponse;
import com.umc.product.community.adapter.in.web.dto.response.CommunityThreadListResponse;
import com.umc.product.community.adapter.in.web.dto.response.CommunityThreadMemberPageResponse;
import com.umc.product.community.adapter.in.web.dto.response.CommunityThreadMessagePageResponse;
import com.umc.product.community.adapter.in.web.validation.CodePointLength;
import com.umc.product.community.adapter.in.web.validation.PositiveDecimalId;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.ListCommunityThreadMembersUseCase;
import com.umc.product.community.application.port.in.query.thread.ListCommunityThreadsUseCase;
import com.umc.product.community.application.port.in.query.thread.SearchCommunityThreadInvitableUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ListThreadMembersQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ListThreadsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.SearchThreadInvitableQuery;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageHistoryUseCase;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageHistoryQuery;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
@Validated
@Tag(name = "Community | Thread Query", description = "Community thread 조회와 REST recovery API")
public class CommunityThreadQueryController {

    private final ListCommunityThreadsUseCase listThreadsUseCase;
    private final GetCommunityThreadDetailUseCase getThreadDetailUseCase;
    private final ListCommunityThreadMembersUseCase listThreadMembersUseCase;
    private final SearchCommunityThreadInvitableUseCase searchThreadInvitableUseCase;
    private final GetCommunityThreadMessageHistoryUseCase getMessageHistoryUseCase;

    @GetMapping("/threads")
    @Operation(operationId = "COMMUNITY-THREAD-101", summary = "Community thread 목록 조회")
    public CommunityThreadListResponse listThreads(
        @RequestParam(defaultValue = "all")
        @Pattern(regexp = "all|unread|STUDY|QNA|PROJECT|FREE") String filter,
        @RequestParam(name = "q", required = false) @CodePointLength(max = 80) String query,
        @RequestParam(defaultValue = "0") @Min(0) int offset,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @CurrentMember MemberPrincipal principal
    ) {
        return CommunityThreadListResponse.from(listThreadsUseCase.listThreads(
            new ListThreadsQuery(
                principal.getMemberId(), CommunityThreadFilterParser.parse(filter), query, offset, limit
            )
        ));
    }

    @GetMapping("/threads/{threadId}")
    @Operation(operationId = "COMMUNITY-THREAD-102", summary = "Community thread 상세 조회")
    public CommunityThreadDetailResponse getThread(
        @PathVariable @PositiveDecimalId String threadId,
        @CurrentMember MemberPrincipal principal
    ) {
        return CommunityThreadDetailResponse.from(getThreadDetailUseCase.getThread(
            new GetThreadDetailQuery(CommunityWebNumbers.id(threadId), principal.getMemberId())
        ));
    }

    @GetMapping("/threads/{threadId}/members")
    @Operation(operationId = "COMMUNITY-THREAD-103", summary = "Community thread member 조회")
    public CommunityThreadMemberPageResponse listMembers(
        @PathVariable @PositiveDecimalId String threadId,
        @RequestParam(name = "q", required = false) @CodePointLength(max = 80) String memberQuery,
        @RequestParam(required = false) CommunityThreadMemberRole role,
        @RequestParam(required = false) ChallengerPart part,
        @RequestParam(required = false) @Positive Long generation,
        @RequestParam(defaultValue = "0") @Min(0) int offset,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @CurrentMember MemberPrincipal principal
    ) {
        return CommunityThreadMemberPageResponse.from(listThreadMembersUseCase.listMembers(
            new ListThreadMembersQuery(
                CommunityWebNumbers.id(threadId), principal.getMemberId(), memberQuery, role, part,
                generation, offset, limit
            )
        ));
    }

    @GetMapping("/threads/{threadId}/invitable")
    @Operation(
        operationId = "COMMUNITY-THREAD-104",
        summary = "초대 가능한 회원 조회"
    )
    public CommunityThreadInvitablePageResponse searchInvitable(
        @PathVariable @PositiveDecimalId String threadId,
        @RequestParam(name = "q", required = false) @CodePointLength(max = 80) String invitableQuery,
        @RequestParam(defaultValue = "0") @Min(0) int offset,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @CurrentMember MemberPrincipal principal
    ) {
        return CommunityThreadInvitablePageResponse.from(searchThreadInvitableUseCase.searchInvitable(
            new SearchThreadInvitableQuery(
                CommunityWebNumbers.id(threadId), principal.getMemberId(), invitableQuery, offset, limit
            )
        ));
    }

    @GetMapping("/threads/{threadId}/messages")
    @Operation(operationId = "COMMUNITY-THREAD-105", summary = "Community thread message history 조회")
    public CommunityThreadMessagePageResponse getMessageHistory(
        @PathVariable @PositiveDecimalId String threadId,
        @RequestParam(required = false) @PositiveDecimalId String before,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) int limit,
        @CurrentMember MemberPrincipal principal
    ) {
        return CommunityThreadMessagePageResponse.from(getMessageHistoryUseCase.getHistory(
            new CommunityThreadMessageHistoryQuery(
                CommunityWebNumbers.id(threadId),
                principal.getMemberId(),
                CommunityWebNumbers.optionalId(before),
                limit
            )
        ));
    }
}
