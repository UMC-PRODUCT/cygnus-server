package com.umc.product.form.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
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
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.support.IntegrationTestSupport;

@Import(FormAnswerUsageTransactionIntegrationTest.FailureConfig.class)
@DisplayName("Form Answer와 usage transaction")
class FormAnswerUsageTransactionIntegrationTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 10L;

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
    FailingManageFileUsageUseCase failingManageFileUsageUseCase;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void disarmFailure() {
        failingManageFileUsageUseCase.disarm();
    }

    @Test
    @DisplayName("answer 저장 후 usage 실패는 answer를 rollback한다")
    void usageFailureRollsBackAnswer() {
        FormFixture fixture = createPublishedForm(QuestionType.PORTFOLIO);
        Long responseId = manageFormResponseUseCase.createDraft(
            fixture.owner(),
            actor(),
            CreateDraftFormResponseCommand.builder().formId(fixture.formId()).build()
        );
        failingManageFileUsageUseCase.arm();

        assertThatThrownBy(() -> manageAnswerUseCase.createAnswer(
            fixture.owner(),
            actor(),
            CreateAnswerCommand.builder()
                .formResponseId(responseId)
                .questionId(fixture.questionId())
                .textValue("rollback")
                .build()
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("injected usage failure");

        assertThat(count("answer")).isZero();
        assertThat(count("form_response")).isOne();
    }

    private FormFixture createPublishedForm(QuestionType questionType) {
        FormOwnerReferenceFactory factory = FormOwnerReferenceFactory.standalone();
        Long formId = manageFormUseCase.createDraft(
            factory,
            actor(),
            CreateDraftFormCommand.builder()
                .title("usage transaction")
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
                .type(questionType)
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

    private FormActorContext actor() {
        return FormActorContext.authenticated(MEMBER_ID);
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }

    private record FormFixture(Long formId, Long questionId, FormOwnerReference owner) {
    }

    @TestConfiguration
    static class FailureConfig {

        @Bean
        @Primary
        FailingManageFileUsageUseCase failingManageFileUsageUseCase() {
            return new FailingManageFileUsageUseCase();
        }
    }

    static final class FailingManageFileUsageUseCase implements ManageFileUsageUseCase {

        private boolean armed;

        void arm() {
            armed = true;
        }

        void disarm() {
            armed = false;
        }

        @Override
        public void replaceUsages(ReplaceFileUsagesCommand command) {
            if (armed) {
                throw new IllegalStateException("injected usage failure");
            }
        }

        @Override
        public void removeAll(BulkRemoveFileUsagesCommand command) {
        }
    }
}
