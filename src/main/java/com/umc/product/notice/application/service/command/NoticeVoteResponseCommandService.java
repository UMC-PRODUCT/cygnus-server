package com.umc.product.notice.application.service.command;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormResponseCommand;
import com.umc.product.form.application.port.in.query.GetVoteUseCase;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.notice.application.port.in.command.ManageNoticeVoteResponseUseCase;
import com.umc.product.notice.application.port.in.command.dto.SubmitNoticeVoteResponseCommand;
import com.umc.product.notice.application.port.in.command.dto.UpdateNoticeVoteResponseCommand;
import com.umc.product.notice.application.port.out.LoadNoticeVotePort;
import com.umc.product.notice.domain.NoticeVote;
import com.umc.product.notice.domain.enums.VoteStatus;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.notice.domain.exception.NoticeErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class NoticeVoteResponseCommandService implements ManageNoticeVoteResponseUseCase {

    private final LoadNoticeVotePort loadNoticeVotePort;
    private final GetVoteUseCase getVoteUseCase;
    private final ManageFormResponseUseCase manageFormResponseUseCase;

    @Audited(
        domain = Domain.NOTICE,
        action = AuditAction.SUBMIT,
        targetType = "NoticeVoteResponse",
        targetId = "#result",
        description = "'공지 투표 응답을 제출했습니다.'"
    )
    @Override
    public Long submit(SubmitNoticeVoteResponseCommand command) {
        NoticeVote noticeVote = loadOpenNoticeVote(command.noticeId());
        Long questionId = getVoteUseCase.getPrimaryQuestionId(noticeVote.getVoteId());

        return manageFormResponseUseCase.submitImmediately(
            SubmitFormResponseCommand.builder()
                .formId(noticeVote.getVoteId())
                .respondentMemberId(command.respondentMemberId())
                .answers(List.of(buildAnswerCommand(questionId, command.selectedOptionIds())))
                .build()
        );
    }

    @Override
    public void updateOrCancel(UpdateNoticeVoteResponseCommand command) {
        if (command.selectedOptionIds() == null) {
            throw new NoticeDomainException(NoticeErrorCode.SELECTED_OPTION_IDS_REQUIRED);
        }

        NoticeVote noticeVote = loadOpenNoticeVote(command.noticeId());

        if (command.selectedOptionIds().isEmpty()) {
            manageFormResponseUseCase.deleteResponse(
                DeleteFormResponseCommand.builder()
                    .formId(noticeVote.getVoteId())
                    .respondentMemberId(command.respondentMemberId())
                    .build()
            );
            return;
        }

        Long questionId = getVoteUseCase.getPrimaryQuestionId(noticeVote.getVoteId());
        manageFormResponseUseCase.updateResponse(
            UpdateFormResponseCommand.builder()
                .formId(noticeVote.getVoteId())
                .respondentMemberId(command.respondentMemberId())
                .answers(List.of(buildAnswerCommand(questionId, command.selectedOptionIds())))
                .build()
        );
    }

    private NoticeVote loadOpenNoticeVote(Long noticeId) {
        NoticeVote noticeVote = loadNoticeVotePort.findVoteByNoticeId(noticeId)
            .orElseThrow(() -> new NoticeDomainException(NoticeErrorCode.NOTICE_VOTE_NOT_FOUND));

        VoteStatus status = noticeVote.getOpenStatus(Instant.now());
        if (status == VoteStatus.NOT_STARTED) {
            throw new NoticeDomainException(NoticeErrorCode.VOTE_NOT_STARTED);
        }
        if (status == VoteStatus.CLOSED) {
            throw new NoticeDomainException(NoticeErrorCode.VOTE_CLOSED);
        }

        return noticeVote;
    }

    private AnswerCommand buildAnswerCommand(Long questionId, List<Long> selectedOptionIds) {
        return AnswerCommand.builder()
            .questionId(questionId)
            .selectedOptionIds(selectedOptionIds)
            .build();
    }
}
