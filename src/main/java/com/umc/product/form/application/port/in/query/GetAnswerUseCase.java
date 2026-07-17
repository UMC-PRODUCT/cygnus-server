package com.umc.product.form.application.port.in.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.domain.FormOwnerReference;

/**
 * Answer 조회 UseCase.
 * <p>
 * 개별 답변 조회와 FormResponse 단위 전체 답변 조회를 모두 지원한다.
 * <p>
 * <b>기명/익명 분리</b> (Command 쪽 {@code ManageAnswerUseCase} 와 동일 패턴):
 * <ul>
 *   <li>기명: {@link #findById} / {@link #getById} / {@link #listByFormResponseId} / {@link #listByFormResponseIds}
 *     — 익명 응답 (respondentMemberId=null) 은 반환 안 함.</li>
 *   <li>익명: {@link #listByFormResponseIdAsAnonymous} — {@code responseAccessKey} sha256 매칭으로 응답 인증 후 반환.</li>
 * </ul>
 */
public interface GetAnswerUseCase {

    /**
     * (기명 전용) 답변 ID 로 단건 조회. 없거나 익명 응답의 답변이면 Optional.empty.
     * expected owner binding과 READ policy는 답변 존재 여부와 무관하게 먼저 검증한다.
     */
    Optional<AnswerInfo> findById(FormOwnerReference expectedOwner, FormActorContext actorContext, Long answerId);

    /**
     * (기명 전용) 답변 ID 로 단건 조회. 없거나 익명 응답의 답변이면 ANSWER_NOT_FOUND.
     */
    AnswerInfo getById(FormOwnerReference expectedOwner, FormActorContext actorContext, Long answerId);

    /**
     * (기명 전용) 특정 FormResponse 에 속한 모든 답변을 반환한다. (질문 orderNo 순)
     * 익명 응답이면 빈 리스트.
     */
    List<AnswerInfo> listByFormResponseId(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long formResponseId
    );

    /**
     * (기명 전용) 여러 FormResponse 에 속한 답변을 한 번에 조회한다. 익명 응답의 답변은 결과에서 제외.
     * 비어 있지 않은 ID 집합에는 null/empty/중복 form ID 없는 expected owner scope가 필수다.
     * 조회된 기명 response/answer가 요청하지 않은 response ID 또는 scope 밖 root를 가리키면
     * 전체 호출을 fail closed한다.
     *
     * @return formResponseId -> 답변 목록
     */
    Map<Long, List<AnswerInfo>> listByFormResponseIds(
        Collection<FormOwnerReference> expectedOwners,
        FormActorContext actorContext,
        Set<Long> formResponseIds
    );

    /**
     * (익명 전용) 답변 ID 로 단건 조회. access-key 인증 후 반환.
     * <p>
     * 인증 실패 케이스는 모두 Optional.empty (정보 유출 방지). rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED.
     * expected owner binding과 READ policy는 답변 존재 여부와 무관하게 먼저 검증한다.
     */
    Optional<AnswerInfo> findByIdAsAnonymous(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long answerId
    );

    /**
     * (익명 전용) 답변 ID 로 단건 조회. access-key 인증 후 반환.
     * <p>
     * 익명 경계 유출 방지를 위해 rawKey null 을 제외한 모든 실패 케이스 (답변 없음, 기명 응답, hash 불일치)
     * 는 {@code FORM_RESPONSE_FORBIDDEN} 로 통일. rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED.
     */
    AnswerInfo getByIdAsAnonymous(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long answerId
    );

    /**
     * (익명 전용) 익명 FormResponse 의 답변을 access-key 인증 후 반환한다. (질문 orderNo 순)
     * <p>
     * {@code responseAccessKey} 는 발급 시 서버가 반환한 raw 값이며, 서버가 저장된 hash 와 sha256 매칭.
     * 익명 경계 유출 방지를 위해 rawKey null 을 제외한 모든 인증 실패 케이스
     * (기명 응답, hash 불일치) 는 {@code FORM_RESPONSE_FORBIDDEN}. rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED.
     * <p>
     * 응답에 답변이 하나도 없으면 빈 리스트 (정보 유출 방지 위해 응답 존재 여부는 검사하지 않음).
     */
    List<AnswerInfo> listByFormResponseIdAsAnonymous(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long formResponseId
    );
}
