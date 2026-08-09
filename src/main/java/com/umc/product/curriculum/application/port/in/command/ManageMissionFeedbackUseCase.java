package com.umc.product.curriculum.application.port.in.command;

import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.DeleteMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionFeedbackCommand;

/**
 * 미션 피드백 관리 UseCase (운영진 전용)
 */
public interface ManageMissionFeedbackUseCase {

    /**
     * 제출된 미션에 대한 피드백 작성
     * <p>
     * 담당 그룹 mentor, 같은 학교·기수 회장단 또는 SUPER_ADMIN이 작성할 수 있습니다.
     * 피드백 벌점 자동화는 현재 제공하지 않습니다.
     *
     * @param command 작성 커맨드 (미션 제출물 ID, 작성자 멤버 ID, 내용, 평가 결과)
     * @return 생성된 피드백 ID
     */
    Long create(CreateMissionFeedbackCommand command);

    /**
     * 제출된 미션 피드백 수정
     * <p>
     * - 작성일 기준 2주가 경과되기 전까지만 수정 가능합니다.
     * - PASS → FAIL 변경은 불가능합니다.
     *
     * @param command 수정 커맨드 (피드백 ID, 요청자 멤버 ID, 변경할 내용)
     */
    void edit(EditMissionFeedbackCommand command);

    /**
     * 제출된 미션 피드백 삭제
     * <p>
     * 해당 기수 종료 이후에는 삭제 불가능합니다.
     *
     * @param command 삭제 커맨드 (피드백 ID, 요청자 멤버 ID)
     */
    void delete(DeleteMissionFeedbackCommand command);
}
