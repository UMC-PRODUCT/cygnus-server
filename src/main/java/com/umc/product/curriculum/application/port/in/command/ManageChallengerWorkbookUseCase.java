package com.umc.product.curriculum.application.port.in.command;

import java.util.List;

import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeleteChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeployChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.ExcuseChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;

/**
 * 챌린저 워크북 관리 UseCase
 * <p>
 * 챌린저의 워크북 배포 요청, 수정, 인정 처리 및 운영진의 강제 삭제/검토 기능을 제공합니다.
 */
public interface ManageChallengerWorkbookUseCase {

    // <------------------ 챌린저 전용 -------------------------->

    /**
     * 챌린저 워크북 배포 요청 (챌린저 전용)
     * <p>
     * 여러 개의 원본 워크북을 한 번에 배포받을 수 있습니다.
     * 하나라도 배포 불가능한 상태이거나 권한이 없으면 전체 요청이 실패합니다.
     *
     * @param command 배포 커맨드 (원본 워크북 ID 목록, 요청 멤버 ID)
     * @return 생성된 챌린저 워크북 ID 목록
     */
    List<ChallengerWorkbookInfo> batchDeploy(DeployChallengerWorkbookCommand command);

    /**
     * 챌린저 워크북 수정 (챌린저 전용)
     *
     * @param command 수정 커맨드 (챌린저 워크북 ID, 요청 멤버 ID, 변경할 내용)
     */
    void edit(EditChallengerWorkbookCommand command);

    // <---------------------- 운영진 전용 -------------------------->

    /**
     * 챌린저 워크북 강제 삭제 (운영진 전용)
     * <p>
     * 부정한 방법으로 배포된 워크북을 강제 삭제합니다.
     * 연관된 미션 제출 또는 피드백이 있으면 삭제를 거부합니다.
     *
     * @param command 삭제 커맨드 (챌린저 워크북 ID, 요청 멤버 ID, 삭제 사유)
     */
    void delete(DeleteChallengerWorkbookCommand command);

    /**
     * 챌린저 워크북 인정 처리 (운영진 전용)
     * <p>
     * 인정 처리된 워크북은 미션을 제출하지 않아도 벌점이 부과되지 않습니다.
     * 인정 처리에 대한 철회는 제공하지 않습니다.
     *
     * @param command 인정 처리 커맨드 (챌린저 워크북 ID, 요청 멤버 ID, 사유)
     */
    void excuse(ExcuseChallengerWorkbookCommand command);

}
