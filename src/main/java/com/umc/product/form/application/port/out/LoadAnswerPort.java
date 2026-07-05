package com.umc.product.form.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;

public interface LoadAnswerPort {

    Optional<Answer> findById(Long answerId);

    /**
     * 특정 FormResponse + 특정 Question 의 답변 존재 여부 (createAnswer의 중복 검증 용).
     */
    boolean existsByFormResponseIdAndQuestionId(Long formResponseId, Long questionId);

    /**
     * 특정 FormResponse 에 속한 모든 답변을 질문 orderNo 오름차순으로 반환.
     */
    List<Answer> listByFormResponseId(Long formResponseId);

    /**
     * 여러 FormResponse 에 속한 모든 답변을 한 번에 조회한다.
     */
    List<Answer> listByFormResponseIds(Set<Long> formResponseIds);

    /**
     * 여러 답변의 AnswerChoice 를 한 번에 조회 (벌크 N+1 회피).
     * questionOption.orderNo 오름차순으로 정렬.
     */
    List<AnswerChoice> listChoicesByAnswerIdIn(Set<Long> answerIds);

    /**
     * 특정 폼(투표)의 총 참여자 수를 반환합니다.
     */
    long countTotalParticipants(Long formId);

    /**
     * 특정 폼(투표)의 옵션별 득표수를 집계하여 반환합니다.
     * key: option ID, value: 투표 수
     */
    Map<Long, Long> countVotesByOptionId(Long formId);

    /**
     * 특정 사용자가 해당 폼(투표)에서 선택한 옵션 ID 목록을 반환합니다.
     */
    List<Long> findSelectedOptionIdsByMember(Long formId, Long memberId);

    /**
     * 특정 폼(투표)의 옵션별 투표자 ID 목록을 조회합니다.
     * key: option ID, value: 투표자 member ID 목록
     */
    Map<Long, List<Long>> findSelectedMemberIdsByOptionId(Long formId);
}
