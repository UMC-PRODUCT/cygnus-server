package com.umc.product.notification.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.notification.application.event.FcmNotificationRequestedEvent;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FcmAudienceResolver {

    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetChapterUseCase getChapterUseCase;

    public List<Long> resolve(FcmNotificationRequestedEvent event) {
        LinkedHashSet<Long> memberIds = new LinkedHashSet<>(event.memberIds());
        memberIds.addAll(resolveByTarget(event));
        return List.copyOf(memberIds);
    }

    private List<Long> resolveByTarget(FcmNotificationRequestedEvent event) {
        if (event.targetGisuId() == null) {
            return List.of();
        }

        List<ChallengerInfo> challengers = getChallengerUseCase.getAllByGisuId(event.targetGisuId());
        if (challengers.isEmpty()) {
            return List.of();
        }

        Set<Long> challengerMemberIds = new HashSet<>();
        for (ChallengerInfo challenger : challengers) {
            challengerMemberIds.add(challenger.memberId());
        }

        Map<Long, Long> schoolIdByMemberId = getMemberUseCase.findAllSchoolIdsByIds(challengerMemberIds);
        Set<Long> schoolIds = new HashSet<>(schoolIdByMemberId.values());
        schoolIds.remove(null);
        Map<Long, Map<Long, ChapterInfo>> chapterMap =
            getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(event.targetGisuId()), schoolIds);

        List<Long> targetMemberIds = new ArrayList<>();
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

            if (matches(event, challenger.gisuId(), chapter.id(), schoolId, challenger.part())) {
                targetMemberIds.add(challenger.memberId());
            }
        }

        return targetMemberIds;
    }

    private boolean matches(
        FcmNotificationRequestedEvent event,
        Long gisuId,
        Long chapterId,
        Long schoolId,
        ChallengerPart part
    ) {
        boolean gisuMatch = event.targetGisuId() == null || event.targetGisuId().equals(gisuId);
        boolean chapterMatch = event.targetChapterId() == null || event.targetChapterId().equals(chapterId);
        boolean schoolMatch = event.targetSchoolId() == null || event.targetSchoolId().equals(schoolId);
        boolean partMatch = event.targetParts().isEmpty() || event.targetParts().contains(part);
        return gisuMatch && chapterMatch && schoolMatch && partMatch;
    }
}
