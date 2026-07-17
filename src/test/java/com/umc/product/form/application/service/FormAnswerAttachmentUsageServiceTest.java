package com.umc.product.form.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Form Answer attachment usage 동기화")
class FormAnswerAttachmentUsageServiceTest {

    @Mock
    ManageFileUsageUseCase manageFileUsageUseCase;
    @Mock
    LoadAnswerPort loadAnswerPort;

    @InjectMocks
    FormAnswerAttachmentUsageService sut;

    @Test
    @DisplayName("FILE Answer 저장 직후 answer ID exact snapshot을 등록한다")
    void synchronizeFileAnswer() {
        Answer answer = answer(41L, QuestionType.FILE, Set.of("file-b", "file-a"));

        sut.synchronize(answer, 7L);

        ArgumentCaptor<ReplaceFileUsagesCommand> captor =
            ArgumentCaptor.forClass(ReplaceFileUsagesCommand.class);
        then(manageFileUsageUseCase).should().replaceUsages(captor.capture());
        assertThat(captor.getValue().owner()).isEqualTo(
            FileUsageCoordinate.of("form.answer", "41", "attachments"));
        assertThat(captor.getValue().fileIds()).containsExactlyInAnyOrder("file-a", "file-b");
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("응답 삭제 전 attachment Answer ID를 한 번에 detach한다")
    void detachByFormResponseId() {
        given(loadAnswerPort.listAttachmentIdsByFormResponseId(31L)).willReturn(List.of(43L, 41L));

        sut.detachByFormResponseId(31L);

        ArgumentCaptor<BulkRemoveFileUsagesCommand> captor =
            ArgumentCaptor.forClass(BulkRemoveFileUsagesCommand.class);
        then(manageFileUsageUseCase).should().removeAll(captor.capture());
        assertThat(captor.getValue().owners()).containsExactly(
            FileUsageCoordinate.of("form.answer", "43", "attachments"),
            FileUsageCoordinate.of("form.answer", "41", "attachments")
        );
    }

    @Test
    @DisplayName("익명 legacy snapshot은 null 유지, empty clear, subset 제거만 허용한다")
    void resolveAnonymousSnapshot() {
        Set<String> existing = Set.of("legacy-a", "legacy-b");

        assertThat(sut.resolveAnonymousSnapshot(existing, null)).containsExactlyInAnyOrderElementsOf(existing);
        assertThat(sut.resolveAnonymousSnapshot(existing, List.of())).isEmpty();
        assertThat(sut.resolveAnonymousSnapshot(existing, List.of("legacy-b"))).containsExactly("legacy-b");
        assertThatThrownBy(() -> sut.resolveAnonymousSnapshot(existing, List.of("legacy-a", "new-file")))
            .isInstanceOf(StorageException.class)
            .hasFieldOrPropertyWithValue("baseCode", StorageErrorCode.FILE_USE_FORBIDDEN);
    }

    private Answer answer(Long answerId, QuestionType type, Set<String> fileIds) {
        Form form = Form.createDraft("폼", 7L, null, true);
        ReflectionTestUtils.setField(form, "id", 11L);
        FormResponse response = FormResponse.createDraft(form, 7L);
        ReflectionTestUtils.setField(response, "id", 31L);
        FormSection section = FormSection.create(form, "섹션", null, 1L);
        Question question = Question.create("질문", type, false, 1L);
        question.assignTo(section);
        Answer answer = Answer.create(response, question, type, null, fileIds);
        ReflectionTestUtils.setField(answer, "id", answerId);
        return answer;
    }
}
