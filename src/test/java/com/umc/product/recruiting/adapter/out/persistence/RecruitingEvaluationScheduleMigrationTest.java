package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.PersistenceException;

@PersistenceAdapterTest
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class
})
class RecruitingEvaluationScheduleMigrationTest extends RecruitingPersistenceAdapterTestSupport {

    @Test
    @DisplayName("legacy 평가 배정 테이블을 제거하고 신규 평가 일정 테이블을 생성한다")
    void legacy_평가_배정_테이블을_제거하고_신규_평가_일정_테이블을_생성한다() {
        @SuppressWarnings("unchecked")
        List<String> tableNames = em.getEntityManager().createNativeQuery("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name LIKE 'recruiting_%'
            """)
            .getResultList();

        assertThat(tableNames).contains(
            "recruiting_application_evaluation",
            "recruiting_interview_schedule"
        );
        assertThat(tableNames).doesNotContain(
            "recruiting_evaluation_template",
            "recruiting_evaluation_criterion",
            "recruiting_interview_assignment",
            "recruiting_interview_evaluation"
        );
    }

    @Test
    @DisplayName("SENT 메일 상태에서 sentAt이 없는 면접 일정은 데이터베이스가 거부한다")
    void SENT_메일_상태에서_sentAt이_없는_면접_일정은_데이터베이스가_거부한다() {
        RecruitingGraph graph = persistApplicationGraph(
            9L,
            90L,
            1,
            "schedule:mail-check",
            RecruitingApplicationStatus.DOCUMENT_PASSED
        );
        em.flush();

        assertThatThrownBy(() -> em.getEntityManager().createNativeQuery("""
            INSERT INTO recruiting_interview_schedule (
                created_at, updated_at, recruiting_application_id, status, contact_snapshot,
                request_mail_status, request_mail_attempts,
                confirmation_mail_status, confirmation_mail_attempts
            ) VALUES (
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :applicationId,
                'AVAILABILITY_REQUESTED', '카카오톡 @umc',
                'SENT', 1, 'PENDING', 0
            )
            """)
            .setParameter("applicationId", graph.application().getId())
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }
}
