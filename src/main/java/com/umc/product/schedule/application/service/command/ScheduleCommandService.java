package com.umc.product.schedule.application.service.command;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfoWithStatus;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.util.GeometryUtils;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.schedule.application.port.in.command.CreateScheduleUseCase;
import com.umc.product.schedule.application.port.in.command.DeleteScheduleUseCase;
import com.umc.product.schedule.application.port.in.command.UpdateScheduleUseCase;
import com.umc.product.schedule.application.port.in.command.dto.CreateScheduleCommand;
import com.umc.product.schedule.application.port.in.command.dto.EditScheduleCommand;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleCapabilitiesInfo;
import com.umc.product.schedule.application.port.out.DeleteScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.DeleteSchedulePort;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.application.port.out.SaveScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.SaveSchedulePort;
import com.umc.product.schedule.application.service.query.ScheduleCapabilitiesService;
import com.umc.product.schedule.domain.AttendancePolicy;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleCommandService implements CreateScheduleUseCase, UpdateScheduleUseCase, DeleteScheduleUseCase {

    private final SaveSchedulePort saveSchedulePort;
    private final LoadSchedulePort loadSchedulePort;
    private final DeleteSchedulePort deleteSchedulePort;

    private final SaveScheduleParticipantPort saveScheduleParticipantPort;
    private final DeleteScheduleParticipantPort deleteScheduleParticipantPort;
    private final LoadScheduleParticipantPort loadScheduleParticipantPort;

    // 일정 생성 권한 판별 service
    private final ScheduleCapabilitiesService capabilitiesService;

    // 외부 도메인 UseCase
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetMemberUseCase getMemberUseCase;


    // 일정 생성
    @Audited(
        domain = Domain.SCHEDULE,
        action = AuditAction.CREATE,
        targetType = "Schedule",
        targetId = "#result",
        description = "'일정을 생성했습니다.'"
    )
    @Override
    public Long create(CreateScheduleCommand command) {

        // 사용자 capabilities 조회
        ScheduleCapabilitiesInfo capabilitiesInfo = capabilitiesService.getCapabilities(command.authorMemberId());

        // 검증 (챌린저 활동 이력, 초대 가능 최대 참여자 수, 출석 정책 생성)
        if (!capabilitiesInfo.canCreateSchedule()) {
            throw new ScheduleDomainException(ScheduleErrorCode.CANNOT_CREATE_SCHEDULE);
        }
        if (command.participantMemberIds().size() > capabilitiesInfo.maxParticipantCount()) {
            throw new ScheduleDomainException(ScheduleErrorCode.EXCEEDED_MAX_PARTICIPANTS,
                capabilitiesInfo.maxParticipantCount() + "명까지 초대 가능합니다.");
        }
        if (command.attendancePolicy() != null && !capabilitiesInfo.canCreateAttendanceRequiredSchedule()) {
            throw new ScheduleDomainException(ScheduleErrorCode.CANNOT_CREATE_ATTENDANCE_REQUIRED_SCHEDULE);
        }

        // 작성자의 가장 최근 기수 Challenger 상태 조회 (탈부, 제명 상태일 시 exeption)
        ChallengerInfoWithStatus challengerInfoWithStatus = getChallengerUseCase.getLatestActiveChallengerByMemberId(
            command.authorMemberId());

        // 생성하려는 일정의 날짜가 현재 기수 활동 기간에서 벗어난 경우 에러 반환
        GisuInfo activeGisu = getGisuUseCase.getActiveGisu();
        if (command.startsAt().isBefore(activeGisu.startAt()) || command.endsAt().isAfter(activeGisu.endAt())) {
            throw new ScheduleDomainException(ScheduleErrorCode.NOT_ACTIVE_GISU_SCHEDULE);
        }

        // 초대하려는 MemberId들이 실제로 존재하는지 검증
        Set<Long> participants = command.participantMemberIds();
        if (participants != null && !participants.isEmpty()) {
            long validMemberCount = getMemberUseCase.countMembersByIds(participants);

            // 요청한 id의 개수와 DB에 실제 존재하는 id의 개수가 다르다면 검증 실패
            if (validMemberCount != participants.size()) {
                throw new ScheduleDomainException(ScheduleErrorCode.INVALID_MEMBER_INVITE);
            }
        }

        // Schedule 생성 및 저장
        Schedule schedule = command.toEntity(challengerInfoWithStatus.memberId());
        Schedule savedSchedule = saveSchedulePort.save(schedule);

        // ScheduleParticipant 생성 및 저장
        List<ScheduleParticipant> participantList = participants.stream()
            .map(memberId -> ScheduleParticipant.builder()
                .memberId(memberId)
                .schedule(savedSchedule)
                .attendance(null)
                .build())
            .toList();

        saveScheduleParticipantPort.saveAll(participantList);

        return savedSchedule.getId();
    }

    // 일정 수정
    @Audited(
        domain = Domain.SCHEDULE,
        action = AuditAction.UPDATE,
        targetType = "Schedule",
        targetId = "#command.scheduleId()",
        description = "'일정을 수정했습니다.'"
    )
    @Override
    public Long update(EditScheduleCommand command) {

        Schedule schedule = loadSchedulePort.findById(command.scheduleId())
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        command.validate();

        if (Instant.now().isAfter(schedule.getStartsAt())) {
            throw new ScheduleDomainException(ScheduleErrorCode.STARTED_SCHEDULE_CANT_BE_EDITED);
        }

        // capabilities 검증이 필요한 경우에만 조회 (출석 정책이 생겼거나 참여자에 변동이 있을 경우)
        boolean needsCapabilitiesCheck = command.isChangingToAttendanceRequired()
            || command.isParticipantsUpdateRequested();

        if (needsCapabilitiesCheck) {
            ScheduleCapabilitiesInfo capabilitiesInfo = capabilitiesService.getCapabilities(
                schedule.getAuthorMemberId());

            // 출석 정책 추가 권한 검증
            if (command.isChangingToAttendanceRequired()
                && !capabilitiesInfo.canCreateAttendanceRequiredSchedule()) {
                throw new ScheduleDomainException(ScheduleErrorCode.CANNOT_CREATE_ATTENDANCE_REQUIRED_SCHEDULE);
            }

            // 최대 참여자 수 검증
            if (command.isParticipantsUpdateRequested()
                && command.participantMemberIds().size() > capabilitiesInfo.maxParticipantCount()) {
                throw new ScheduleDomainException(ScheduleErrorCode.EXCEEDED_MAX_PARTICIPANTS,
                    capabilitiesInfo.maxParticipantCount() + "명까지 초대 가능합니다.");
            }
        }

        // 대면 -> 비대면 전환 시
        if (command.isChangingToOnline()) {
            schedule.convertToOnline();
        }

        AttendancePolicy newPolicy = null;
        // 출석 O -> 출석 X 전환 시
        if (Boolean.FALSE.equals(command.isAttendanceRequired())) {
            schedule.removeAttendancePolicy();
        } else {
            // 출석 해제가 아닐 때만 프론트가 보낸 데이터 파싱
            newPolicy = createPolicyFromCommand(command, schedule);
        }

        // 일반 필드 업데이트 (대면/비대면 유지 또는 비대면 -> 대면 포함)
        schedule.update(
            command.name(),
            command.description(),
            command.tags(),
            command.startsAt(),
            command.endsAt(),
            extractLocationName(command),
            extractLocation(command),
            newPolicy
        );

        if (command.isParticipantsUpdateRequested()) {
            // DB에 있는 기존 참여자 ID 목록을 가져옴
            Set<Long> existingParticipantIds = loadScheduleParticipantPort.findMemberIdsByScheduleId(schedule.getId());

            // 진짜 명단이 달라졌는지 비교
            if (!existingParticipantIds.equals(command.participantMemberIds())) {
                // 진짜 달라졌을 때만 업데이트 수행
                updateParticipants(schedule, command);
            }
        }

        // 일정 update
        saveSchedulePort.save(schedule);

        return schedule.getId();
    }

    // 일정 삭제
    // 출석 기록이 존재하면 삭제 불가
    @Audited(
        domain = Domain.SCHEDULE,
        action = AuditAction.DELETE,
        targetType = "Schedule",
        targetId = "#scheduleId",
        description = "'일정을 삭제했습니다.'"
    )
    @Override
    public void delete(Long scheduleId) {
        Schedule schedule = loadSchedulePort.findById(scheduleId)
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        if (loadScheduleParticipantPort.existsAttendanceStatusByScheduleId(scheduleId)) {
            throw new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_HAS_ATTENDANCE_RECORD);
        }

        deleteScheduleWithParticipants(schedule);
    }

    // 일정 강제 삭제
    // 출석 기록이 존재해도 삭제 가능
    @Audited(
        domain = Domain.SCHEDULE,
        action = AuditAction.DELETE,
        targetType = "Schedule",
        targetId = "#scheduleId",
        description = "'일정을 강제로 삭제했습니다.'"
    )
    @Override
    public void forceDelete(Long scheduleId) {
        Schedule schedule = loadSchedulePort.findById(scheduleId)
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        deleteScheduleWithParticipants(schedule);
    }

    // ============================== Helper Methods ==============================

    // 일정에 연결된 모든 ScheduleParticipant 및 Schedule을 삭제
    // 참여자는 단일 벌크 DELETE 쿼리로 삭제하여, 대규모 일정에서도 메모리/네트워크 부하를 최소화
    private void deleteScheduleWithParticipants(Schedule schedule) {
        deleteScheduleParticipantPort.deleteByScheduleId(schedule.getId());
        deleteSchedulePort.delete(schedule.getId());
    }


    // command로부터 policy 생성
    private AttendancePolicy createPolicyFromCommand(EditScheduleCommand command, Schedule schedule) {
        if (command.attendancePolicy() == null) {
            return null;
        }

        // 시간이 변경되었으면 새 값, 아니면 기존 값 사용
        Instant startsAt = command.startsAt() != null ? command.startsAt() : schedule.getStartsAt();
        Instant endsAt = command.endsAt() != null ? command.endsAt() : schedule.getEndsAt();

        return Schedule.createAttendancePolicy(
            command.attendancePolicy().checkInStartAt(),
            command.attendancePolicy().onTimeEndAt(),
            command.attendancePolicy().lateEndAt(),
            startsAt,
            endsAt
        );
    }

    private String extractLocationName(EditScheduleCommand command) {
        return command.location() != null ? command.location().locationName() : null;
    }

    private Point extractLocation(EditScheduleCommand command) {
        if (command.location() == null) {
            return null;
        }
        return GeometryUtils.createPoint(
            command.location().latitude(),
            command.location().longitude()
        );
    }

    // 참여자 업데이트
    private void updateParticipants(Schedule schedule, EditScheduleCommand command) {
        Set<Long> newMemberIds = command.participantMemberIds();

        // 기존 참여자 조회
        List<ScheduleParticipant> existingParticipants = loadScheduleParticipantPort.findAllByScheduleId(
            schedule.getId());
        Set<Long> existingMemberIds = existingParticipants.stream()
            .map(ScheduleParticipant::getMemberId)
            .collect(Collectors.toSet());

        // 삭제되어야 할 MemberId
        Set<Long> toDeleteIds = new HashSet<>(existingMemberIds);
        toDeleteIds.removeAll(newMemberIds);

        // 추가되어야 할 MemberId
        Set<Long> toAddIds = new HashSet<>(newMemberIds);
        toAddIds.removeAll(existingMemberIds);

        // 추가되어야 할 MemberId들이 실제로 존재하는지 검증
        if (!toAddIds.isEmpty()) {
            long validMemberCount = getMemberUseCase.countMembersByIds(toAddIds);

            if (validMemberCount != toAddIds.size()) {
                throw new ScheduleDomainException(ScheduleErrorCode.INVALID_MEMBER_INVITE);
            }
        }

        // 삭제
        if (!toDeleteIds.isEmpty()) {
            List<ScheduleParticipant> participantsToDelete = existingParticipants.stream()
                .filter(p -> toDeleteIds.contains(p.getMemberId()))
                .toList();

            deleteScheduleParticipantPort.deleteAll(participantsToDelete);
        }

        // 추가
        if (!toAddIds.isEmpty()) {
            List<ScheduleParticipant> participantsToAdd = toAddIds.stream()
                .map(memberId -> ScheduleParticipant.builder()
                    .memberId(memberId)
                    .schedule(schedule)
                    .attendance(null)
                    .build())
                .toList();

            saveScheduleParticipantPort.saveAll(participantsToAdd);
        }
    }
}
