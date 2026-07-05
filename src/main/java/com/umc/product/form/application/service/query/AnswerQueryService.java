package com.umc.product.form.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnswerQueryService implements GetAnswerUseCase {

    private final LoadAnswerPort loadAnswerPort;

    @Override
    public Optional<AnswerInfo> findById(Long answerId) {
        return loadAnswerPort.findById(answerId)
            .map(this::toAnswerInfo);
    }

    @Override
    public AnswerInfo getById(Long answerId) {
        Answer answer = loadAnswerPort.findById(answerId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.ANSWER_NOT_FOUND));
        return toAnswerInfo(answer);
    }

    @Override
    public List<AnswerInfo> listByFormResponseId(Long formResponseId) {
        // 1. 답변 로드 (섹션/질문 orderNo 정렬)
        List<Answer> answers = loadAnswerPort.listByFormResponseId(formResponseId);
        if (answers.isEmpty()) {
            return List.of();
        }

        // 2. 답변 ID 셋으로 AnswerChoice 벌크 로드 (N+1 회피)
        Set<Long> answerIds = answers.stream()
            .map(Answer::getId)
            .collect(Collectors.toSet());
        List<AnswerChoice> allChoices = loadAnswerPort.listChoicesByAnswerIdIn(answerIds);

        // 3. answerId -> choices 그룹핑
        Map<Long, List<AnswerChoice>> choicesByAnswer = allChoices.stream()
            .collect(Collectors.groupingBy(c -> c.getAnswer().getId()));

        // 4. DTO 조립 (answers 의 정렬 순서 보존)
        return answers.stream()
            .map(answer -> AnswerInfo.from(
                answer,
                choicesByAnswer.getOrDefault(answer.getId(), List.of())
            ))
            .toList();
    }

    @Override
    public Map<Long, List<AnswerInfo>> listByFormResponseIds(Set<Long> formResponseIds) {
        if (formResponseIds == null || formResponseIds.isEmpty()) {
            return Map.of();
        }

        List<Answer> answers = loadAnswerPort.listByFormResponseIds(formResponseIds);
        if (answers.isEmpty()) {
            return Map.of();
        }

        Set<Long> answerIds = answers.stream()
            .map(Answer::getId)
            .collect(Collectors.toSet());
        List<AnswerChoice> allChoices = loadAnswerPort.listChoicesByAnswerIdIn(answerIds);
        Map<Long, List<AnswerChoice>> choicesByAnswer = allChoices.stream()
            .collect(Collectors.groupingBy(c -> c.getAnswer().getId()));

        return answers.stream()
            .collect(Collectors.groupingBy(
                answer -> answer.getFormResponse().getId(),
                Collectors.mapping(
                    answer -> AnswerInfo.from(answer, choicesByAnswer.getOrDefault(answer.getId(), List.of())),
                    Collectors.toList()
                )
            ));
    }

    /**
     * 단건 Answer -> AnswerInfo. 해당 답변의 AnswerChoice 만 조회 후 조립.
     */
    private AnswerInfo toAnswerInfo(Answer answer) {
        List<AnswerChoice> choices = loadAnswerPort.listChoicesByAnswerIdIn(Set.of(answer.getId()));
        return AnswerInfo.from(answer, choices);
    }
}
