package com.umc.product.form.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.port.out.SaveFormResponsePort;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.enums.FormResponseStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FormResponsePersistenceAdapter implements LoadFormResponsePort, SaveFormResponsePort {

    private final FormResponseJpaRepository formResponseJpaRepository;
    private final FormResponseQueryRepository formResponseQueryRepository;

    @Override
    public Optional<FormResponse> findById(Long formResponseId) {
        return formResponseJpaRepository.findById(formResponseId);
    }

    @Override
    public List<FormResponse> listByIdsWithForm(Set<Long> formResponseIds) {
        return formResponseQueryRepository.findAllByIdInWithForm(formResponseIds);
    }

    @Override
    public List<FormResponse> listByFormId(Long formId) {
        return formResponseQueryRepository.findAllByFormId(formId);
    }

    @Override
    public List<FormResponse> listSubmittedByFormId(Long formId) {
        return formResponseQueryRepository.findAllSubmittedByFormId(formId);
    }

    @Override
    public Optional<FormResponse> findDraftByFormIdAndRespondentMemberId(Long formId, Long respondentMemberId) {
        return formResponseJpaRepository.findFirstByForm_IdAndRespondentMemberIdAndStatusOrderByIdDesc(
            formId, respondentMemberId, FormResponseStatus.DRAFT
        );
    }

    @Override
    public FormResponse save(FormResponse formResponse) {
        return formResponseJpaRepository.save(formResponse);
    }

    @Override
    public void deleteById(Long formResponseId) {
        formResponseJpaRepository.deleteById(formResponseId);
    }

    @Override
    public List<FormResponse> findAllDraftByRespondentMemberId(Long respondentMemberId) {
        return formResponseJpaRepository.findByRespondentMemberIdAndStatus(
            respondentMemberId, FormResponseStatus.DRAFT
        );
    }

    @Override
    public List<Long> findDraftIdsByFormId(Long formId) {
        return formResponseJpaRepository.findIdsByFormIdAndStatus(formId, FormResponseStatus.DRAFT);
    }

    @Override
    public void deleteAllByIds(List<Long> ids) {
        formResponseJpaRepository.deleteAllByIdInBatch(ids);
    }

    @Override
    public boolean existsByFormIdAndMemberId(Long formId, Long memberId) {
        return formResponseJpaRepository.existsByForm_IdAndRespondentMemberId(formId, memberId);
    }

    @Override
    @Transactional
    public int deleteByFormIdAndStatus(Long formId, FormResponseStatus status) {
        return formResponseJpaRepository.deleteByFormIdAndStatus(formId, status);
    }

    @Override
    public List<Long> findIdsByFormIdAndStatus(Long formId, FormResponseStatus status) {
        return formResponseJpaRepository.findIdsByFormIdAndStatus(formId, status);
    }

    @Override
    public long countSubmittedByFormId(Long formId) {
        return formResponseJpaRepository.countByFormIdAndStatus(formId, FormResponseStatus.SUBMITTED);
    }

    @Override
    public Optional<FormResponse> findSubmittedByFormIdAndRespondentMemberId(Long formId, Long respondentMemberId) {
        return formResponseJpaRepository.findFirstByForm_IdAndRespondentMemberIdAndStatusOrderByIdDesc(
            formId, respondentMemberId, FormResponseStatus.SUBMITTED
        );
    }

    @Override
    @Transactional
    public void deleteByFormId(Long formId) {
        formResponseJpaRepository.deleteByFormId(formId);
    }

    @Override
    public Optional<FormResponse> findDraftByAccessKeyHash(String accessKeyHash) {
        return formResponseJpaRepository.findByResponseAccessKeyHashAndStatus(
            accessKeyHash, FormResponseStatus.DRAFT
        );
    }

    @Override
    public Optional<FormResponse> findSubmittedByAccessKeyHash(String accessKeyHash) {
        return formResponseJpaRepository.findByResponseAccessKeyHashAndStatus(
            accessKeyHash, FormResponseStatus.SUBMITTED
        );
    }
}
