package com.umc.product.form.application.port.out;

import java.util.List;
import java.util.Set;

import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;

public interface SaveAnswerPort {

    Answer save(Answer answer);

    List<Answer> saveAll(List<Answer> answers);

    List<AnswerChoice> saveAllChoices(List<AnswerChoice> choices);

    /**
     * 특정 FormResponse에 속한 모든 Answer 와 그에 딸린 AnswerChoice 를 삭제.
     * updateResponse 시 기존 답변을 지우고 새로 쓰기 위해 사용.
     * AnswerChoice -> Answer 순서로 삭제하여 FK 제약을 만족시킨다.
     */
    void deleteAllByFormResponseId(Long formResponseId);

    /**
     * 특정 FormResponse 에 속한 답변 중 questionId 가 주어진 집합에 포함되는 Answer 와 그에 딸린 AnswerChoice 를 삭제.
     * <p>
     * orphan answer 정리(제출 시 방문하지 않은 섹션의 답변 정리) 용.
     * AnswerChoice -> Answer 순서로 삭제하여 FK 제약을 만족시킨다.
     * 반환값은 삭제된 Answer 행 수.
     */
    int deleteByFormResponseIdAndQuestionIdIn(Long formResponseId, Set<Long> questionIds);

    /**
     * 특정 폼에 속한 모든 Answer 와 그에 딸린 AnswerChoice 를 삭제 (deleteForm cascade 용).
     * AnswerChoice -> Answer 순서로 삭제하여 FK 제약을 만족시킨다.
     */
    void deleteByFormId(Long formId);

    /**
     * 특정 질문에 속한 모든 Answer 와 그에 딸린 AnswerChoice 를 삭제 (deleteQuestion cascade 용).
     * AnswerChoice -> Answer 순서로 삭제하여 FK 제약을 만족시킨다.
     */
    void deleteByQuestionId(Long questionId);

    /**
     * 단일 Answer 와 그에 딸린 AnswerChoice를 삭제 (deleteAnswer 용).
     * AnswerChoice -> Answer 순서로 삭제하여 FK 제약을 만족시킨다.
     */
    void deleteByAnswerId(Long answerId);

    /**
     * 단일 Answer의 AnswerChoice만 삭제 (updateAnswer의 객관식 갈아끼움 용).
     * Answer 자체는 보존.
     */
    void deleteChoicesByAnswerId(Long answerId);
}
