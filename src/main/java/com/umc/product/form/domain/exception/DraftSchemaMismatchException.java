package com.umc.product.form.domain.exception;

import java.util.List;

import lombok.Getter;

/**
 * draft 제출 시 저장된 답변이 현재 폼 스키마와 어긋난 경우 발생. 응답 body 에 어긋난 질문 ID 목록을 노출해 클라이언트가 사용자에게 해당 질문의 재확인을 유도할 수 있도록 한다.
 */
@Getter
public class DraftSchemaMismatchException extends FormDomainException {

    private final List<Long> staleQuestionIds;

    public DraftSchemaMismatchException(List<Long> staleQuestionIds) {
        super(FormErrorCode.DRAFT_SCHEMA_MISMATCH);
        this.staleQuestionIds = List.copyOf(staleQuestionIds);
    }
}
