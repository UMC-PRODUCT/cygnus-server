package com.umc.product.project.application.port.in.command;

import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;

/**
 * 프로젝트 지원 폼 저장 UseCase (PROJECT-106).
 * <p>
 * PUT 시멘틱: 폼이 없으면 생성하고, 있으면 본문 구조와 일치하도록 섹션/질문/옵션을 동기화한다.
 * Form 도메인의 {@link com.umc.product.form.application.port.in.command.ManageFormUseCase} 등 5종 UseCase를
 * orchestration 으로 호출하며, 정책({@link com.umc.product.project.domain.ProjectApplicationFormPolicy}) 도 함께 갱신한다.
 */
public interface UpsertProjectApplicationFormUseCase {

    /**
     * 본문 구조와 일치하도록 폼을 생성/갱신한다.
     *
     * @return 갱신 후 폼의 전체 구조
     */
    ApplicationFormInfo upsert(UpsertApplicationFormCommand command);
}
