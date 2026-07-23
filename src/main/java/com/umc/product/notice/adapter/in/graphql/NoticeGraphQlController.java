package com.umc.product.notice.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.graphql.dto.PageGraphQlRequest;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;
import com.umc.product.notice.adapter.in.NoticeViewerInfoAssembler;
import com.umc.product.notice.adapter.in.graphql.dto.NoticeFilterGraphQlRequest;
import com.umc.product.notice.adapter.in.graphql.dto.NoticeGraphQlResponse;
import com.umc.product.notice.adapter.in.graphql.dto.NoticePageGraphQlResponse;
import com.umc.product.notice.application.port.in.command.ManageNoticeUseCase;
import com.umc.product.notice.application.port.in.query.GetNoticeUseCase;
import com.umc.product.notice.application.port.in.query.dto.NoticeViewerInfo;
import com.umc.product.organization.adapter.in.graphql.dto.ChapterGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.SchoolGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class NoticeGraphQlController {

    private final GetNoticeUseCase getNoticeUseCase;
    private final ManageNoticeUseCase manageNoticeUseCase;
    private final NoticeViewerInfoAssembler noticeViewerInfoAssembler;
    private final GetMemberUseCase getMemberUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final GetSchoolUseCase getSchoolUseCase;

    @QueryMapping
    public NoticePageGraphQlResponse notices(
        @CurrentMember MemberPrincipal principal,
        @Argument NoticeFilterGraphQlRequest filter,
        @Argument PageGraphQlRequest page
    ) {
        NoticeViewerInfo viewer = noticeViewerInfoAssembler.toMemberIdAndGisuId(
            principal.getMemberId(),
            filter.gisuId()
        );
        var pageable = PageGraphQlRequest.defaultIfNull(page)
            .toPageable(Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = filter.keyword() == null || filter.keyword().isBlank()
            ? getNoticeUseCase.getAllNoticeSummaries(viewer, filter.toClassification(), pageable)
            : getNoticeUseCase.searchNoticesByKeyword(
                filter.keyword(),
                viewer,
                filter.toClassification(),
                pageable
            );
        return NoticePageGraphQlResponse.from(result);
    }

    @QueryMapping
    @CheckAccess(resourceType = ResourceType.NOTICE, resourceId = "#id", permission = PermissionType.READ)
    public NoticeGraphQlResponse notice(
        @CurrentMember MemberPrincipal principal,
        @Argument Long id
    ) {
        var notice = getNoticeUseCase.getNoticeDetail(id, principal.getMemberId());
        manageNoticeUseCase.incrementViewCount(id);
        return NoticeGraphQlResponse.from(notice);
    }

    @BatchMapping(typeName = "Notice", field = "author")
    public Map<NoticeGraphQlResponse, MemberPublicInfo> authors(List<NoticeGraphQlResponse> notices) {
        Set<Long> memberIds = notices.stream()
            .map(NoticeGraphQlResponse::authorMemberId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberPublicInfo> membersById = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        Map<NoticeGraphQlResponse, MemberPublicInfo> result = new LinkedHashMap<>();
        for (NoticeGraphQlResponse notice : notices) {
            result.put(notice, membersById.get(notice.authorMemberId()));
        }
        return result;
    }

    @BatchMapping(typeName = "NoticeTarget", field = "gisu")
    public Map<NoticeGraphQlResponse.Target, GisuGraphQlResponse> targetGisus(
        List<NoticeGraphQlResponse.Target> targets
    ) {
        Set<Long> ids = targets.stream()
            .map(NoticeGraphQlResponse.Target::gisuId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, GisuInfo> byId = getGisuUseCase.getByIds(ids).stream()
            .collect(Collectors.toMap(GisuInfo::gisuId, Function.identity()));

        Map<NoticeGraphQlResponse.Target, GisuGraphQlResponse> result = new LinkedHashMap<>();
        for (NoticeGraphQlResponse.Target target : targets) {
            GisuInfo info = byId.get(target.gisuId());
            result.put(target, info == null ? null : GisuGraphQlResponse.from(info));
        }
        return result;
    }

    @BatchMapping(typeName = "NoticeTarget", field = "chapter")
    public Map<NoticeGraphQlResponse.Target, ChapterGraphQlResponse> targetChapters(
        List<NoticeGraphQlResponse.Target> targets
    ) {
        Set<Long> ids = targets.stream()
            .map(NoticeGraphQlResponse.Target::chapterId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, ChapterInfo> byId = getChapterUseCase.getAllChapters().stream()
            .filter(chapter -> ids.contains(chapter.id()))
            .collect(Collectors.toMap(ChapterInfo::id, Function.identity()));

        Map<NoticeGraphQlResponse.Target, ChapterGraphQlResponse> result = new LinkedHashMap<>();
        for (NoticeGraphQlResponse.Target target : targets) {
            ChapterInfo info = byId.get(target.chapterId());
            result.put(target, info == null ? null : ChapterGraphQlResponse.from(info));
        }
        return result;
    }

    @BatchMapping(typeName = "NoticeTarget", field = "school")
    public Map<NoticeGraphQlResponse.Target, SchoolGraphQlResponse> targetSchools(
        List<NoticeGraphQlResponse.Target> targets
    ) {
        Set<Long> ids = targets.stream()
            .map(NoticeGraphQlResponse.Target::schoolId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SchoolDetailInfo> byId = getSchoolUseCase.listDetailsByIds(ids).stream()
            .collect(Collectors.toMap(SchoolDetailInfo::schoolId, Function.identity()));

        Map<NoticeGraphQlResponse.Target, SchoolGraphQlResponse> result = new LinkedHashMap<>();
        for (NoticeGraphQlResponse.Target target : targets) {
            SchoolDetailInfo info = byId.get(target.schoolId());
            result.put(target, info == null ? null : SchoolGraphQlResponse.from(info));
        }
        return result;
    }
}
