package com.umc.product.notice.adapter.in.web;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.response.CursorResponse;
import com.umc.product.global.response.PageResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.notice.adapter.in.NoticeViewerInfoAssembler;
import com.umc.product.notice.adapter.in.web.dto.request.GetNoticeStatusRequest;
import com.umc.product.notice.adapter.in.web.dto.response.query.GetNoticeDetailResponse;
import com.umc.product.notice.adapter.in.web.dto.response.query.GetNoticeReadStatusResponse;
import com.umc.product.notice.adapter.in.web.dto.response.query.GetNoticeStaticsResponse;
import com.umc.product.notice.adapter.in.web.dto.response.query.GetNoticeSummaryResponse;
import com.umc.product.notice.adapter.in.web.swagger.NoticeQueryApi;
import com.umc.product.notice.application.port.in.command.ManageNoticeUseCase;
import com.umc.product.notice.application.port.in.query.GetNoticeUseCase;
import com.umc.product.notice.application.port.in.query.dto.NoticeInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusResult;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusSummary;
import com.umc.product.notice.application.port.in.query.dto.NoticeSummary;
import com.umc.product.notice.application.port.in.query.dto.NoticeViewerInfo;
import com.umc.product.notice.domain.NoticeClassification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeQueryController implements NoticeQueryApi {

    private final GetNoticeUseCase getNoticeUseCase;
    private final ManageNoticeUseCase manageNoticeUseCase;

    private final NoticeViewerInfoAssembler noticeViewerInfoAssembler;

    /*
     * 공지 전체 조회
     */
    @GetMapping
    public PageResponse<GetNoticeSummaryResponse> getAllNotices(
        @ParameterObject @Valid NoticeClassification classification,
        @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
        @ParameterObject Pageable pageable,
        @CurrentMember MemberPrincipal memberPrincipal) {

        NoticeViewerInfo viewerInfo = noticeViewerInfoAssembler.toMemberIdAndGisuId(memberPrincipal.getMemberId(),
            classification.gisuId());
        Page<NoticeSummary> notices = getNoticeUseCase.getAllNoticeSummaries(viewerInfo, classification, pageable);

        return PageResponse.of(notices, GetNoticeSummaryResponse::from);
    }

    /*
     * 검색어 기반 공지 전체 조회
     */
    @GetMapping("/search")
    public PageResponse<GetNoticeSummaryResponse> searchNotices(
        @RequestParam String keyword,
        @ParameterObject @Valid NoticeClassification classification,
        @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
        @ParameterObject
        Pageable pageable,
        @CurrentMember MemberPrincipal memberPrincipal) {

        NoticeViewerInfo viewerInfo = noticeViewerInfoAssembler.toMemberIdAndGisuId(memberPrincipal.getMemberId(),
            classification.gisuId());
        Page<NoticeSummary> notices = getNoticeUseCase.searchNoticesByKeyword(keyword, viewerInfo, classification,
            pageable);

        return PageResponse.of(notices, GetNoticeSummaryResponse::from);
    }

    /*
     * 공지사항 상세 조회
     */
    @CheckAccess(
        resourceType = ResourceType.NOTICE,
        resourceId = "#noticeId",
        permission = PermissionType.READ
    )
    @GetMapping("/{noticeId}")
    public GetNoticeDetailResponse getNotice(
        @PathVariable Long noticeId,
        @CurrentMember MemberPrincipal memberPrincipal) {

        NoticeInfo noticeDetail = getNoticeUseCase.getNoticeDetail(noticeId, memberPrincipal.getMemberId());
        manageNoticeUseCase.incrementViewCount(noticeId);
        return GetNoticeDetailResponse.from(noticeDetail);
    }

    /*
     * 공지사항 수신 현황 통계 조회
     */
    @CheckAccess(
        resourceType = ResourceType.NOTICE,
        resourceId = "#noticeId",
        permission = PermissionType.CHECK
    )
    @GetMapping("/{noticeId}/read-statics")
    public GetNoticeStaticsResponse getNoticeReadStatics(@PathVariable("noticeId") Long noticeId) {

        NoticeReadStatusSummary statistics = getNoticeUseCase.getReadStatistics(noticeId);
        return GetNoticeStaticsResponse.from(statistics);
    }

    /*
     * 공지사항 수신 현황 조회
     */
    @CheckAccess(
        resourceType = ResourceType.NOTICE,
        resourceId = "#noticeId",
        permission = PermissionType.CHECK
    )
    @GetMapping("/{noticeId}/read-status")
    public CursorResponse<GetNoticeReadStatusResponse> getNoticeReadStatus(
        @PathVariable("noticeId") Long noticeId,
        @ParameterObject @Valid GetNoticeStatusRequest request) {

        NoticeReadStatusResult result = getNoticeUseCase.getReadStatus(request.toQuery(noticeId));
        CursorResponse<GetNoticeReadStatusResponse> response = CursorResponse.of(
            result.content().stream()
                .map(GetNoticeReadStatusResponse::from)
                .toList(),
            result.cursorId(),
            result.hasNext()
        );

        return response;
    }
}
