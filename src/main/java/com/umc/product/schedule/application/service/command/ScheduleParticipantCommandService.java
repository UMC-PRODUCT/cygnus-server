package com.umc.product.schedule.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.util.GeometryUtils;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.schedule.application.port.in.command.CreateScheduleParticipantUseCase;
import com.umc.product.schedule.application.port.in.command.UpdateScheduleParticipantUseCase;
import com.umc.product.schedule.application.port.in.command.dto.DecideAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.ExcuseScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.ScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.result.ScheduleParticipantAttendanceResult;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.application.port.out.SaveScheduleParticipantPort;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.ScheduleParticipantAttendance;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleParticipantCommandService implements
    CreateScheduleParticipantUseCase,
    UpdateScheduleParticipantUseCase {

    private final LoadSchedulePort loadSchedulePort;

    private final SaveScheduleParticipantPort saveScheduleParticipantPort;
    private final LoadScheduleParticipantPort loadScheduleParticipantPort;

    private final ScheduleAttendanceAuditRecorder auditRecorder;

    private static void checkSchedulePolicyExists(Schedule schedule) {
        if (schedule.getPolicy() == null) {
            throw new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_ATTENDANCE_POLICY_NOT_EXIST);
        }
    }

    // 출석 요청
    @Override
    public ScheduleParticipantAttendanceResult createScheduleParticipantWithAttendance(
        ScheduleAttendanceCommand command) {

        Schedule schedule = loadSchedulePort.findById(command.scheduleId())
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        // 출석을 요하지 않는, 즉 출석 정책이 없는 일정이면 에러 반환
        checkSchedulePolicyExists(schedule);

        // ScheduleParticipant 정보 가져오기
        ScheduleParticipant scheduleParticipant = getScheduleParticipant(
            command.scheduleId(),
            command.requesterMemberId()
        );

        // 클라이언트에서 값을 안 주면 null (비대면인 경우를 고려)
        Point location = getLocation(command.latitude(), command.longitude());

        // scheduleParticipant에 연결되는 ScheduleParticipantAttendance를 저장
        // 이미 출석 요청 기록이 있으면 에러 반환
        // 이미 종료된 일정에 요청, 출석 시작 전 요청은 에러 반환
        scheduleParticipant.createAttendance(location, command.locationVerified());

        // 요청 저장
        saveScheduleParticipantPort.save(scheduleParticipant);

        ScheduleParticipantAttendance attendance = scheduleParticipant.getAttendance();
        Point savedLocation = attendance.getLocation();

        auditRecorder.recordSelf(
            schedule,
            command.requesterMemberId(),
            attendance.getStatus(),
            AuditAction.CHECK
        );

        return ScheduleParticipantAttendanceResult.builder()
            .latitude(savedLocation != null ? savedLocation.getY() : null)
            .longitude(savedLocation != null ? savedLocation.getX() : null)
            .status(attendance.getStatus())
            .excuseReason(attendance.getExcuseReason())
            .isPendingDecision(attendance.getStatus().isPending())
            // 의사 결정자가 개입하지 않은 최초 출석 요청이므로 false/null 처리
            .hasDecisionMakerMember(false)
            .decisionMakerMemberInfo(null)
            .decidedAt(attendance.getDecidedAt())
            .decisionReason(attendance.getDecisionReason())
            .build();
    }

    // 사유 제출
    @Override
    public ScheduleParticipantAttendanceResult createExcusedScheduleParticipantWithAttendance(
        ExcuseScheduleAttendanceCommand command) {

        Schedule schedule = loadSchedulePort.findById(command.scheduleId())
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        // 출석을 요하지 않는, 즉 출석 정책이 없는 일정이면 에러 반환
        checkSchedulePolicyExists(schedule);

        // ScheduleParticipant 정보 가져오기
        ScheduleParticipant scheduleParticipant = getScheduleParticipant(
            command.scheduleId(),
            command.requesterMemberId()
        );

        // 클라이언트에서 값을 안 주면 null
        Point location = getLocation(command.latitude(), command.longitude());

        // scheduleParticipant에 연결되는 ScheduleParticipantAttendance를 저장
        // 이미 출석 요청 기록이 있으면 에러 반환
        // 이미 종료된 일정에 요청, 출석 시작 전 요청은 에러 반환
        scheduleParticipant.submitExcuse(location, command.isVerified(), command.excuseReason());

        // 요청 저장
        saveScheduleParticipantPort.save(scheduleParticipant);

        // attendance 에서 데이터를 꺼내 DTO 직접 매핑
        ScheduleParticipantAttendance attendance = scheduleParticipant.getAttendance();
        Point savedLocation = attendance.getLocation();

        auditRecorder.recordSelf(
            schedule,
            command.requesterMemberId(),
            attendance.getStatus(),
            AuditAction.SUBMIT
        );

        // decisionMakerMember는 null
        return ScheduleParticipantAttendanceResult.builder()
            .latitude(savedLocation != null ? savedLocation.getY() : null)
            .longitude(savedLocation != null ? savedLocation.getX() : null)
            .status(attendance.getStatus())
            .excuseReason(attendance.getExcuseReason())
            .isPendingDecision(attendance.getStatus().isPending())
            .hasDecisionMakerMember(false)
            .decisionMakerMemberInfo(null)
            .decidedAt(attendance.getDecidedAt())
            .decisionReason(attendance.getDecisionReason())
            .build();
    }

    // 출석 요청 승인/거절
    @Audited(
        domain = Domain.SCHEDULE,
        action = AuditAction.CHECK,
        targetType = "ScheduleAttendance",
        description = "'일정 출석 요청을 처리했습니다.'"
    )
    @Override
    public List<ScheduleParticipantAttendanceResult> decideAttendances(List<DecideAttendanceCommand> commands) {
        if (commands.isEmpty()) {
            return List.of();
        }

        List<ProcessedDecision> processedDecisions = commands.stream()
            .map(this::processDecision) // 단일 command에 대해 출석 요청 승인/거절 로직 수행
            .toList();

        Map<Long, MemberInfo> members = auditRecorder.recordDecisions(processedDecisions.stream()
            .map(this::toAuditDecision)
            .toList());

        List<ScheduleParticipantAttendanceResult> results = new ArrayList<>(processedDecisions.size());
        for (ProcessedDecision processed : processedDecisions) {
            DecideAttendanceCommand command = processed.command();
            MemberInfo decisionMaker = members.get(command.decidedByMemberId());

            results.add(toDecisionResult(processed.attendance(), decisionMaker));
        }
        return List.copyOf(results);
    }

    private ScheduleAttendanceAuditRecorder.Decision toAuditDecision(ProcessedDecision processed) {
        DecideAttendanceCommand command = processed.command();
        return ScheduleAttendanceAuditRecorder.Decision.of(
            processed.schedule(),
            command.decidedByMemberId(),
            command.participantMemberId(),
            processed.attendance().getStatus(),
            command.isApproved() ? AuditAction.APPROVE : AuditAction.REJECT
        );
    }

    // 출석 요청 승인/거절 로직
    private ProcessedDecision processDecision(DecideAttendanceCommand command) {
        Schedule schedule = loadSchedulePort.findById(command.scheduleId())
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        // 출석을 요하지 않는, 즉 출석 정책이 없는 일정이면 에러 반환
        checkSchedulePolicyExists(schedule);

        // ScheduleParticipant 정보가 없으면 에러 반환
        ScheduleParticipant scheduleParticipant = getScheduleParticipant(
            command.scheduleId(),
            command.participantMemberId()
        );

        // 승인 or 거절로 현재 출석 상태에 맞는 status로 업데이트
        if (command.isApproved()) {
            scheduleParticipant.approveAttendance(command.decidedByMemberId(), command.reason());
        } else {
            scheduleParticipant.rejectAttendance(command.decidedByMemberId(), command.reason());
        }
        saveScheduleParticipantPort.save(scheduleParticipant);

        ScheduleParticipantAttendance attendance = scheduleParticipant.getAttendance();
        return new ProcessedDecision(command, ScheduleAuditEventFactory.snapshot(schedule), attendance);
    }

    private ScheduleParticipantAttendanceResult toDecisionResult(
        ScheduleParticipantAttendance attendance,
        MemberInfo decisionMaker
    ) {
        Point savedLocation = attendance.getLocation();

        return ScheduleParticipantAttendanceResult.builder()
            .latitude(savedLocation != null ? savedLocation.getY() : null)
            .longitude(savedLocation != null ? savedLocation.getX() : null)
            .status(attendance.getStatus())
            .excuseReason(attendance.getExcuseReason())
            .isPendingDecision(attendance.getStatus().isPending())
            .hasDecisionMakerMember(decisionMaker != null)
            .decisionMakerMemberInfo(
                decisionMaker != null ?
                    ScheduleParticipantAttendanceResult.DecisionMakerMemberInfo.builder()
                    .memberId(decisionMaker.id())
                    .name(decisionMaker.name())
                    .nickname(decisionMaker.nickname())
                    .schoolId(decisionMaker.schoolId())
                    .schoolName(decisionMaker.schoolName())
                    .build()
                    : null
            )
            .decidedAt(attendance.getDecidedAt())
            .decisionReason(attendance.getDecisionReason())
            .build();
    }

    private ScheduleParticipant getScheduleParticipant(Long scheduleId, Long memberId) {
        return loadScheduleParticipantPort
            .findByScheduleIdAndMemberId(scheduleId, memberId)
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.PARTICIPANT_NOT_FOUND));
    }

    private Point getLocation(Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            return GeometryUtils.createPoint(latitude, longitude);
        }
        return null;
    }

    private record ProcessedDecision(
        DecideAttendanceCommand command,
        ScheduleAuditEventFactory.ScheduleSnapshot schedule,
        ScheduleParticipantAttendance attendance
    ) {
    }
}
