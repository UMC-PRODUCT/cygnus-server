package com.umc.product.form.application.service.command;

import static com.umc.product.form.application.service.FormAccessTestFixtures.owner;
import static com.umc.product.form.application.service.FormAccessTestFixtures.responseActor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveAnswerPort;
import com.umc.product.form.application.port.out.SaveFormResponsePort;
import com.umc.product.form.application.service.FormAnswerAttachmentUsageService;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("익명 FormResponse attachment full update")
class FormResponseAnonymousAttachmentUsageTest {

    private static final Long FORM_ID = 10L;
    private static final Long RESPONSE_ID = 20L;
    private static final Long QUESTION_ID = 30L;
    private static final Long ANSWER_ID = 40L;
    private static final String RAW_KEY = "raw-key";
    private static final String KEY_HASH = "key-hash";

    @Mock
    LoadFormPort loadFormPort;
    @Mock
    LoadFormSectionPort loadFormSectionPort;
    @Mock
    LoadQuestionPort loadQuestionPort;
    @Mock
    LoadQuestionOptionPort loadQuestionOptionPort;
    @Mock
    LoadFormResponsePort loadFormResponsePort;
    @Mock
    LoadAnswerPort loadAnswerPort;
    @Mock
    SaveFormResponsePort saveFormResponsePort;
    @Mock
    SaveAnswerPort saveAnswerPort;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    FormOwnershipAccessService ownershipAccessService;
    @Mock
    SecureTokenGenerator secureTokenGenerator;
    @Mock
    FormAnswerAttachmentUsageService attachmentUsageService;

    @InjectMocks
    FormResponseCommandService sut;

    FormResponse response;
    Question question;
    Answer existing;

    @BeforeEach
    void setUp() {
        Form form = Form.createDraft("폼", 1L, null, true);
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        form.publish();
        response = FormResponse.createAnonymousDraft(form, KEY_HASH);
        ReflectionTestUtils.setField(response, "id", RESPONSE_ID);
        FormSection section = FormSection.create(form, "섹션", null, 1L);
        question = Question.create("파일", QuestionType.FILE, false, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);
        existing = Answer.create(
            response, question, QuestionType.FILE, null, Set.of("legacy-a", "legacy-b"));
        ReflectionTestUtils.setField(existing, "id", ANSWER_ID);

        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(KEY_HASH);
        given(loadFormResponsePort.findDraftByAccessKeyHash(KEY_HASH)).willReturn(Optional.of(response));
        given(loadAnswerPort.listByFormResponseId(RESPONSE_ID)).willReturn(List.of(existing));
        given(loadQuestionPort.listByFormId(FORM_ID)).willReturn(List.of(question));
        given(saveAnswerPort.save(existing)).willReturn(existing);
    }

    @Test
    @DisplayName("same-question subset은 Answer ID를 보존하고 일부 attachment만 제거한다")
    void subsetPreservesAnswerId() {
        given(attachmentUsageService.resolveAnonymousSnapshot(
            existing.getFileIds(), List.of("legacy-b"))).willReturn(Set.of("legacy-b"));

        update(List.of("legacy-b"));

        assertThat(existing.getId()).isEqualTo(ANSWER_ID);
        assertThat(existing.getFileIds()).containsExactly("legacy-b");
        then(saveAnswerPort).should(never()).deleteAllByFormResponseId(RESPONSE_ID);
        then(attachmentUsageService).should().synchronize(existing, null);
    }

    @Test
    @DisplayName("same-question null은 Answer ID와 legacy attachment snapshot을 유지한다")
    void nullKeepsAnswerIdAndSnapshot() {
        given(attachmentUsageService.resolveAnonymousSnapshot(existing.getFileIds(), null))
            .willReturn(existing.getFileIds());

        update(null);

        assertThat(existing.getId()).isEqualTo(ANSWER_ID);
        assertThat(existing.getFileIds()).containsExactlyInAnyOrder("legacy-a", "legacy-b");
        then(attachmentUsageService).should().synchronize(existing, null);
    }

    @Test
    @DisplayName("same-question empty는 Answer ID를 보존하고 attachment snapshot을 비운다")
    void emptyClearsSnapshotWithoutReplacingAnswer() {
        given(attachmentUsageService.resolveAnonymousSnapshot(existing.getFileIds(), List.of()))
            .willReturn(Set.of());

        update(List.of());

        assertThat(existing.getId()).isEqualTo(ANSWER_ID);
        assertThat(existing.getFileIds()).isNull();
        then(attachmentUsageService).should().synchronize(existing, null);
    }

    private void update(List<String> fileIds) {
        sut.updateAnonymousDraft(
            owner(FORM_ID),
            responseActor(RAW_KEY),
            UpdateAnonymousDraftFormResponseCommand.builder()
                .answers(List.of(AnswerCommand.builder()
                    .questionId(QUESTION_ID)
                    .fileIds(fileIds)
                    .build()))
                .build()
        );
    }
}
