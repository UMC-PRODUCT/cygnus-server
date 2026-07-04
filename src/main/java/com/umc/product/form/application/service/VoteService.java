package com.umc.product.form.application.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageVoteUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateVoteCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormCommand;
import com.umc.product.form.application.port.out.SaveFormPort;
import com.umc.product.form.application.port.out.SaveFormSectionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VoteService implements ManageVoteUseCase {

    private final SaveFormPort saveFormPort;
    private final SaveFormSectionPort saveFormSectionPort;
    private final SaveQuestionPort saveQuestionPort;
    private final SaveQuestionOptionPort saveQuestionOptionPort;
    private final ManageFormUseCase manageFormUseCase;

    @Audited(
        domain = Domain.FORM,
        action = AuditAction.CREATE,
        targetType = "Vote",
        targetId = "#result",
        description = "'투표를 생성했습니다.'"
    )
    @Override
    public Long createVote(CreateVoteCommand command) {
        QuestionType qType = command.allowMultipleChoice()
            ? QuestionType.CHECKBOX
            : QuestionType.RADIO;

        // 1. 순수한 Form 생성 (상태는 무조건 PUBLISHED)
        Form form = Form.createPublished(command.createdMemberId(), command.title(), command.isAnonymous());
        Form savedForm = saveFormPort.save(form);

        // 2. 단일 섹션 생성
        FormSection section = FormSection.create(
            savedForm,
            command.title(),
            null,
            1L
        );

        FormSection savedSection = saveFormSectionPort.save(section);

        // 3. 단일 질문 생성
        Question question = Question.create(
            command.title(),
            qType,
            true, // isRequired
            1L    // orderNo
        );
        question.assignTo(savedSection);
        Question savedQuestion = saveQuestionPort.save(question);

        // 4. 질문에 대한 선택지(옵션) 생성
        AtomicInteger order = new AtomicInteger(1);
        List<QuestionOption> options = command.options().stream()
            .map(optContent -> {
                QuestionOption opt = QuestionOption.create(optContent, order.getAndIncrement(), false);
                opt.assignTo(savedQuestion);
                return opt;
            })
            .collect(Collectors.toList());
        saveQuestionOptionPort.saveAll(options);

        // 조립된 Form의 ID 반환 (이 ID를 NoticeVote가 voteId라는 이름으로 가집니다)
        return savedForm.getId();
    }

    @Override
    public void deleteVote(Long formId) {
        manageFormUseCase.deleteForm(
            DeleteFormCommand.builder()
                .formId(formId)
                .build()
        );
    }
}
