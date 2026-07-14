package com.umc.product.schedule.application.port.in.command;

public interface DeleteScheduleUseCase {

    /**
     * 일정을 삭제합니다.
     * <p>
     * 일정에 연결된 ScheduleParticipant 중 attendance.status가 하나라도 존재한다면 삭제가 불가능하며, 예외가 발생합니다.
     * <p>
     * 일정에 연결된 모든 ScheduleParticipant도 함께 삭제됩니다.
     */
    void delete(Long scheduleId, Long actorMemberId);

    /**
     * 일정을 강제로 삭제합니다.
     * <p>
     * 출석 기록 존재 여부와 관계 없이 삭제 가능하며, 일정에 연결된 모든 ScheduleParticipant도 함께 삭제됩니다.
     */
    void forceDelete(Long scheduleId, Long actorMemberId);
}
