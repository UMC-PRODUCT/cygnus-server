package com.umc.product.notification.application.service;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.global.config.FcmProperties;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notification.application.port.in.SendNotificationToAudienceUseCase;
import com.umc.product.notification.application.port.in.dto.AudienceNotificationCommand;
import com.umc.product.notification.application.port.in.dto.NotificationCommand;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.application.port.out.SendFcmMessagePort;
import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;
import com.umc.product.notification.application.port.out.dto.FcmSendTarget;
import com.umc.product.notification.domain.FcmToken;
import com.umc.product.notification.domain.exception.FcmDomainException;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmAudienceService implements SendNotificationToAudienceUseCase {

    private static final int FCM_MULTICAST_BATCH_SIZE = 500;

    private final FcmProperties fcmProperties;
    private final SendFcmMessagePort sendFcmMessagePort;
    private final LoadFcmPort loadFcmPort;
    private final SaveFcmPort saveFcmPort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final OperationalMetrics operationalMetrics;

    @Override
    public void sendToAudience(AudienceNotificationCommand command) {

        if (!fcmProperties.enabled()) {
            log.info("[FCM 비활성화] sendToAudience를 건너뜁니다");
            return;
        }

        List<Long> memberIds = resolveTargetMemberIds(command.targetInfo());
        if (memberIds.isEmpty()) {
            log.info("알림 발송 대상 없음. targetInfo={}", command.targetInfo());
            return;
        }

        List<FcmToken> tokens = loadFcmPort.listActiveByMemberIds(memberIds);
        if (tokens.isEmpty()) {
            log.info("활성 FCM 토큰 없음. memberIds count={}", memberIds.size());
            return;
        }

        SendBatchResult result = sendBatch(tokens, command.title(), command.body());
        recordFcmMetric("SEND_TO_AUDIENCE", result);
        log.info("대상자 알림을 발송했습니다: memberCount={}, tokenCount={}", memberIds.size(), tokens.size());
    }

    @Override
    public void sendToMember(NotificationCommand command) {

        if (!fcmProperties.enabled()) {
            log.info("[FCM 비활성화] sendToMember를 건너뜁니다: memberId={}", command.memberId());
            return;
        }

        List<FcmToken> tokens = loadFcmPort.listActiveByMemberId(command.memberId());
        if (tokens.isEmpty()) {
            log.warn("활성 FCM 토큰 없음. memberId={}", command.memberId());
            return;
        }

        SendBatchResult result = sendBatch(tokens, command.title(), command.body());
        recordFcmMetric("SEND_TO_MEMBER", result);
    }

    @Override
    public void sendToMembers(List<Long> memberIds, String title, String body) {

        if (!fcmProperties.enabled()) {
            log.info("[FCM 비활성화] sendToMembers를 건너뜁니다: count={}", memberIds == null ? 0 : memberIds.size());
            return;
        }

        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        List<FcmToken> tokens = loadFcmPort.listActiveByMemberIds(memberIds);
        if (tokens.isEmpty()) {
            log.info("활성 FCM 토큰 없음. memberIds count={}", memberIds.size());
            return;
        }

        SendBatchResult result = sendBatch(tokens, title, body);
        recordFcmMetric("SEND_TO_MEMBERS", result);
        log.info("bulk 알림을 발송했습니다: memberCount={}, tokenCount={}", memberIds.size(), tokens.size());
    }

    // =========== PRIVATE ===========

    private SendBatchResult sendBatch(List<FcmToken> tokens, String title, String body) {
        int totalSuccess = 0;
        int totalFail = 0;

        for (List<FcmToken> batch : partition(tokens, FCM_MULTICAST_BATCH_SIZE)) {
            try {
                FcmSendResult response = sendFcmMessagePort.send(FcmSendRequest.of(
                    batch.stream()
                        .map(token -> FcmSendTarget.of(token.getId(), token.getFcmToken()))
                        .toList(),
                    title,
                    body,
                    Map.of(),
                    null,
                    null
                ));
                totalSuccess += response.successCount();
                totalFail += response.failureCount();
                deactivateInvalidTokens(batch, response.invalidTokenIds());
            } catch (FcmDomainException e) {
                log.error("FCM 배치 발송 실패 batchSize={}", batch.size(), e);
                totalFail += batch.size();
            }
        }

        log.info("FCM 발송 결과: success={}, failure={}", totalSuccess, totalFail);
        return new SendBatchResult(totalSuccess, totalFail);
    }

    private void deactivateInvalidTokens(List<FcmToken> tokens, List<Long> invalidTokenIds) {
        Set<Long> invalidTokenIdSet = new HashSet<>(invalidTokenIds);
        tokens.stream()
            .filter(token -> invalidTokenIdSet.contains(token.getId()))
            .forEach(token -> {
                token.deactivate();
                saveFcmPort.save(token);
            });
    }

    private void recordFcmMetric(String operation, SendBatchResult result) {
        operationalMetrics.recordNotification("FCM", operation, "success", result.successCount());
        operationalMetrics.recordNotification("FCM", operation, "failure", result.failureCount());
    }

    private List<Long> resolveTargetMemberIds(NoticeTargetInfo targetInfo) {

        if (targetInfo == null || targetInfo.targetGisuId() == null) {
            return List.of();
        }

        List<ChallengerInfo> challengers = getChallengerUseCase.getAllByGisuId(targetInfo.targetGisuId());
        if (challengers.isEmpty()) {
            return List.of();
        }

        // 1. 챌린저의 memberId 목록으로 schoolId 일괄 조회 (쿼리 1회)
        Set<Long> memberIdSet = challengers.stream()
            .map(ChallengerInfo::memberId)
            .collect(Collectors.toSet());

        Map<Long, Long> schoolIdByMemberId = getMemberUseCase.findAllSchoolIdsByIds(memberIdSet);

        // 2. (gisuId, schoolId) 조합으로 chapterId 일괄 조회 (쿼리 1회)
        Set<Long> gisuIds = Set.of(targetInfo.targetGisuId());
        Set<Long> schoolIds = new HashSet<>(schoolIdByMemberId.values());
        schoolIds.remove(null);
        Map<Long, Map<Long, ChapterInfo>> chapterMap =
            getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(gisuIds, schoolIds);

        // 3. 필터링
        List<Long> memberIds = new ArrayList<>();
        for (ChallengerInfo challenger : challengers) {
            Long schoolId = schoolIdByMemberId.get(challenger.memberId());
            if (schoolId == null) {
                continue;
            }

            ChapterInfo chapter = chapterMap
                .getOrDefault(challenger.gisuId(), Map.of())
                .get(schoolId);
            if (chapter == null) {
                continue;
            }

            if (targetInfo.isTarget(challenger.gisuId(), chapter.id(), schoolId, challenger.part())) {
                memberIds.add(challenger.memberId());
            }
        }

        return memberIds;
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();

        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }

        return result;
    }

    private record SendBatchResult(int successCount, int failureCount) {
    }
}
