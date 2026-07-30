package com.umc.product.recruiting.application.service.command;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.ListChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.recruiting.application.port.out.SaveRecruitingDecisionHistoryPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingDecisionHistory;

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
    private final SaveRecruitingDecisionHistoryPort saveDecisionHistoryPort;

    public void record(RecruitingApplication application, Long decidedByMemberId) {
        Long gisuId = application.getRound().getSeason().getGisuId();
        Long schoolId = application.getRound().getSeason().getSchoolId();
        ChallengerRoleType deciderRoleType = resolveDeciderRoleType(decidedByMemberId, gisuId, schoolId);
        saveDecisionHistoryPort.save(
            RecruitingDecisionHistory.create(application, decidedByMemberId, deciderRoleType)
        );
    }

    private ChallengerRoleType resolveDeciderRoleType(Long memberId, Long gisuId, Long schoolId) {
        List<ChallengerRoleInfo> roles = listChallengerRoleUseCase.listByMemberIdAndGisuId(memberId, gisuId);
        Optional<ChallengerRoleType> schoolCoreRole = roles.stream()
            .filter(role -> role.roleType().isAtLeastSchoolCore()
                && Objects.equals(role.organizationId(), schoolId))
            .map(ChallengerRoleInfo::roleType)
            .min(Comparator.comparingInt(Enum::ordinal));
        return schoolCoreRole.orElseGet(() -> roles.stream()
            .filter(role -> role.roleType().isAtLeastCentralMember())
            .map(ChallengerRoleInfo::roleType)
            .min(Comparator.comparingInt(Enum::ordinal))
            .orElse(null));
    }
}
