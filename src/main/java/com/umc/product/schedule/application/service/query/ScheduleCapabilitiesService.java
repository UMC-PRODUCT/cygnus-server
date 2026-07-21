package com.umc.product.schedule.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.schedule.application.port.in.query.GetScheduleCapabilitiesUseCase;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleCapabilitiesInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleCapabilitiesService implements GetScheduleCapabilitiesUseCase {

    private final GetChallengerUseCase getChallengerUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Override
    public ScheduleCapabilitiesInfo getCapabilities(Long memberId) {
        if (getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            return ScheduleCapabilitiesInfo.forCentralCore();
        }

        // 챌린저 활동 기록이 없으면 일정 생성 불가
        if (getChallengerUseCase.getAllByMemberId(memberId).isEmpty()) {
            return ScheduleCapabilitiesInfo.notAllowed();
        }

        // 현재 활성 기수 조회
        Long activeGisuId = getGisuUseCase.getActiveGisu().gisuId();

        // 현재 기수의 역할 조회
        List<ChallengerRoleInfo> currentGisuRoles = getChallengerRoleUseCase.findAllByMemberId(memberId).stream()
            .filter(role -> role.gisuId().equals(activeGisuId))
            .toList();

        // 역할이 없으면 일반 챌린저
        if (currentGisuRoles.isEmpty()) {
            return ScheduleCapabilitiesInfo.forChallenger();
        }

        // 가장 높은 권한 기준으로 capabilities 반환
        return determineCapabilities(currentGisuRoles);
    }


    // 역할 목록에서 가장 높은 권한을 기준으로 capabilities 반환
    // 우선순위 : CentralCore > CentralMember > ChapterPresident > SchoolCore > SchoolAdmin > Challenger
    private ScheduleCapabilitiesInfo determineCapabilities(List<ChallengerRoleInfo> roles) {

        int highestPriority = Integer.MAX_VALUE;

        for (ChallengerRoleInfo role : roles) {
            int priority = getRolePriority(role.roleType());
            if (priority < highestPriority) {
                highestPriority = priority;
            }
        }

        return mapPriorityToCapabilities(highestPriority);
    }

    // 역할의 우선순위 반환 (낮을수록 높은 권한, maxParticipantCount 기준)
    private int getRolePriority(ChallengerRoleType roleType) {

        if (roleType.isAtLeastCentralCore()) { // 중앙 총괄단
            return 1; // 2000명
        }
        if (roleType.isAtLeastCentralMember()) { // 중앙 운영진
            return 2; // 300명
        }
        if (roleType == ChallengerRoleType.CHAPTER_PRESIDENT) { // 지부장
            return 3; // 300명
        }
        if (roleType.isAtLeastSchoolCore()) { // 교내 회장단
            return 4; // 100명
        }
        if (roleType.isAtLeastSchoolAdmin()) { // 교내 파트장, 교내 기타 운영진
            return 5; // 100명
        }
        return Integer.MAX_VALUE;
    }


    // 우선순위를 capabilities로 매핑
    private ScheduleCapabilitiesInfo mapPriorityToCapabilities(int priority) {

        return switch (priority) {
            case 1 -> ScheduleCapabilitiesInfo.forCentralCore();       // 2000명
            case 2 -> ScheduleCapabilitiesInfo.forCentralMember();     // 300명
            case 3 -> ScheduleCapabilitiesInfo.forChapterPresident();  // 300명
            case 4 -> ScheduleCapabilitiesInfo.forSchoolCore();        // 100명
            case 5 -> ScheduleCapabilitiesInfo.forSchoolAdmin();       // 100명
            default -> ScheduleCapabilitiesInfo.forChallenger();       // 50명
        };
    }
}
