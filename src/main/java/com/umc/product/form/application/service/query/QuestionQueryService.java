package com.umc.product.form.application.service.query;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetQuestionUseCase;
import com.umc.product.form.application.port.in.query.dto.QuestionInfo;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionQueryService implements GetQuestionUseCase {

    private final LoadQuestionPort loadQuestionPort;

    @Override
    public Optional<QuestionInfo> findById(Long questionId) {
        return loadQuestionPort.findById(questionId)
            .map(QuestionInfo::from);
    }

    @Override
    public QuestionInfo getById(Long questionId) {
        return loadQuestionPort.findById(questionId)
            .map(QuestionInfo::from)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));
    }

    @Override
    public List<QuestionInfo> listBySectionId(Long sectionId) {
        return loadQuestionPort.listBySectionId(sectionId).stream()
            .map(QuestionInfo::from)
            .toList();
    }
}
