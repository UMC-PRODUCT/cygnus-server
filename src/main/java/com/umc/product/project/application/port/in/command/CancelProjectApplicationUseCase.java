package com.umc.product.project.application.port.in.command;

import com.umc.product.project.application.port.in.command.dto.CancelProjectApplicationCommand;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationInfo;

/**
 * 챌린저 지원서 철회 UseCase (APPLY-005).
 * <p>
 * 지원서를 {@code CANCELLED}로 soft delete 합니다.
 * <p>
 * 정책:
 * <ul>
 *   <li>가능 상태: {@code DRAFT}, {@code SUBMITTED}</li>
 *   <li>불가 상태: {@code APPROVED} / {@code REJECTED} (이미 종결), {@code CANCELLED} (이중 취소)</li>
 *   <li>시간 제약: 지원한 매칭 차수가 OPEN 인 동안만 (startsAt ≤ now ≤ endsAt). 선발 시작 후 철회 불가.</li>
 * </ul>
 * <p>
 * 책임 분리:
 * <ul>
 *   <li>차수 OPEN 검증: {@code ProjectApplicationCommandService}</li>
 *   <li>상태 머신 전이 (DRAFT/SUBMITTED -> CANCELLED): {@code ProjectApplication.cancel}</li>
 * </ul>
 * <p>
 * 권한 검증 (행위자, 상태별 접근 등)은 호출하는 endpoint의 권한 모델에서 결정합니다.
 * <p>
 * CANCELLED 후 동일 매칭 차수 재지원이 가능합니다 (DB partial unique index가 활성 지원서 1 개로 제한).
 * Form {@code FormResponse} 본문은 보존.
 */
public interface CancelProjectApplicationUseCase {
    ProjectApplicationInfo cancel(CancelProjectApplicationCommand command);
}
