package com.umc.product.test.adapter.out.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.umc.product.test.application.port.out.DeleteSeedProjectDataPort;
import com.umc.product.test.application.port.out.dto.ProjectDataDeletionCounts;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ProjectSeedDataCleanupPersistenceAdapter implements DeleteSeedProjectDataPort {

    /*
     * 대상 Project 관련 데이터:
     * - project: 프로젝트 루트. gisu_id로 대상 기수를 판별한다.
     * - project_member: project, project_application을 참조하는 프로젝트 멤버.
     * - project_part_quota: project를 참조하는 파트별 TO.
     * - project_matching_round: chapter_id를 통해 대상 기수 chapter의 매칭 차수를 판별한다.
     * - project_application: project_application_form, project_matching_round를 참조하는 지원서 메타데이터.
     * - project_application_form: project와 form form을 연결한다. form_id는 현재 DB FK가 아닌 스칼라 ID다.
     * - project_application_form_policy: project_application_form과 form_section_id를 연결하는 섹션 노출 정책.
     * - form, form_section, question, question_option: 프로젝트 지원 폼의 설문 구조.
     * - form_response, answer, answer_choice: 프로젝트 지원 폼에 제출된 신규 설문 응답 구조.
     * - single_answer: answer/answer_choice로 이관된 뒤에도 남아 있는 레거시 설문 응답 테이블.
     *
     * FK 안전 삭제 순서:
     * 1. project_member: project_application, project보다 먼저 삭제한다.
     * 2. answer_choice: answer, question_option보다 먼저 삭제한다.
     * 3. single_answer: form_response, question보다 먼저 삭제한다.
     * 4. answer: form_response, question보다 먼저 삭제한다.
     * 5. project_application: project_application_form, project_matching_round보다 먼저 삭제한다.
     * 6. project_application_form_policy: project_application_form보다 먼저 삭제한다.
     * 7. question_option: question보다 먼저 삭제한다.
     * 8. question: form_section보다 먼저 삭제한다.
     * 9. form_section: form보다 먼저 삭제한다.
     * 10. form_response: form보다 먼저 삭제한다.
     * 11. form: project_application_form.form_id가 FK가 아니므로 연결 행보다 먼저 삭제할 수 있다.
     * 12. project_application_form: project보다 먼저 삭제한다.
     * 13. project_part_quota: project보다 먼저 삭제한다.
     * 14. project_matching_round: project_application 삭제 이후 대상 기수 chapter 기준으로 삭제한다.
     * 15. project: 모든 하위 프로젝트 데이터를 제거한 뒤 마지막에 삭제한다.
     */
    private final EntityManager entityManager;

    @Override
    public ProjectDataDeletionCounts deleteByGisuId(Long gisuId) {
        int deletedProjectMembers = execute("""
            DELETE FROM project_member pm
            WHERE pm.project_id IN (
                SELECT p.id
                FROM project p
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedSurveyAnswerChoices = execute("""
            DELETE FROM answer_choice ac
            WHERE ac.answer_id IN (
                SELECT a.id
                FROM answer a
                JOIN form_response fr ON fr.id = a.form_response_id
                JOIN project_application_form paf ON paf.form_id = fr.form_id
                JOIN project p ON p.id = paf.project_id
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedSurveySingleAnswers = execute("""
            DELETE FROM single_answer sa
            WHERE sa.response_id IN (
                SELECT fr.id
                FROM form_response fr
                JOIN project_application_form paf ON paf.form_id = fr.form_id
                JOIN project p ON p.id = paf.project_id
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedSurveyAnswers = execute("""
            DELETE FROM answer a
            WHERE a.form_response_id IN (
                SELECT fr.id
                FROM form_response fr
                JOIN project_application_form paf ON paf.form_id = fr.form_id
                JOIN project p ON p.id = paf.project_id
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedProjectApplications = execute("""
            DELETE FROM project_application pa
            WHERE pa.project_application_form_id IN (
                SELECT paf.id
                FROM project_application_form paf
                JOIN project p ON p.id = paf.project_id
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedProjectApplicationFormPolicies = execute("""
            DELETE FROM project_application_form_policy pfp
            WHERE pfp.project_application_form_id IN (
                SELECT paf.id
                FROM project_application_form paf
                JOIN project p ON p.id = paf.project_id
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedSurveyQuestionOptions = execute("""
            DELETE FROM question_option qo
            WHERE qo.question_id IN (
                SELECT q.id
                FROM question q
                JOIN form_section fs ON fs.id = q.form_section_id
                JOIN project_application_form paf ON paf.form_id = fs.form_id
                JOIN project p ON p.id = paf.project_id
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedSurveyQuestions = execute("""
            DELETE FROM question q
            WHERE q.form_section_id IN (
                SELECT fs.id
                FROM form_section fs
                JOIN project_application_form paf ON paf.form_id = fs.form_id
                JOIN project p ON p.id = paf.project_id
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedSurveyFormSections = execute("""
            DELETE FROM form_section fs
            WHERE fs.form_id IN (
                SELECT paf.form_id
                FROM project_application_form paf
                JOIN project p ON p.id = paf.project_id
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedSurveyFormResponses = execute("""
            DELETE FROM form_response fr
            WHERE fr.form_id IN (
                SELECT paf.form_id
                FROM project_application_form paf
                JOIN project p ON p.id = paf.project_id
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedSurveyForms = execute("""
            DELETE FROM form f
            WHERE f.id IN (
                SELECT paf.form_id
                FROM project_application_form paf
                JOIN project p ON p.id = paf.project_id
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedProjectApplicationForms = execute("""
            DELETE FROM project_application_form paf
            WHERE paf.project_id IN (
                SELECT p.id
                FROM project p
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedProjectPartQuotas = execute("""
            DELETE FROM project_part_quota ppq
            WHERE ppq.project_id IN (
                SELECT p.id
                FROM project p
                WHERE p.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedProjectMatchingRounds = execute("""
            DELETE FROM project_matching_round pmr
            WHERE pmr.chapter_id IN (
                SELECT c.id
                FROM chapter c
                WHERE c.gisu_id = :gisuId
            )
            """, gisuId);

        int deletedProjects = execute("""
            DELETE FROM project p
            WHERE p.gisu_id = :gisuId
            """, gisuId);

        return ProjectDataDeletionCounts.builder()
            .deletedProjects(deletedProjects)
            .deletedProjectMembers(deletedProjectMembers)
            .deletedProjectApplications(deletedProjectApplications)
            .deletedProjectApplicationForms(deletedProjectApplicationForms)
            .deletedProjectApplicationFormPolicies(deletedProjectApplicationFormPolicies)
            .deletedProjectPartQuotas(deletedProjectPartQuotas)
            .deletedProjectMatchingRounds(deletedProjectMatchingRounds)
            .deletedSurveyForms(deletedSurveyForms)
            .deletedSurveyFormSections(deletedSurveyFormSections)
            .deletedSurveyQuestions(deletedSurveyQuestions)
            .deletedSurveyQuestionOptions(deletedSurveyQuestionOptions)
            .deletedSurveyFormResponses(deletedSurveyFormResponses)
            .deletedSurveyAnswers(deletedSurveyAnswers)
            .deletedSurveyAnswerChoices(deletedSurveyAnswerChoices)
            .deletedSurveySingleAnswers(deletedSurveySingleAnswers)
            .build();
    }

    private int execute(String sql, Long gisuId) {
        return entityManager.createNativeQuery(sql)
            .setParameter("gisuId", gisuId)
            .executeUpdate();
    }
}
