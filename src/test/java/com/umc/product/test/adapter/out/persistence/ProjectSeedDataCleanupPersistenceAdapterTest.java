package com.umc.product.test.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.support.PersistenceAdapterTest;
import com.umc.product.test.application.port.out.dto.ProjectDataDeletionCounts;

import jakarta.persistence.Query;

@PersistenceAdapterTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.seed.enabled=true")
@Import({
    ProjectSeedDataCleanupPersistenceAdapter.class
})
class ProjectSeedDataCleanupPersistenceAdapterTest {

    private static final Instant GISU_START_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant GISU_END_AT = Instant.parse("2026-12-31T00:00:00Z");
    private static final Instant ROUND_STARTS_AT = Instant.parse("2026-03-01T00:00:00Z");
    private static final Instant ROUND_ENDS_AT = Instant.parse("2026-03-02T00:00:00Z");
    private static final Instant ROUND_DECISION_DEADLINE = Instant.parse("2026-03-03T00:00:00Z");

    @Autowired
    TestEntityManager em;

    @Autowired
    ProjectSeedDataCleanupPersistenceAdapter sut;

    @Test
    @DisplayName("대상 기수의 프로젝트 관련 데이터만 모두 삭제한다")
    void deleteOnlyTargetGisuProjectData() {
        // Given
        Gisu targetGisu = persistGisu(11L);
        Gisu otherGisu = persistGisu(12L);
        Chapter targetChapter = persistChapter(targetGisu, "target");
        Chapter otherChapter = persistChapter(otherGisu, "other");
        ProjectGraph target = persistProjectGraph(targetGisu.getId(), targetChapter.getId(), 100L);
        ProjectGraph other = persistProjectGraph(otherGisu.getId(), otherChapter.getId(), 200L);
        em.flush();
        em.clear();

        // When
        ProjectDataDeletionCounts result = sut.deleteByGisuId(targetGisu.getId());
        em.flush();
        em.clear();

        // Then
        assertThat(result.deletedProjects()).isEqualTo(1);
        assertThat(result.deletedProjectMembers()).isEqualTo(1);
        assertThat(result.deletedProjectApplications()).isEqualTo(1);
        assertThat(result.deletedProjectApplicationForms()).isEqualTo(1);
        assertThat(result.deletedProjectApplicationFormPolicies()).isEqualTo(1);
        assertThat(result.deletedProjectPartQuotas()).isEqualTo(1);
        assertThat(result.deletedProjectMatchingRounds()).isEqualTo(1);
        assertThat(result.deletedSurveyForms()).isEqualTo(1);
        assertThat(result.deletedSurveyFormSections()).isEqualTo(1);
        assertThat(result.deletedSurveyQuestions()).isEqualTo(1);
        assertThat(result.deletedSurveyQuestionOptions()).isEqualTo(1);
        assertThat(result.deletedSurveyFormResponses()).isEqualTo(1);
        assertThat(result.deletedSurveyAnswers()).isEqualTo(1);
        assertThat(result.deletedSurveyAnswerChoices()).isEqualTo(1);
        assertThat(result.deletedSurveySingleAnswers()).isEqualTo(1);

        assertGraphDeleted(target);
        assertGraphExists(other);
    }

    private Gisu persistGisu(Long generation) {
        Gisu gisu = Gisu.create(generation, GISU_START_AT, GISU_END_AT, false);
        em.persist(gisu);
        return gisu;
    }

    private Chapter persistChapter(Gisu gisu, String suffix) {
        Chapter chapter = Chapter.create(gisu, "chapter-" + suffix);
        em.persist(chapter);
        return chapter;
    }

    private ProjectGraph persistProjectGraph(Long gisuId, Long chapterId, Long seed) {
        Project project = Project.createDraft(gisuId, chapterId, seed, seed, seed);
        em.persist(project);

        ProjectMatchingRound round = ProjectMatchingRound.create(
            "round-" + seed,
            null,
            MatchingType.PLAN_DESIGN,
            MatchingPhase.FIRST,
            chapterId,
            ROUND_STARTS_AT,
            ROUND_ENDS_AT,
            ROUND_DECISION_DEADLINE
        );
        em.persist(round);

        Form form = Form.createPublished(seed, "form-" + seed, false);
        em.persist(form);

        FormSection section = FormSection.create(form, "section-" + seed, null, 1L);
        em.persist(section);

        Question question = Question.create("question-" + seed, QuestionType.RADIO, true, 1L);
        question.assignTo(section);
        em.persist(question);

        QuestionOption option = QuestionOption.create("option-" + seed, 1L, false);
        option.assignTo(question);
        em.persist(option);

        FormResponse formResponse = FormResponse.createDraft(form, seed + 1000);
        formResponse.submit(Instant.now(), "127.0.0.1");
        em.persist(formResponse);

        Answer answer = Answer.create(formResponse, question, QuestionType.RADIO, null, null, null);
        em.persist(answer);

        AnswerChoice answerChoice = AnswerChoice.create(answer, option);
        em.persist(answerChoice);

        em.flush();
        Long singleAnswerId = insertLegacySingleAnswer(formResponse.getId(), question.getId(), option.getId());

        ProjectApplicationForm applicationForm = ProjectApplicationForm.create(project, form.getId());
        em.persist(applicationForm);

        ProjectApplicationFormPolicy policy = ProjectApplicationFormPolicy.createCommon(
            applicationForm,
            section.getId()
        );
        em.persist(policy);

        ProjectApplication application = ProjectApplication.create(
            applicationForm,
            formResponse.getId(),
            seed + 1000,
            round
        );
        em.persist(application);

        ProjectMember member = ProjectMember.create(project, seed + 1000, ChallengerPart.WEB, seed);
        em.persist(member);

        ProjectPartQuota quota = ProjectPartQuota.create(project, ChallengerPart.WEB, 3L, seed);
        em.persist(quota);

        return new ProjectGraph(
            project.getId(),
            member.getId(),
            quota.getId(),
            round.getId(),
            application.getId(),
            applicationForm.getId(),
            policy.getId(),
            form.getId(),
            section.getId(),
            question.getId(),
            option.getId(),
            formResponse.getId(),
            answer.getId(),
            answerChoice.getId(),
            singleAnswerId
        );
    }

    private Long insertLegacySingleAnswer(Long responseId, Long questionId, Long optionId) {
        // single_answer는 현재 JPA 엔티티가 없는 레거시 테이블이라 삭제 경로 검증에 필요한 행만 직접 삽입한다.
        Query query = em.getEntityManager().createNativeQuery("""
            INSERT INTO single_answer (
                created_at, updated_at, response_id, question_id, answered_as_type, value
            )
            VALUES (
                now(), now(), :responseId, :questionId, 'RADIO',
                jsonb_build_object('selectedOptionId', :optionId)
            )
            RETURNING id
            """);
        query.setParameter("responseId", responseId);
        query.setParameter("questionId", questionId);
        query.setParameter("optionId", optionId);
        return ((Number) query.getSingleResult()).longValue();
    }

    private <T> boolean existsById(Class<T> entityClass, Long id) {
        return em.find(entityClass, id) != null;
    }

    private boolean existsLegacySingleAnswerById(Long id) {
        Object result = em.getEntityManager()
            .createNativeQuery("SELECT EXISTS (SELECT 1 FROM single_answer WHERE id = :id)")
            .setParameter("id", id)
            .getSingleResult();
        return (Boolean) result;
    }

    private void assertGraphDeleted(ProjectGraph graph) {
        assertGraphState(graph, false);
    }

    private void assertGraphExists(ProjectGraph graph) {
        assertGraphState(graph, true);
    }

    private void assertGraphState(ProjectGraph graph, boolean expected) {
        assertThat(existsById(Project.class, graph.projectId())).isEqualTo(expected);
        assertThat(existsById(ProjectMember.class, graph.memberId())).isEqualTo(expected);
        assertThat(existsById(ProjectPartQuota.class, graph.quotaId())).isEqualTo(expected);
        assertThat(existsById(ProjectMatchingRound.class, graph.roundId())).isEqualTo(expected);
        assertThat(existsById(ProjectApplication.class, graph.applicationId())).isEqualTo(expected);
        assertThat(existsById(ProjectApplicationForm.class, graph.applicationFormId())).isEqualTo(expected);
        assertThat(existsById(ProjectApplicationFormPolicy.class, graph.policyId())).isEqualTo(expected);
        assertThat(existsById(Form.class, graph.formId())).isEqualTo(expected);
        assertThat(existsById(FormSection.class, graph.sectionId())).isEqualTo(expected);
        assertThat(existsById(Question.class, graph.questionId())).isEqualTo(expected);
        assertThat(existsById(QuestionOption.class, graph.optionId())).isEqualTo(expected);
        assertThat(existsById(FormResponse.class, graph.formResponseId())).isEqualTo(expected);
        assertThat(existsById(Answer.class, graph.answerId())).isEqualTo(expected);
        assertThat(existsById(AnswerChoice.class, graph.answerChoiceId())).isEqualTo(expected);
        assertThat(existsLegacySingleAnswerById(graph.singleAnswerId())).isEqualTo(expected);
    }

    private record ProjectGraph(
        Long projectId,
        Long memberId,
        Long quotaId,
        Long roundId,
        Long applicationId,
        Long applicationFormId,
        Long policyId,
        Long formId,
        Long sectionId,
        Long questionId,
        Long optionId,
        Long formResponseId,
        Long answerId,
        Long answerChoiceId,
        Long singleAnswerId
    ) {
    }
}
