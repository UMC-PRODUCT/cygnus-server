package com.umc.product.form.application.port.in.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.form.domain.exception.FormErrorCode;

/**
 * FormResponse 조회 UseCase.
 * <p>
 * 폼 응답을 다양한 기준으로 조회한다. DRAFT / SUBMITTED 상태 구분해 조회할 수 있도록 상태별 조회 메서드를 제공.
 */
public interface GetFormResponseUseCase {

    /**
     * 응답 ID 로 단건 조회. 없으면 Optional.empty.
     */
    Optional<FormResponseInfo> findById(Long formResponseId);

    /**
     * 응답 ID 로 단건 조회. 없으면 FORM_RESPONSE_NOT_FOUND 예외.
     */
    FormResponseInfo getById(Long formResponseId);

    /**
     * 특정 폼의 모든 응답 (DRAFT + SUBMITTED) 을 id 내림차순으로 반환.
     */
    List<FormResponseInfo> listByFormId(Long formId);

    /**
     * 특정 폼의 SUBMITTED 응답 목록을 id 내림차순으로 반환. 폼 생성자(소유자)의 응답 관리 / 통계 화면 용도.
     */
    List<FormResponseInfo> listSubmittedByFormId(Long formId);

    /**
     * (기명 전용) 특정 사용자의 모든 draft 응답을 반환한다. "내가 작성 중인 응답 목록" 용도.
     * <p>
     * {@code respondentMemberId} 가 필수이며 null 을 넘기면
     * {@link FormErrorCode#RESPONDENT_MEMBER_ID_REQUIRED} 예외.
     */
    List<FormResponseInfo> listDraftByRespondentMemberId(Long respondentMemberId);

    /**
     * (기명 전용) 특정 폼에 대한 특정 사용자의 draft 응답을 조회. 없으면 Optional.empty. "작성 중 응답 이어서 보기" 용도.
     * <p>
     * 중복 응답을 허용하지 않는 폼 전용 단건 조회다. 중복 허용 폼은 {@code formResponseId} 기준으로 조회해야 한다.
     * <p>
     * {@code respondentMemberId} 가 필수이며 null 을 넘기면
     * {@link FormErrorCode#RESPONDENT_MEMBER_ID_REQUIRED} 예외.
     */
    Optional<FormResponseInfo> findDraftByFormIdAndRespondentMemberId(Long formId, Long respondentMemberId);

    /**
     * (기명 전용) 특정 폼에 대한 특정 사용자의 SUBMITTED 응답을 조회. 없으면 Optional.empty.
     * <p>
     * 중복 응답을 허용하지 않는 폼 전용 단건 조회다. 중복 허용 폼은 {@code formResponseId} 기준으로 조회해야 한다.
     * <p>
     * {@code respondentMemberId} 가 필수이며 null 을 넘기면
     * {@link FormErrorCode#RESPONDENT_MEMBER_ID_REQUIRED} 예외.
     */
    Optional<FormResponseInfo> findSubmittedByFormIdAndRespondentMemberId(Long formId, Long respondentMemberId);

    /**
     * 특정 응답의 메타 + 모든 답변을 한 번에 조회 (facade). 응답 상세 화면 (응답자 본인 / 폼 작성자) 용도. 없으면 FORM_RESPONSE_NOT_FOUND 예외.
     */
    FormResponseWithAnswersInfo getResponseWithAnswers(Long formResponseId);

    /**
     * {@link #getResponseWithAnswers} 의 graceful 버전. 미존재 시 Optional.empty() 를 반환하므로 호출 도메인의 invariant(예: dangling
     * formResponseId)를 자체 에러 코드로 통일하고 싶은 경우 사용한다.
     */
    Optional<FormResponseWithAnswersInfo> findResponseWithAnswers(Long formResponseId);

    /**
     * 여러 응답의 메타 + 답변을 한 번에 조회한다.
     *
     * @return formResponseId -> 응답 상세. 미존재 ID는 결과에 포함하지 않는다.
     */
    Map<Long, FormResponseWithAnswersInfo> findResponsesWithAnswers(Set<Long> formResponseIds);
}
