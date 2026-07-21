package com.umc.product.schedule.application.service.query;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.schedule.application.port.in.query.GetScheduleUseCase;
import com.umc.product.schedule.application.port.in.query.dto.AdminScheduleInfo;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleBaseInfo;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleInfo;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.application.port.out.dto.ScheduleParticipantDetailDto;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleQueryService implements GetScheduleUseCase {

    private final LoadSchedulePort loadSchedulePort;
    private final LoadScheduleParticipantPort loadScheduleParticipantPort;

    // 역할 기반 필터링을 위한 외부 도메인 UseCase
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final GetGisuUseCase getGisuUseCase;

    // 내 일정 조회
    @Override
    public List<ScheduleInfo> searchMySchedules(
        Instant from,
        Instant to,
        Boolean isAttendanceRequired,
        Long memberId
    ) {
        // 사용자가 참여하는 일정 조회
        List<Schedule> schedules = loadSchedulePort.findMySchedules(memberId, from, to, isAttendanceRequired);

        if (schedules.isEmpty()) {
            return List.of();
        }

        // 조회된 일정들의 Id 추출
        List<Long> scheduleIds = schedules.stream()
            .map(Schedule::getId)
            .toList();

        // 해당 일정들의 참여자 전체 조회
        // 참여자 상세 정보 일괄 조회 후 scheduleId 기준으로 그룹화
        Map<Long, List<ScheduleParticipantDetailDto>> participantsMap = getParticipantsMap(scheduleIds);

        // 도메인 -> List<ScheduleInfo> 매핑
        return schedules.stream()
            .map(schedule -> {
                // 순회 중인 scheduleId를 key로 하여 참여자 목록 꺼냄, 참여자가 없으면 빈 리스트 반환
                List<ScheduleParticipantDetailDto> participants =
                    participantsMap.getOrDefault(schedule.getId(), List.of());

                return ScheduleInfo.from(schedule, participants, memberId);
            }).toList();
    }

    // 일정 상세 조회
    @Override
    public ScheduleInfo getScheduleDetails(Long scheduleId, Long memberId) {

        Schedule schedule = loadSchedulePort.findByIdWithTags(scheduleId)
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        List<ScheduleParticipantDetailDto> participants = loadScheduleParticipantPort.findParticipantDetailsByScheduleId(
            scheduleId);

        return ScheduleInfo.from(schedule, participants, memberId);
    }

    // [운영진용] 기간 기반 일정들의 출석 현황 조회
    // 역할별 조회 범위
    // - 중앙 총괄단/운영진 : 본인 참석 일정
    // - 학교 회장단 : 본인 생성 일정 + 교내 인원이 포함된 스터디 그룹 일정 + 교내 파트장이 멘토인 스터디 그룹 일정
    // - 교내 파트장 : 본인 생성 일정 + 본인 멘토 스터디 그룹 일정
    // - 기타 운영진 : 본인 생성 일정
    // 조회 기간과 무관하게 과거 일정 중에서 출석을 승인하지 않은 일정은 계속 표시됨.
    @Override
    public List<AdminScheduleInfo> searchAdminSchedules(Instant from, Instant to,
                                                        AttendanceStatus attendanceStatus,
                                                        Long memberId) {
        // 역할 기반 조회 대상 일정 ID 수집
        Set<Long> targetScheduleIds = collectTargetScheduleIds(memberId);

        if (targetScheduleIds.isEmpty()) {
            return List.of();
        }

        // 일정 조회 (기간 필터 + 승인 대기 일정 포함 + 출석 상태 필터)
        List<Schedule> schedules = loadSchedulePort.findAdminSchedulesByRole(
            targetScheduleIds, from, to, attendanceStatus
        );

        if (schedules.isEmpty()) {
            return List.of();
        }

        List<Long> scheduleIds = schedules.stream().map(Schedule::getId).toList();

        // 3. 해당 일정들의 참여자 전체 조회
        Map<Long, List<ScheduleParticipantDetailDto>> participantsMap = getParticipantsMap(scheduleIds);

        return schedules.stream()
            .map(schedule -> {
                List<ScheduleParticipantDetailDto> participants =
                    participantsMap.getOrDefault(schedule.getId(), List.of());

                // 출석 상태 필터링 (참여자 레벨)
                List<ScheduleParticipantDetailDto> filteredDtos = participants.stream()
                    .filter(p -> attendanceStatus == null || p.attendanceStatus() == attendanceStatus)
                    .toList();

                return AdminScheduleInfo.from(schedule, filteredDtos);
            })
            // 필터링 결과, 보여줄 참여자가 없는 일정은 목록에서 제외
            .filter(adminInfo -> !adminInfo.participants().isEmpty())
            .toList();
    }

    @Override
    public AdminScheduleInfo getAdminSchedule(Long scheduleId, Long memberId,
                                              AttendanceStatus attendanceStatus) {

        // 존재하지 않는 일정이면 에러 반환
        Schedule schedule = loadSchedulePort.findByIdWithTags(scheduleId)
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        // 일정의 참여자가 아니면 에러 반환
        loadScheduleParticipantPort.findByScheduleIdAndMemberId(scheduleId, memberId)
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.NOT_SCHEDULE_PARTICIPANT));

        List<ScheduleParticipantDetailDto> participants =
            loadScheduleParticipantPort.findParticipantDetailsByScheduleIdAndStatus(scheduleId, attendanceStatus);

        return AdminScheduleInfo.from(schedule, participants);
    }

    // 일정이 출석 정책을 가졌는지 여부를 리턴
    @Override
    public boolean hasAttendancePolicy(Long scheduleId) {
        Schedule schedule = loadSchedulePort.findById(scheduleId)
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        return schedule.getPolicy() != null;
    }

    @Override
    public ScheduleBaseInfo getScheduleBaseInfo(Long scheduleId) {

        Schedule schedule = loadSchedulePort.findById(scheduleId)
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        return ScheduleBaseInfo.from(schedule);
    }

    // ============================= Helper Method ===================================

    // 일정들의 참여자 상세 정보 일괄 조회 후, scheduleId 기준으로 그룹화 하는 메서드
    private Map<Long, List<ScheduleParticipantDetailDto>> getParticipantsMap(List<Long> scheduleIds) {

        List<ScheduleParticipantDetailDto> allParticipants =
            loadScheduleParticipantPort.findParticipantDetailsByScheduleIds(scheduleIds);

        return allParticipants.stream()
            .collect(Collectors.groupingBy(ScheduleParticipantDetailDto::scheduleId));
    }


    // 역할 기반으로 조회 대상 일정 ID를 수집하는 메서드
    // 여러 역할을 가진 경우 합집합으로 처리
    private Set<Long> collectTargetScheduleIds(Long memberId) {
        Set<Long> targetScheduleIds = new HashSet<>();
        if (getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            targetScheduleIds.addAll(loadScheduleParticipantPort.findScheduleIdsByMemberId(memberId));
            return targetScheduleIds;
        }

        // 현재 활성 기수 조회
        Long activeGisuId = getGisuUseCase.getActiveGisu().gisuId();

        // 현재 기수의 역할 조회
        List<ChallengerRoleInfo> currentGisuRoles = getChallengerRoleUseCase.findAllByMemberId(memberId).stream()
            .filter(role -> role.gisuId().equals(activeGisuId))
            .toList();

        for (ChallengerRoleInfo role : currentGisuRoles) {
            ChallengerRoleType roleType = role.roleType();

            // 중앙 총괄단/운영진 : 본인 참석 일정
            if (roleType.isAtLeastCentralMember()) {
                targetScheduleIds.addAll(loadScheduleParticipantPort.findScheduleIdsByMemberId(memberId));
            }
            // 학교 회장단 : 본인 생성 일정 + 교내 인원이 포함된 스터디 그룹 일정 + 교내 파트장이 멘토인 스터디 그룹 일정
            else if (roleType.isAtLeastSchoolCore()) {
                targetScheduleIds.addAll(loadSchedulePort.findScheduleIdsByAuthor(memberId));
                // TODO : Organization UseCase로 교내 스터디 그룹 일정 ID 조회
            }
            // 교내 파트장: 본인 생성 일정 + 본인 멘토 스터디 그룹 일정
            else if (roleType == ChallengerRoleType.SCHOOL_PART_LEADER) {
                targetScheduleIds.addAll(loadSchedulePort.findScheduleIdsByAuthor(memberId));
                // TODO : Organization UseCase로 본인 멘토 스터디 그룹 일정 ID 조회
            }
            // 기타 운영진 : 본인 생성 일정
            else if (roleType == ChallengerRoleType.SCHOOL_ETC_ADMIN) {
                targetScheduleIds.addAll(loadSchedulePort.findScheduleIdsByAuthor(memberId));
            }
        }

        return targetScheduleIds;
    }
}
