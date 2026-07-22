package com.umc.product.notice.application.service.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.notice.application.port.in.query.GetNoticeContentUseCase;
import com.umc.product.notice.application.port.in.query.GetNoticeUseCase;
import com.umc.product.notice.application.port.in.query.dto.GetNoticeStatusQuery;
import com.umc.product.notice.application.port.in.query.dto.NoticeImageInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeLinkInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusResult;
import com.umc.product.notice.application.port.in.query.dto.NoticeReadStatusSummary;
import com.umc.product.notice.application.port.in.query.dto.NoticeSummary;
import com.umc.product.notice.application.port.in.query.dto.NoticeViewerInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeVoteInfo;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.application.port.out.LoadNoticeReadPort;
import com.umc.product.notice.application.port.out.LoadNoticeTargetPort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeClassification;
import com.umc.product.notice.domain.NoticeRead;
import com.umc.product.notice.domain.NoticeTarget;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notice.domain.enums.NoticeTargetPattern;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.notice.domain.exception.NoticeErrorCode;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoticeQueryService implements GetNoticeUseCase {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final LoadNoticePort loadNoticePort;
    private final LoadNoticeReadPort loadNoticeReadPort;
    private final LoadNoticeTargetPort loadNoticeTargetPort;

    private final GetChapterUseCase getChapterUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetNoticeContentUseCase getNoticeContentUseCase;

    @Override
    public Page<NoticeSummary> getAllNoticeSummaries(NoticeViewerInfo viewerInfo, NoticeClassification classification,
                                                     Pageable pageable) {
        validateClassification(classification, viewerInfo);
        NoticeClassification enriched = enrichClassification(viewerInfo, classification);
        Page<Notice> notices = loadNoticePort.findNoticesByClassification(enriched, viewerInfo, pageable);
        return toNoticeSummaryPage(notices);
    }

    @Override
    public Page<NoticeSummary> searchNoticesByKeyword(String keyword, NoticeViewerInfo viewerInfo,
                                                      NoticeClassification classification,
                                                      Pageable pageable) {
        validateClassification(classification, viewerInfo);
        NoticeClassification enriched = enrichClassification(viewerInfo, classification);
        Page<Notice> notices = loadNoticePort.findNoticesByKeyword(keyword, enriched, viewerInfo, pageable);
        return toNoticeSummaryPage(notices);
    }

    @Override
    public NoticeInfo getNoticeDetail(Long noticeId, Long memberId) {
        // 권한 검증은 컨트롤러에서 @CheckAccess 사용
        // Notice, NoticeContent 조회
        Notice notice = findById(noticeId);
        List<NoticeImageInfo> imageInfos = getNoticeContentUseCase.findImageByNoticeId(noticeId);
        List<NoticeLinkInfo> linkInfos = getNoticeContentUseCase.findLinkByNoticeId(noticeId);
        NoticeVoteInfo voteInfo = getNoticeContentUseCase.findVoteByNoticeId(noticeId, memberId);

        // NoticeTargetInfo 조회
        NoticeTarget target = loadNoticeTargetPort.findByNoticeId(noticeId).orElse(null);
        NoticeTargetInfo targetInfo = target != null ? NoticeTargetInfo.from(target) : null;

        return new NoticeInfo(
            notice.getId(),
            notice.getTitle(),
            notice.getContent(),
            notice.getAuthorMemberId(),
            notice.isMustRead(),
            voteInfo,
            imageInfos,
            linkInfos,
            targetInfo,
            notice.getViewCount(),
            notice.getCreatedAt()
        );
    }

    @Override
    public NoticeReadStatusResult getReadStatus(GetNoticeStatusQuery command) {
        TargetChallengerContext context = findTargetChallengerContext(command.noticeId());
        if (context.targetChallengers().isEmpty()) {
            return new NoticeReadStatusResult(List.of(), null, false);
        }

        // 조직 필터 적용
        List<ChallengerInfo> filteredByOrg = context.targetChallengers().stream()
            .filter(
                challenger -> isOrganizationMatch(command, challenger, context.memberMap().get(challenger.memberId()),
                    context.chapterCache()))
            .toList();

        if (filteredByOrg.isEmpty()) {
            return new NoticeReadStatusResult(List.of(), null, false);
        }

        List<NoticeRead> reads = loadNoticeReadPort.findNoticeReadByNoticeId(command.noticeId());
        Map<Long, NoticeRead> readMap = reads.stream()
            .collect(Collectors.toMap(NoticeRead::getChallengerId, Function.identity()));

        // 읽음/안읽음 상태에 따라 필터링
        List<ChallengerInfo> filteredByStatus = switch (command.status()) {
            case READ -> filteredByOrg.stream()
                .filter(challenger -> readMap.containsKey(challenger.challengerId()))
                .toList();
            case UNREAD -> filteredByOrg.stream()
                .filter(challenger -> !readMap.containsKey(challenger.challengerId()))
                .toList();
        };

        // 커서 기반 페이지네이션 적용
        int startIdx = findCursorIndex(filteredByStatus, readMap, command);

        List<ChallengerInfo> pagedChallengers = filteredByStatus.stream()
            .skip(startIdx)
            .limit(DEFAULT_PAGE_SIZE + 1)  // 다음 페이지 존재 여부 확인용
            .toList();

        boolean hasNext = pagedChallengers.size() > DEFAULT_PAGE_SIZE;
        List<ChallengerInfo> resultChallengers = hasNext
            ? pagedChallengers.subList(0, DEFAULT_PAGE_SIZE)
            : pagedChallengers;

        List<NoticeReadStatusInfo> content = resultChallengers.stream()
            .map(challenger -> toReadStatusInfo(challenger, context.memberMap().get(challenger.memberId()),
                context.chapterCache()))
            .toList();

        Long nextCursorId = content.isEmpty() ? null : switch (command.status()) {
            case READ -> readMap.get(content.get(content.size() - 1).challengerId()).getId();
            case UNREAD -> content.get(content.size() - 1).challengerId();
        };

        return new NoticeReadStatusResult(content, nextCursorId, hasNext);
    }

    @Override
    public NoticeReadStatusSummary getReadStatistics(Long noticeId) {
        List<Long> targetIds = findTargetChallengerIds(noticeId);

        int totalCount = targetIds.size();
        if (totalCount == 0) {
            return new NoticeReadStatusSummary(0, 0, 0, 0f);
        }

        int readCount = (int) loadNoticeReadPort.countReadsByChallengerIdIn(noticeId, targetIds);
        int unreadCount = totalCount - readCount;
        float readRate = (float) readCount / totalCount * 100;

        return new NoticeReadStatusSummary(totalCount, readCount, unreadCount, readRate);
    }

    // ====== PRIVATE =====

    /**
     * 공지 대상 챌린저 조회를 위한 공통 컨텍스트를 생성합니다.
     */
    private TargetChallengerContext findTargetChallengerContext(Long noticeId) {
        NoticeTarget target = loadNoticeTargetPort.findByNoticeId(noticeId)
            .orElseThrow(() -> new NoticeDomainException(NoticeErrorCode.NOTICE_NOT_FOUND));
        NoticeTargetInfo targetInfo = NoticeTargetInfo.from(target);

        List<ChallengerInfo> challengers;

        // 전체 기수 공지 여부
        if (targetInfo.targetGisuId() != null) {
            challengers = getChallengerUseCase.getAllByGisuId(targetInfo.targetGisuId());
        } else {
            // 모든 기수 대상 공지: DB 쿼리에서 멤버당 최신 기수 챌린저 1건만 조회 (읽음 현황 조회 시 혼선 방지)
            challengers = getChallengerUseCase.getAllLatestGisuPerMemberWithoutChallengerPoints();
        }

        if (challengers.isEmpty()) {
            return new TargetChallengerContext(List.of(), Map.of(), new HashMap<>());
        }

        Map<Long, MemberInfo> memberMap = getMemberUseCase.findAllByIds(
            challengers.stream().map(ChallengerInfo::memberId).collect(Collectors.toSet())
        );

        // 필터링 전에 필요한 챕터 정보를 1번 쿼리로 선점해 캐시에 채워둠
        Set<Long> gisuIds = challengers.stream()
            .map(ChallengerInfo::gisuId)
            .collect(Collectors.toSet());

        Set<Long> schoolIds = challengers.stream()
            .map(c -> memberMap.get(c.memberId()))
            .filter(Objects::nonNull)
            .map(MemberInfo::schoolId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<ChapterKey, ChapterInfo> chapterCache = new HashMap<>();
        getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(gisuIds, schoolIds)
            .forEach((gisuId, schoolMap) ->
                schoolMap.forEach((schoolId, info) ->
                    chapterCache.put(new ChapterKey(gisuId, schoolId), info)
                )
            );

        // 조건에 맞는 챌린저 필터링
        List<ChallengerInfo> targetChallengers = challengers.stream()
            .filter(challenger -> isTargetChallenger(targetInfo, challenger, memberMap.get(challenger.memberId()),
                chapterCache))
            .toList();

        return new TargetChallengerContext(targetChallengers, memberMap, chapterCache);
    }

    /**
     * 공지 대상 챌린저 ID 목록을 반환합니다. (경량 조회)
     * <p>
     * 통계 집계처럼 ID만 필요한 경우 사용합니다.
     */
    private List<Long> findTargetChallengerIds(Long noticeId) {
        NoticeTarget target = loadNoticeTargetPort.findByNoticeId(noticeId)
            .orElseThrow(() -> new NoticeDomainException(NoticeErrorCode.NOTICE_NOT_FOUND));
        NoticeTargetInfo targetInfo = NoticeTargetInfo.from(target);

        List<ChallengerInfo> challengers;
        if (targetInfo.targetGisuId() != null) {
            challengers = getChallengerUseCase.getAllByGisuId(targetInfo.targetGisuId());
        } else {
            challengers = getChallengerUseCase.getAllLatestGisuPerMemberWithoutChallengerPoints();
        }

        if (challengers.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> schoolIdMap = getMemberUseCase.findAllSchoolIdsByIds(
            challengers.stream().map(ChallengerInfo::memberId).collect(Collectors.toSet())
        );

        Set<Long> gisuIds = challengers.stream()
            .map(ChallengerInfo::gisuId)
            .collect(Collectors.toSet());

        Set<Long> schoolIds = schoolIdMap.values().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<ChapterKey, ChapterInfo> chapterCache = new HashMap<>();
        getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(gisuIds, schoolIds)
            .forEach((gisuId, schoolMap) ->
                schoolMap.forEach((schoolId, info) ->
                    chapterCache.put(new ChapterKey(gisuId, schoolId), info)
                )
            );

        return challengers.stream()
            .filter(challenger -> isTargetChallenger(targetInfo, challenger,
                schoolIdMap.get(challenger.memberId()), chapterCache))
            .map(ChallengerInfo::challengerId)
            .toList();
    }


    /**
     * 조회 범위(지부/학교) 권한 검증.
     * <p>
     * 총괄/중앙운영진(CENTRAL_MEMBER)은 타 학교/지부 공지까지 자유롭게 조회할 수 있습니다. 그 외 역할(회장단/지부장/파트장/일반 챌린저)은 본인 소속 지부/학교 범위만 조회할 수
     * 있습니다. 목록 조회와 상세 조회(@CheckAccess)의 접근 범위를 일치시켜, 임의의 학교/지부 ID를 넣어 타 범위 공지를 열람하는 우회를 차단합니다.
     */
    private void validateViewerScope(NoticeClassification classification, NoticeViewerInfo viewerInfo) {
        if (viewerInfo.viewerRole() == NoticeTab.CENTRAL_MEMBER) {
            return;
        }
        if (classification.chapterId() != null
            && !classification.chapterId().equals(viewerInfo.chapterId())) {
            throw new NoticeDomainException(NoticeErrorCode.NO_READ_PERMISSION,
                "본인 소속 지부의 공지만 조회할 수 있습니다.");
        }
        if (classification.schoolId() != null
            && !classification.schoolId().equals(viewerInfo.schoolId())) {
            throw new NoticeDomainException(NoticeErrorCode.NO_READ_PERMISSION,
                "본인 소속 학교의 공지만 조회할 수 있습니다.");
        }
    }

    private void validateClassification(NoticeClassification classification, NoticeViewerInfo viewerInfo) {
        validateViewerScope(classification, viewerInfo);
        if (classification.isChallengerQuery()) {
            // 챌린저 공지: 필드 조합 유효성 검증 (지부+학교 동시 지정 등 불가 조합 차단)
            NoticeTargetPattern.from(classification.toTargetInfo());
        } else {
            // 운영진 공지: 조회자 역할이 요청 역할 이상인지 검증
            NoticeTab viewerRole = viewerInfo.viewerRole();
            if (viewerRole == null) {
                throw new NoticeDomainException(NoticeErrorCode.NO_READ_PERMISSION,
                    "운영진 공지를 조회할 권한이 없습니다.");
            }
            if (!classification.noticeTab().includes(viewerRole)) {
                throw new NoticeDomainException(NoticeErrorCode.NO_READ_PERMISSION,
                    "조회자의 역할이 요청한 공지 대상 역할보다 낮습니다.");
            }
        }
    }

    /**
     * 챌린저 공지 한정으로 파트 필터 시 지부/학교 ID를 보완합니다.
     * <p>
     * - chapterId/schoolId가 이미 명시된 경우: 그대로 사용 (총괄단 등이 특정 범위 지정 가능) - 챌린저 공지에서 part만 있고 chapterId/schoolId 없는 경우:
     * viewerInfo로 보완 - 운영진 공지는 schoolId를 자동 보완하지 않습니다. schoolId 유무로 중앙공지/교내공지를 명확히 구분하므로, 반드시 클라이언트가 명시해야 합니다.
     */
    private NoticeClassification enrichClassification(NoticeViewerInfo viewerInfo,
                                                      NoticeClassification classification) {
        if (classification.chapterId() == null
            && classification.schoolId() == null
            && classification.part() == null) {
            return classification;
        }

        boolean hasPart = classification.part() != null;
        boolean isChallengerQuery = classification.isChallengerQuery();

        Long filteredChapterId = classification.chapterId() != null
            ? classification.chapterId()
            : (hasPart && isChallengerQuery ? viewerInfo.chapterId() : null);
        Long filteredSchoolId = classification.schoolId() != null
            ? classification.schoolId()
            : (hasPart && isChallengerQuery ? viewerInfo.schoolId() : null);

        return new NoticeClassification(classification.gisuId(), filteredChapterId, filteredSchoolId,
            classification.part(), classification.noticeTab());
    }

    private Page<NoticeSummary> toNoticeSummaryPage(Page<Notice> notices) {
        NoticeQueryData queryData = fetchBatchData(notices);
        return notices.map(notice -> toNoticeSummary(notice, queryData));
    }

    // N+1문제 방지를 위해 Batch로 데이터를 정리
    private NoticeQueryData fetchBatchData(Page<Notice> notices) {
        List<Long> noticeIds = notices.getContent().stream().map(Notice::getId).toList();

        Set<Long> authorMemberIds = notices.getContent().stream()
            .map(Notice::getAuthorMemberId).collect(Collectors.toSet());

        Map<Long, NoticeTarget> targetMap = loadNoticeTargetPort.findByNoticeIdIn(noticeIds).stream()
            .collect(Collectors.toMap(NoticeTarget::getNoticeId, Function.identity()));

        Map<Long, MemberInfo> memberMap = getMemberUseCase.findAllByIds(authorMemberIds);

        return new NoticeQueryData(targetMap, memberMap);
    }

    // Notice를 NoticeSummary로 매핑
    private NoticeSummary toNoticeSummary(Notice notice, NoticeQueryData data) {
        MemberInfo memberInfo = data.memberMap.get(notice.getAuthorMemberId());

        NoticeTarget target = data.targetMap.get(notice.getId());

        NoticeTargetInfo targetInfo = target != null ? NoticeTargetInfo.from(target) : null;

        long viewCount = notice.getViewCount();

        return new NoticeSummary(
            notice.getId(), notice.getTitle(), notice.getContent(),
            notice.isShouldSendNotification(), notice.isMustRead(), viewCount, notice.getCreatedAt(),
            targetInfo, notice.getAuthorMemberId(),
            memberInfo != null ? memberInfo.nickname() : null,
            memberInfo != null ? memberInfo.name() : null
        );
    }

    private Notice findById(Long noticeId) {
        return loadNoticePort.findNoticeById(noticeId).orElseThrow(
            () -> new NoticeDomainException(NoticeErrorCode.NOTICE_NOT_FOUND));
    }

    private boolean isTargetChallenger(
        NoticeTargetInfo targetInfo,
        ChallengerInfo challenger,
        MemberInfo memberInfo,
        Map<ChapterKey, ChapterInfo> chapterCache
    ) {
        if (memberInfo == null) {
            return false;
        }

        ChapterInfo chapterInfo = getCachedChapterInfo(challenger.gisuId(), memberInfo.schoolId(), chapterCache);
        Long chapterId = chapterInfo != null ? chapterInfo.id() : null;

        return targetInfo.isTarget(
            challenger.gisuId(),
            chapterId,
            memberInfo.schoolId(),
            challenger.part()
        );
    }

    private boolean isTargetChallenger(
        NoticeTargetInfo targetInfo,
        ChallengerInfo challenger,
        Long schoolId,
        Map<ChapterKey, ChapterInfo> chapterCache
    ) {
        if (schoolId == null) {
            return false;
        }

        ChapterInfo chapterInfo = getCachedChapterInfo(challenger.gisuId(), schoolId, chapterCache);
        Long chapterId = chapterInfo != null ? chapterInfo.id() : null;

        return targetInfo.isTarget(
            challenger.gisuId(),
            chapterId,
            schoolId,
            challenger.part()
        );
    }

    private boolean isOrganizationMatch(
        GetNoticeStatusQuery command,
        ChallengerInfo challenger,
        MemberInfo memberInfo,
        Map<ChapterKey, ChapterInfo> chapterCache
    ) {
        if (command.filterType() == null) {
            return true;
        }

        return switch (command.filterType()) {
            case ALL -> true;
            case SCHOOL -> memberInfo != null
                && command.organizationId() != null
                && command.organizationId().contains(memberInfo.schoolId());
            case CHAPTER -> {
                if (memberInfo == null || command.organizationId() == null || command.organizationId().isEmpty()) {
                    yield false;
                }
                ChapterInfo chapterInfo = getCachedChapterInfo(
                    challenger.gisuId(), memberInfo.schoolId(), chapterCache
                );
                yield chapterInfo != null && command.organizationId().contains(chapterInfo.id());
            }
        };
    }

    private ChapterInfo getCachedChapterInfo(Long gisuId, Long schoolId, Map<ChapterKey, ChapterInfo> cache) {
        if (gisuId == null || schoolId == null) {
            return null;
        }
        return cache.computeIfAbsent(
            new ChapterKey(gisuId, schoolId),
            key -> getChapterUseCase.byGisuAndSchool(key.gisuId(), key.schoolId())
        );
    }

    private NoticeReadStatusInfo toReadStatusInfo(
        ChallengerInfo challenger,
        MemberInfo memberInfo,
        Map<ChapterKey, ChapterInfo> chapterCache
    ) {
        ChapterInfo chapterInfo = memberInfo != null
            ? getCachedChapterInfo(challenger.gisuId(), memberInfo.schoolId(), chapterCache)
            : null;

        Long chapterId = chapterInfo != null ? chapterInfo.id() : null;
        String chapterName = chapterInfo != null ? chapterInfo.name() : null;

        return new NoticeReadStatusInfo(
            challenger.challengerId(),
            memberInfo != null ? memberInfo.name() : null,
            memberInfo != null ? memberInfo.profileImageLink() : null,
            challenger.part(),
            memberInfo != null ? memberInfo.schoolId() : null,
            memberInfo != null ? memberInfo.schoolName() : null,
            chapterId,
            chapterName
        );
    }

    /**
     * 커서 위치 이후의 시작 인덱스를 찾습니다.
     */
    private int findCursorIndex(
        List<ChallengerInfo> challengers,
        Map<Long, NoticeRead> readMap,
        GetNoticeStatusQuery command
    ) {
        if (command.cursorId() == null) {
            return 0;
        }

        for (int i = 0; i < challengers.size(); i++) {
            ChallengerInfo challenger = challengers.get(i);
            Long currentCursorId = switch (command.status()) {
                case READ -> readMap.get(challenger.challengerId()).getId();
                case UNREAD -> challenger.challengerId();
            };

            if (currentCursorId.equals(command.cursorId())) {
                return i + 1;  // 커서 다음부터 시작
            }
        }

        return 0;  // 커서를 찾지 못하면 처음부터
    }

    // ChapterInfo 캐싱용 키
    private record ChapterKey(Long gisuId, Long schoolId) {
    }

    // =============== PRIVATE records ===============

    private record NoticeQueryData(
        Map<Long, NoticeTarget> targetMap,
        Map<Long, MemberInfo> memberMap
    ) {
    }

    private record TargetChallengerContext(
        List<ChallengerInfo> targetChallengers,
        Map<Long, MemberInfo> memberMap,
        Map<ChapterKey, ChapterInfo> chapterCache
    ) {
    }

}
