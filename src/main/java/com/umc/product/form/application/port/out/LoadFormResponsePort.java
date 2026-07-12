package com.umc.product.form.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.enums.FormResponseStatus;

public interface LoadFormResponsePort {

    Optional<FormResponse> findById(Long formResponseId);

    List<FormResponse> listByIdsWithForm(Set<Long> formResponseIds);

    /**
     * 특정 폼의 모든 응답 (DRAFT + SUBMITTED) 을 id 내림차순으로 반환.
     */
    List<FormResponse> listByFormId(Long formId);

    /**
     * 특정 폼의 SUBMITTED 응답 목록을 id 내림차순으로 반환.
     */
    List<FormResponse> listSubmittedByFormId(Long formId);

    Optional<FormResponse> findDraftByFormIdAndRespondentMemberId(Long formId, Long respondentMemberId);

    List<FormResponse> findAllDraftByRespondentMemberId(Long respondentMemberId);

    List<Long> findDraftIdsByFormId(Long formId);

    boolean existsByFormIdAndMemberId(Long formId, Long memberId);

    List<Long> findIdsByFormIdAndStatus(Long formId, FormResponseStatus status);

    long countSubmittedByFormId(Long formId);

    Optional<FormResponse> findSubmittedByFormIdAndRespondentMemberId(Long formId, Long respondentMemberId);

    /**
     * (익명 전용) sha256 해시 매칭으로 DRAFT 응답 조회.
     * 매칭 없거나 SUBMITTED 인 경우 Optional.empty.
     */
    Optional<FormResponse> findDraftByAccessKeyHash(String accessKeyHash);

    /**
     * (익명 전용) sha256 해시 매칭으로 SUBMITTED 응답 조회.
     * 매칭 없거나 DRAFT 인 경우 Optional.empty.
     */
    Optional<FormResponse> findSubmittedByAccessKeyHash(String accessKeyHash);
}
