package com.umc.product.form.application.service.command;

import com.umc.product.form.application.port.in.command.ManageQuestionOptionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionOptionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionOptionCommand;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionOptionCommandService implements ManageQuestionOptionUseCase {

    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuestionOptionPort loadQuestionOptionPort;
    private final SaveQuestionOptionPort saveQuestionOptionPort;

    @Override
    public Long createOption(CreateQuestionOptionCommand command) {
        Question question = loadQuestionPort.findById(command.questionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        long nextOrderNo = loadQuestionOptionPort.listByQuestionId(command.questionId()).stream()
            .mapToLong(QuestionOption::getOrderNo)
            .max()
            .orElse(0L) + 1L;

        QuestionOption option = QuestionOption.create(
            command.content(),
            nextOrderNo,
            command.isOther()
        );
        option.assignTo(question);

        return saveQuestionOptionPort.save(option).getId();
    }

    @Override
    public void updateOption(UpdateQuestionOptionCommand command) {
        QuestionOption option = loadQuestionOptionPort.findById(command.optionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        option.update(command.content(), command.isOther());
        saveQuestionOptionPort.save(option);
    }

    @Override
    public void deleteOption(DeleteQuestionOptionCommand command) {
        saveQuestionOptionPort.deleteById(command.optionId());
    }

    @Override
    public void reorderOptions(ReorderQuestionOptionsCommand command) {
        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionId(command.questionId());

        Set<Long> existingIds = options.stream()
            .map(QuestionOption::getId)
            .collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(command.orderedOptionIds());

        if (!existingIds.equals(requestedIds)) {
            throw new FormDomainException(
                FormErrorCode.INVALID_VOTE_FORM_STRUCTURE,
                "재배치 요청의 선택지 ID 셋이 실제 질문의 선택지 ID 셋과 일치하지 않습니다."
            );
        }

        Map<Long, QuestionOption> byId = options.stream()
            .collect(Collectors.toMap(QuestionOption::getId, Function.identity()));

        for (int i = 0; i < command.orderedOptionIds().size(); i++) {
            byId.get(command.orderedOptionIds().get(i)).updateOrderNo(i + 1);
        }

        saveQuestionOptionPort.saveAll(options);
    }
}
