package com.umc.product.form.adapter.out.persistence;

import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.domain.QuestionOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class QuestionOptionPersistenceAdapter implements SaveQuestionOptionPort, LoadQuestionOptionPort {

    private final QuestionOptionJpaRepository questionOptionJpaRepository;
    private final QuestionOptionQueryRepository questionOptionQueryRepository;

    @Override
    public QuestionOption save(QuestionOption option) {
        return questionOptionJpaRepository.save(option);
    }

    @Override
    public List<QuestionOption> saveAll(List<QuestionOption> questionOptions) {
        return questionOptionJpaRepository.saveAll(questionOptions);
    }

    @Override
    public void deleteById(Long optionId) {
        questionOptionJpaRepository.deleteById(optionId);
    }

    @Override
    public void deleteAllByQuestionId(Long questionId) {
        questionOptionJpaRepository.deleteAllByQuestionId(questionId);
    }

    @Override
    public void deleteByFormId(Long formId) {
        questionOptionJpaRepository.deleteByFormId(formId);
    }

    @Override
    public void deleteBySectionId(Long sectionId) {
        questionOptionJpaRepository.deleteBySectionId(sectionId);
    }

    @Override
    public Optional<QuestionOption> findById(Long optionId) {
        return questionOptionJpaRepository.findById(optionId);
    }

    @Override
    public boolean existsByIdAndQuestionId(Long optionId, Long questionId) {
        if (optionId == null || questionId == null) {
            return false;
        }
        return questionOptionJpaRepository.existsByIdAndQuestion_Id(optionId, questionId);
    }

    @Override
    public List<QuestionOption> listByQuestionId(Long questionId) {
        return questionOptionQueryRepository.findAllByQuestionId(questionId);
    }

    @Override
    public List<QuestionOption> listByQuestionIdIn(Set<Long> questionIds) {
        return questionOptionQueryRepository.findAllByQuestionIdIn(questionIds);
    }
}
