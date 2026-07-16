package com.umc.product.notice.adapter.in.web.assembler;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.notice.application.port.in.query.dto.NoticeViewerInfo;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공지 조회자의 소속 정보를 여러 UseCase를 통해 조립하는 헬퍼 컴포넌트입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeViewerInfoAssembler {

    private final GetChallengerUseCase getChallengerUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetChapterUseCase getChapterUseCase;

    public NoticeViewerInfo toMemberIdAndGisuId(Long memberId, Long gisuId) {
        Set<ChallengerPart> memberParts = resolveParts(memberId, gisuId);
        NoticeTab viewerRole = resolveViewerRole(memberId, gisuId);

        MemberInfo memberInfo = getMemberUseCase.findAllByIds(Set.of(memberId)).get(memberId);
        Long schoolId = memberInfo != null ? memberInfo.schoolId() : null;

        Long chapterId = null;
        if (schoolId != null) {
            try {
                chapterId = getChapterUseCase.byGisuAndSchool(gisuId, schoolId).id();
            } catch (Exception e) {
                log.debug("지부 정보 조회 실패 - gisuId={}, schoolId={}: {}", gisuId, schoolId, e.getMessage());
            }
        }

        return new NoticeViewerInfo(memberParts, schoolId, chapterId, viewerRole);
    }

    private Set<ChallengerPart> resolveParts(Long memberId, Long gisuId) {
        if (memberId == null || gisuId == null) {
            return Set.of();
        }

        return getChallengerUseCase.findByMemberIdAndGisuId(memberId, gisuId)
            .map(challenger -> {
                Set<ChallengerPart> parts = new HashSet<>();
                parts.add(challenger.part());
                parts.addAll(getChallengerRoleUseCase.getAllResponsiblePartByMemberIdAndGisuId(memberId, gisuId));
                return parts;
            })
            .orElse(Set.of());
    }

    /**
     * 조회자의 최상위 운영진 역할을 반환합니다. 총괄단 및 중앙운영진은 CENTRAL_MEMBER(레벨 1)로 통합됩니다. 여러 역할을 가진 경우 레벨이 가장 낮은(상위) 역할을 반환합니다.
     */
    private NoticeTab resolveViewerRole(Long memberId, Long gisuId) {
        if (memberId == null || gisuId == null) {
            return null;
        }
        if (getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            return NoticeTab.CENTRAL_MEMBER;
        }

        return getChallengerRoleUseCase.findAllByMemberId(memberId).stream()
            .filter(role -> gisuId.equals(role.gisuId()))
            .filter(role -> role.roleType().isAtLeastCentralMember()
                || role.roleType().isAtLeastSchoolAdmin())
            .map(role -> {
                if (role.roleType().isAtLeastCentralMember()) {
                    return NoticeTab.CENTRAL_MEMBER;
                }
                return NoticeTab.findFrom(role.roleType()).orElse(null);
            })
            .filter(role -> role != null && role.isStaffRole())
            .min(Comparator.comparingInt(NoticeTab::getLevel))
            .orElse(null);
    }
}
