package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.in.command.ManageAnswerUseCase;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.CreateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("Form Answer usage cascade")
class FormAnswerUsageCascadeIntegrationTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 10L;
    private static final String FILE_ID = "shared-form-file";

    @Autowired
    ManageFormUseCase manageFormUseCase;
    @Autowired
    ManageFormSectionUseCase manageFormSectionUseCase;
    @Autowired
    ManageQuestionUseCase manageQuestionUseCase;
    @Autowired
    ManageFormResponseUseCase manageFormResponseUseCase;
    @Autowired
    ManageAnswerUseCase manageAnswerUseCase;
    @Autowired
    SaveFileMetadataPort saveFileMetadataPort;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("shared file은 첫 response 삭제 후 유지되고 form 삭제에서 마지막 usage가 detach된다")
    void responseAndFormDeleteDetachSharedFileUsages() {
        saveFile();
        FormFixture fixture = createPublishedFileForm();
        Long firstResponseId = createDraftAnswer(fixture);
        createDraftAnswer(fixture);
        assertThat(usageCount()).isEqualTo(2);

        manageFormResponseUseCase.deleteDraft(
            fixture.owner(),
            actor(),
            DeleteDraftFormResponseCommand.builder().formResponseId(firstResponseId).build()
        );

        assertThat(usageCount()).isOne();
        assertThat(unreferencedAt()).isNull();

        manageFormUseCase.deleteForm(
            fixture.owner(),
            actor(),
            DeleteFormCommand.builder().formId(fixture.formId()).build()
        );

        assertThat(usageCount()).isZero();
        assertThat(unreferencedAt()).isNotNull();
    }

    private FormFixture createPublishedFileForm() {
        FormOwnerReferenceFactory factory = FormOwnerReferenceFactory.standalone();
        Long formId = manageFormUseCase.createDraft(
            factory,
            actor(),
            CreateDraftFormCommand.builder()
                .title("usage cascade")
                .allowDuplicateResponses(true)
                .build()
        );
        FormOwnerReference owner = factory.create(formId);
        Long sectionId = manageFormSectionUseCase.createSection(
            owner,
            actor(),
            CreateFormSectionCommand.builder().formId(formId).title("section").build()
        );
        Long questionId = manageQuestionUseCase.createQuestion(
            owner,
            actor(),
            CreateQuestionCommand.builder()
                .sectionId(sectionId)
                .type(QuestionType.FILE)
                .title("attachment")
                .isRequired(false)
                .build()
        );
        manageFormUseCase.publishForm(
            owner,
            actor(),
            PublishFormCommand.builder().formId(formId).build()
        );
        return new FormFixture(formId, questionId, owner);
    }

    private Long createDraftAnswer(FormFixture fixture) {
        Long responseId = manageFormResponseUseCase.createDraft(
            fixture.owner(),
            actor(),
            CreateDraftFormResponseCommand.builder().formId(fixture.formId()).build()
        );
        manageAnswerUseCase.createAnswer(
            fixture.owner(),
            actor(),
            CreateAnswerCommand.builder()
                .formResponseId(responseId)
                .questionId(fixture.questionId())
                .fileIds(List.of(FILE_ID))
                .build()
        );
        return responseId;
    }

    private void saveFile() {
        FileMetadata metadata = FileMetadata.builder()
            .fileId(FILE_ID)
            .originalFileName("shared.pdf")
            .category(FileCategory.POST_IMAGE)
            .contentType("application/pdf")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("test/shared-form-file")
            .uploadedMemberId(MEMBER_ID)
            .build();
        metadata.markAsUploaded(Instant.parse("2026-07-18T00:00:00Z"));
        saveFileMetadataPort.save(metadata);
    }

    private FormActorContext actor() {
        return FormActorContext.authenticated(MEMBER_ID);
    }

    private long usageCount() {
        return jdbcTemplate.queryForObject(
            "select count(*) from file_usage where file_id = ?", Long.class, FILE_ID);
    }

    private Instant unreferencedAt() {
        return jdbcTemplate.queryForObject(
            "select unreferenced_at from file_metadata where id = ?", Instant.class, FILE_ID);
    }

    private record FormFixture(Long formId, Long questionId, FormOwnerReference owner) {
    }
}
