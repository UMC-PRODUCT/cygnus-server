package com.umc.product.form.application.service.query;

import com.umc.product.form.application.port.in.query.GetVoteUseCase;
import com.umc.product.form.application.port.in.query.dto.VoteInfo;
import com.umc.product.form.application.port.in.query.dto.VoteInfo.VoteOptionInfo;
import com.umc.product.form.application.port.out.*;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VoteQueryService implements GetVoteUseCase {

    private final LoadFormPort loadFormPort;
    private final LoadAnswerPort loadAnswerPort;
    private final LoadFormSectionPort loadFormSectionPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuestionOptionPort loadQuestionOptionPort;

    @Override
    public VoteInfo getVoteInfo(Long formId, Long memberId) {
        Form form = loadFormPort.findById(formId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        // 1. 투표 통계 데이터 로드
        Map<Long, Long> counts = loadAnswerPort.countVotesByOptionId(formId);
        long totalParticipants = loadAnswerPort.countTotalParticipants(formId);
        List<Long> mySelections = loadAnswerPort.findSelectedOptionIdsByMember(formId, memberId);

        // 2. 투표 구조 로드 (섹션 -> 질문 -> 옵션)
        // 공식적으로는 1섹션 1질문 구조를 전제로 하지만, 확장성을 고려하여 조회
        FormSection section = loadFormSectionPort.listByFormId(formId).stream()
            .findFirst()
            .orElseThrow(() -> new FormDomainException(FormErrorCode.INVALID_VOTE_FORM_STRUCTURE));

        Question question = loadQuestionPort.findAllByFormSectionIdIn(Set.of(section.getId())).stream()
            .findFirst()
            .orElseThrow(() -> new FormDomainException(FormErrorCode.INVALID_VOTE_FORM_STRUCTURE));

        boolean allowMultipleChoice = (question.getType() == QuestionType.CHECKBOX);
        List<QuestionOption> questionOptions = loadQuestionOptionPort.listByQuestionId(question.getId());

        // 3. 옵션별 투표자 ID 목록 로드 (익명인 경우 빈 맵)
        Map<Long, List<Long>> selectedMemberIdsByOptionId = form.isAnonymous()
            ? Map.of()
            : loadAnswerPort.findSelectedMemberIdsByOptionId(formId);

        // 4. 옵션 정보 및 득표율 계산
        List<VoteOptionInfo> optionInfos = questionOptions.stream()
            .map(opt -> {
                long voteCount = counts.getOrDefault(opt.getId(), 0L);
                BigDecimal voteRate = calculateVoteRate(voteCount, totalParticipants);
                List<Long> selectedMemberIds = selectedMemberIdsByOptionId.getOrDefault(opt.getId(), List.of());

                return new VoteOptionInfo(
                    opt.getId(),
                    opt.getContent(),
                    voteCount,
                    voteRate,
                    selectedMemberIds
                );
            })
            .toList();

        return new VoteInfo(
            form.getId(),
            form.getTitle(),
            form.isAnonymous(),
            allowMultipleChoice,
            totalParticipants,
            mySelections,
            optionInfos
        );
    }

    @Override
    public Long getPrimaryQuestionId(Long voteId) {
        List<Question> questions = loadQuestionPort.listByFormId(voteId);
        if (questions.isEmpty()) {
            throw new FormDomainException(FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);
        }
        return questions.get(0).getId();
    }

    private BigDecimal calculateVoteRate(long voteCount, long totalParticipants) {
        if (totalParticipants == 0L) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(voteCount)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalParticipants), 1, RoundingMode.HALF_UP);
    }
}
