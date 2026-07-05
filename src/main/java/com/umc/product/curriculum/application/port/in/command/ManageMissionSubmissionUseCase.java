package com.umc.product.curriculum.application.port.in.command;

import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.WithdrawMissionSubmissionCommand;

/**
 * 미션 제출물 관리 UseCase (챌린저 전용)
 */
public interface ManageMissionSubmissionUseCase {

    /**
     * 워크북 내 미션 제출
     * <p>
     * 스터디 일정 등록 여부에 따라 지각 여부가 결정됩니다.
     * 주차 종료 일자 이후에는 제출이 불가능합니다.
     *
     * @param command 제출 커맨드 (챌린저 워크북 ID, 미션 ID, 멤버 ID, 제출 내용)
     * @return 생성된 미션 제출물 ID
     */
    Long create(CreateMissionSubmissionCommand command);

    /**
     * 제출한 미션 수정
     * <p>
     * 스터디 일정이 등록된 경우 일정 당일 00:00 이후에는 수정이 불가능합니다.
     *
     * @param command 수정 커맨드 (미션 제출물 ID, 멤버 ID, 변경할 내용)
     */
    void edit(EditMissionSubmissionCommand command);

    /**
     * 제출한 미션 철회
     * <p>
     * 기간과 관계없이 철회 가능하나, 재제출이 불가능해 LATE 처리될 수 있습니다.
     *
     * @param command 철회 커맨드 (미션 제출물 ID, 요청자 멤버 ID)
     */
    void withdraw(WithdrawMissionSubmissionCommand command);
}
