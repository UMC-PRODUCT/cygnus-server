package com.umc.product.form.application.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FormAnswerAttachmentUsageService {

    private static final String NAMESPACE = "form.answer";
    private static final String SLOT = "attachments";

    private final ManageFileUsageUseCase manageFileUsageUseCase;
    private final LoadAnswerPort loadAnswerPort;

    public void synchronize(Answer answer, Long requesterMemberId) {
        if (!isAttachmentAnswer(answer)) {
            return;
        }
        Set<String> fileIds = answer.getFileIds() == null
            ? Set.of()
            : new LinkedHashSet<>(answer.getFileIds());
        manageFileUsageUseCase.replaceUsages(new ReplaceFileUsagesCommand(
            coordinate(answer.getId()),
            fileIds,
            requesterMemberId
        ));
    }

    public Set<String> resolveAnonymousSnapshot(Set<String> currentFileIds, List<String> requestedFileIds) {
        Set<String> current = currentFileIds == null
            ? Set.of()
            : new LinkedHashSet<>(currentFileIds);
        if (requestedFileIds == null) {
            return Collections.unmodifiableSet(current);
        }

        Set<String> requested = new LinkedHashSet<>(requestedFileIds);
        if (!current.containsAll(requested)) {
            throw new StorageException(StorageErrorCode.FILE_USE_FORBIDDEN);
        }
        return Collections.unmodifiableSet(requested);
    }

    public void detachAnswer(Answer answer) {
        if (isAttachmentAnswer(answer)) {
            detach(List.of(answer.getId()));
        }
    }

    public void detachAnswers(List<Answer> answers) {
        detach(answers.stream()
            .filter(FormAnswerAttachmentUsageService::isAttachmentAnswer)
            .map(Answer::getId)
            .toList());
    }

    public void detachByFormResponseId(Long formResponseId) {
        detach(loadAnswerPort.listAttachmentIdsByFormResponseId(formResponseId));
    }

    public void detachByFormId(Long formId) {
        detach(loadAnswerPort.listAttachmentIdsByFormId(formId));
    }

    public void detachByQuestionId(Long questionId) {
        detach(loadAnswerPort.listAttachmentIdsByQuestionId(questionId));
    }

    public static boolean isAttachmentAnswer(Answer answer) {
        return answer.getAnsweredAsType() == QuestionType.FILE
            || answer.getAnsweredAsType() == QuestionType.PORTFOLIO;
    }

    private void detach(List<Long> answerIds) {
        if (answerIds.isEmpty()) {
            return;
        }
        List<FileUsageCoordinate> coordinates = answerIds.stream()
            .map(FormAnswerAttachmentUsageService::coordinate)
            .toList();
        manageFileUsageUseCase.removeAll(new BulkRemoveFileUsagesCommand(coordinates));
    }

    private static FileUsageCoordinate coordinate(Long answerId) {
        return FileUsageCoordinate.of(NAMESPACE, answerId.toString(), SLOT);
    }
}
