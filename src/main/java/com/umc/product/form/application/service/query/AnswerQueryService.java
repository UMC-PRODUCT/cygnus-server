package com.umc.product.form.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnswerQueryService implements GetAnswerUseCase {

    private final LoadAnswerPort loadAnswerPort;
    // 익명 조회의 access-key 매칭용 (AnswerCommandService 와 동일 패턴)
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    public Optional<AnswerInfo> findById(Long answerId) {
        return loadAnswerPort.findById(answerId)
            .filter(a -> a.getFormResponse().getRespondentMemberId() != null)
            .map(this::toAnswerInfo);
    }

    @Override
    public AnswerInfo getById(Long answerId) {
        Answer answer = loadAnswerPort.findById(answerId)
            .filter(a -> a.getFormResponse().getRespondentMemberId() != null)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.ANSWER_NOT_FOUND));
        return toAnswerInfo(answer);
    }

    @Override
    public List<AnswerInfo> listByFormResponseId(Long formResponseId) {
        List<Answer> answers = loadAnswerPort.listByFormResponseId(formResponseId);
        if (answers.isEmpty()) {
            return List.of();
        }
        // 익명 응답의 답변은 노출 안 함 (기명 전용). 같은 formResponseId 는 응답도 동일하므로 첫 원소로 판정.
        if (answers.get(0).getFormResponse().getRespondentMemberId() == null) {
            return List.of();
        }
        return buildAnswerInfos(answers);
    }

    @Override
    public Map<Long, List<AnswerInfo>> listByFormResponseIds(Set<Long> formResponseIds) {
        if (formResponseIds == null || formResponseIds.isEmpty()) {
            return Map.of();
        }

        List<Answer> answers = loadAnswerPort.listByFormResponseIds(formResponseIds).stream()
            .filter(a -> a.getFormResponse().getRespondentMemberId() != null)
            .toList();
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

    @Override
    public Optional<AnswerInfo> findByIdAsAnonymous(Long answerId, String responseAccessKey) {
        if (responseAccessKey == null) {
            throw new FormDomainException(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
        }
        // 인증 실패 유출 방지 — 익명/hash 검증 실패 시 silently empty
        return loadAnswerPort.findById(answerId)
            .filter(answer -> isAuthorizedAnonymous(answer.getFormResponse(), responseAccessKey))
            .map(this::toAnswerInfo);
    }

    @Override
    public AnswerInfo getByIdAsAnonymous(Long answerId, String responseAccessKey) {
        if (responseAccessKey == null) {
            throw new FormDomainException(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
        }
        // 익명 경계 유출 방지 — 답변 없음 / 기명 응답 / hash 불일치 모두 FORBIDDEN 으로 통일
        Answer answer = loadAnswerPort.findById(answerId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN));
        if (!isAuthorizedAnonymous(answer.getFormResponse(), responseAccessKey)) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }
        return toAnswerInfo(answer);
    }

    @Override
    public List<AnswerInfo> listByFormResponseIdAsAnonymous(Long formResponseId, String responseAccessKey) {
        if (responseAccessKey == null) {
            throw new FormDomainException(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
        }
        List<Answer> answers = loadAnswerPort.listByFormResponseId(formResponseId);
        if (answers.isEmpty()) {
            // 응답 존재 여부 유출 방지 — 빈 리스트로 통일
            return List.of();
        }
        if (!isAuthorizedAnonymous(answers.get(0).getFormResponse(), responseAccessKey)) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
        }
        return buildAnswerInfos(answers);
    }

    /**
     * FormResponse 가 익명이고 저장된 hash 가 rawKey 의 sha256 과 일치하는지 확인.
     */
    private boolean isAuthorizedAnonymous(FormResponse response, String rawAccessKey) {
        if (response.getRespondentMemberId() != null) {
            return false;
        }
        String hash = secureTokenGenerator.sha256Hex(rawAccessKey);
        return hash.equals(response.getResponseAccessKeyHash());
    }

    /**
     * 답변 목록에서 AnswerChoice 를 벌크 로드해 AnswerInfo 로 조립.
     */
    private List<AnswerInfo> buildAnswerInfos(List<Answer> answers) {
        Set<Long> answerIds = answers.stream()
            .map(Answer::getId)
            .collect(Collectors.toSet());
        List<AnswerChoice> allChoices = loadAnswerPort.listChoicesByAnswerIdIn(answerIds);
        Map<Long, List<AnswerChoice>> choicesByAnswer = allChoices.stream()
            .collect(Collectors.groupingBy(c -> c.getAnswer().getId()));

        return answers.stream()
            .map(answer -> AnswerInfo.from(
                answer,
                choicesByAnswer.getOrDefault(answer.getId(), List.of())
            ))
            .toList();
    }

    /**
     * 단건 Answer -> AnswerInfo. 해당 답변의 AnswerChoice 만 조회 후 조립.
     */
    private AnswerInfo toAnswerInfo(Answer answer) {
        List<AnswerChoice> choices = loadAnswerPort.listChoicesByAnswerIdIn(Set.of(answer.getId()));
        return AnswerInfo.from(answer, choices);
    }
}
