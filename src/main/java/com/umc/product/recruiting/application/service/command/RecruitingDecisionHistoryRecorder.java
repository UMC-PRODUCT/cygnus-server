package com.umc.product.recruiting.application.service.command;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.ListChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.recruiting.application.port.out.SaveRecruitingDecisionHistoryPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingDecisionHistory;
import com.umc.product.recruiting.domain.RecruitingDecisionHistoryDeciderSnapshot;

import lombok.RequiredArgsConstructor;

/**
 * 판정 커맨드와 같은 트랜잭션에서 판정 이력을 기록합니다.
 * <p>
 * 담당자의 직위는 판정 권한 우선순위와 같은 순서로 스냅샷합니다:
 * 지원서 학교의 회장단 -> 중앙운영사무국 역할 -> 없음(SUPER_ADMIN 등).
 */
@Component
@RequiredArgsConstructor
public class RecruitingDecisionHistoryRecorder {

    private final ListChallengerRoleUseCase listChallengerRoleUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetSchoolUseCase getSchoolUseCase;
    private final SaveRecruitingDecisionHistoryPort saveDecisionHistoryPort;

    public void record(RecruitingApplication application, Long decidedByMemberId) {
        Long gisuId = application.getRound().getSeason().getGisuId();
        Long schoolId = application.getRound().getSeason().getSchoolId();
        ChallengerRoleInfo deciderRole = resolveDeciderRole(decidedByMemberId, gisuId, schoolId);
        MemberInfo decider = getMemberUseCase.getById(decidedByMemberId);
        saveDecisionHistoryPort.save(
            RecruitingDecisionHistory.create(application, toDeciderSnapshot(decidedByMemberId, deciderRole, decider))
        );
    }

    private ChallengerRoleInfo resolveDeciderRole(Long memberId, Long gisuId, Long schoolId) {
        List<ChallengerRoleInfo> roles = listChallengerRoleUseCase.listByMemberIdAndGisuId(memberId, gisuId);
        Optional<ChallengerRoleInfo> schoolCoreRole = roles.stream()
            .filter(role -> role.roleType().isAtLeastSchoolCore()
                && Objects.equals(role.organizationId(), schoolId))
            .min(Comparator.comparingInt(role -> role.roleType().ordinal()));
        return schoolCoreRole.orElseGet(() -> roles.stream()
            .filter(role -> role.roleType().isAtLeastCentralMember())
            .min(Comparator.comparingInt(role -> role.roleType().ordinal()))
            .orElse(null));
    }

    private RecruitingDecisionHistoryDeciderSnapshot toDeciderSnapshot(
        Long decidedByMemberId,
        ChallengerRoleInfo deciderRole,
        MemberInfo decider
    ) {
        SchoolDetailInfo school = deciderRole != null && deciderRole.roleType().isAtLeastSchoolCore()
            ? getSchoolUseCase.getSchoolDetail(deciderRole.organizationId())
            : null;
        return RecruitingDecisionHistoryDeciderSnapshot.builder()
            .memberId(decidedByMemberId)
            .chapterId(school == null ? null : school.chapterId())
            .chapterName(school == null ? null : school.chapterName())
            .schoolId(school == null ? null : school.schoolId())
            .schoolName(school == null ? null : school.schoolName())
            .roleType(deciderRole == null ? null : deciderRole.roleType())
            .name(decider.name())
            .nickname(decider.nickname())
            .build();
    }
}
