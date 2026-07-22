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

    boolean existsByFormId(Long formId);

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
     * (기명 전용) 특정 응답의 메타 + 모든 답변을 한 번에 조회 (facade). 응답 상세 화면 (응답자 본인 / 폼 작성자) 용도.
     * 없으면 FORM_RESPONSE_NOT_FOUND 예외.
     * <p>
     * 익명 응답 (respondentMemberId=null) 은 조회되지 않고 FORM_RESPONSE_NOT_FOUND 로 처리된다.
     * 익명 응답 상세 조회는 {@link #getResponseWithAnswersByAccessKey} 를 사용해야 한다.
     */
    FormResponseWithAnswersInfo getResponseWithAnswers(Long formResponseId);

    /**
     * (기명 전용) {@link #getResponseWithAnswers} 의 graceful 버전. 미존재 시 Optional.empty() 를 반환하므로
     * 호출 도메인의 invariant(예: dangling formResponseId)를 자체 에러 코드로 통일하고 싶은 경우 사용한다.
     * <p>
     * 익명 응답 (respondentMemberId=null) 은 조회되지 않고 Optional.empty() 로 처리된다.
     * 익명 응답 조회는 {@link #findByAccessKey} 를 사용해야 한다.
     */
    Optional<FormResponseWithAnswersInfo> findResponseWithAnswers(Long formResponseId);

    /**
     * (기명 전용) 여러 응답의 메타 + 답변을 한 번에 조회한다.
     * <p>
     * 익명 응답 (respondentMemberId=null) 은 결과 map 에서 제외된다. 미존재 ID 도 결과에 포함하지 않는다.
     *
     * @return formResponseId -> 응답 상세
     */
    Map<Long, FormResponseWithAnswersInfo> findResponsesWithAnswers(Set<Long> formResponseIds);

    /**
     * (익명 전용) 응답 ID와 raw access key를 함께 검증해 여러 응답의 메타와 답변을 조회한다.
     * <p>
     * access key가 일치하지 않거나 기명 응답인 경우 결과 map에서 제외한다. 호출 도메인이 자체 접근 권한을 먼저
     * 검증한 뒤 익명 응답을 일괄 조회할 때 사용한다.
     */
    Map<Long, FormResponseWithAnswersInfo> findAnonymousResponsesWithAnswers(
        Map<Long, String> accessKeysByFormResponseId
    );

    /**
     * (익명 전용) {@code responseAccessKey}(raw) 의 sha256 매칭으로 응답 단건 조회.
     * <p>
     * 매칭 없으면 Optional.empty. 기명 응답이 매칭되면 방어 목적으로 Optional.empty.
     * {@code rawKey} 가 null 이면 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     * <p>
     * 소비 도메인(리크루팅 등) 이 응답 존재 확인 용도로 사용.
     */
    Optional<FormResponseInfo> findByAccessKey(String rawKey);

    /**
     * (익명 전용) {@code responseAccessKey}(raw) 의 sha256 매칭으로 응답 + 답변 상세 조회.
     * <p>
     * 매칭 없거나 기명 응답이 매칭되면 {@link FormErrorCode#FORM_RESPONSE_NOT_FOUND}.
     * {@code rawKey} 가 null 이면 {@link FormErrorCode#RESPONSE_ACCESS_KEY_REQUIRED}.
     * <p>
     * 응답자 본인이 자기 응답 상세를 확인하는 용도.
     */
    FormResponseWithAnswersInfo getResponseWithAnswersByAccessKey(String rawKey);
}
