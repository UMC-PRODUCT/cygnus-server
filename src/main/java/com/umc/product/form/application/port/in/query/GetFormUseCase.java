package com.umc.product.form.application.port.in.query;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.dto.FormInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.FormOwnerReference;

/**
 * Form 조회 UseCase.
 * <p>
 * {@link #findById}는 없을 수 있는 케이스(탈퇴 사용자의 폼 등)에 사용,
 * {@link #getById}는 반드시 존재해야 하는 케이스에 사용한다.
 * {@link #getFormWithStructure}는 편집기 초기 로딩/응답자 화면 등에서 폼 전체 구조(섹션/질문/옵션)를 한 번에 조회할 때 사용.
 */
public interface GetFormUseCase {

    /**
     * Form ID로 단건 조회. 없으면 Optional.empty.
     */
    Optional<FormInfo> findById(FormOwnerReference expectedOwner, FormActorContext actorContext);

    /**
     * Form ID로 단건 조회. 없으면 FORM_NOT_FOUND 예외.
     */
    FormInfo getById(FormOwnerReference expectedOwner, FormActorContext actorContext);

    /**
     * 폼 전체 구조(섹션 → 질문 → 옵션 중첩)를 한 번에 조회.
     * 편집기 초기 로딩이나 응답자 UI 렌더링 시 N+1 왕복을 피하기 위한 facade.
     */
    FormWithStructureInfo getFormWithStructure(FormOwnerReference expectedOwner, FormActorContext actorContext);

    /**
     * 여러 폼의 전체 구조를 한 번에 조회한다.
     * <p>
     * 입력된 모든 formId 는 존재해야 한다. 누락 시 FORM_NOT_FOUND 예외.
     * null/empty scope는 빈 map을 반환한다. null owner나 같은 form ID의 중복 owner는 거부하며,
     * adapter가 scope 밖 Form root 또는 조회한 parent 집합 밖 child를 반환해도 fail closed한다.
     *
     * @return formId -> 폼 구조
     */
    Map<Long, FormWithStructureInfo> batchGetFormsWithStructure(
        Collection<FormOwnerReference> expectedOwners,
        FormActorContext actorContext
    );

    /**
     * 제출된 응답의 questionId 집합을 기준으로 폼 구조를 조립한다 (isActive 무관).
     * <p>
     * 질문 fork 이후에도 답변 당시의 질문이 표시되도록 Answer.questionId 역추적 방식으로 구조를 구성한다.
     * 편집기, 모집문항 보기, 신규 지원 경로에서는 {@link #getFormWithStructure}를 사용할 것.
     * adapter가 요청하지 않은 question 또는 expected owner 밖 Form root를 반환하면
     * 전체 호출을 거부한다.
     */
    FormWithStructureInfo getFormWithStructureByQuestionIds(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Set<Long> questionIds
    );
}
