package com.umc.product.feedback.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.domain.Form;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(UserFeedbackTemplateQueryRepository.class)
class UserFeedbackTemplateQueryRepositoryTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    UserFeedbackTemplateQueryRepository sut;

    @Test
    @DisplayName("context, targetType, active 선택 필터를 모든 조합으로 적용한다")
    void 선택_필터_모든_조합() {
        // given
        entityManager.getEntityManager()
            .createNativeQuery("DELETE FROM user_feedback_template")
            .executeUpdate();
        UserFeedbackTemplate submittedNewActive = persist(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER,
            true,
            101L
        );
        UserFeedbackTemplate submittedExperiencedActive = persist(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.EXPERIENCED_CHALLENGER,
            true,
            102L
        );
        UserFeedbackTemplate submittedNewInactive = persist(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER,
            false,
            103L
        );
        UserFeedbackTemplate matchingNewActive = persist(
            UserFeedbackContext.MATCHING_COMPLETED,
            UserFeedbackTargetType.NEW_CHALLENGER,
            true,
            104L
        );
        entityManager.flush();
        entityManager.clear();

        // when & then
        assertIds(
            sut.findByCondition(UserFeedbackContext.APPLICATION_SUBMITTED, null, null),
            submittedNewActive,
            submittedExperiencedActive,
            submittedNewInactive
        );
        assertIds(
            sut.findByCondition(null, UserFeedbackTargetType.NEW_CHALLENGER, null),
            submittedNewActive,
            submittedNewInactive,
            matchingNewActive
        );
        assertIds(
            sut.findByCondition(null, null, true),
            submittedNewActive,
            submittedExperiencedActive,
            matchingNewActive
        );
        assertIds(
            sut.findByCondition(
                UserFeedbackContext.APPLICATION_SUBMITTED,
                UserFeedbackTargetType.NEW_CHALLENGER,
                null
            ),
            submittedNewActive,
            submittedNewInactive
        );
        assertIds(
            sut.findByCondition(UserFeedbackContext.APPLICATION_SUBMITTED, null, true),
            submittedNewActive,
            submittedExperiencedActive
        );
        assertIds(
            sut.findByCondition(null, UserFeedbackTargetType.NEW_CHALLENGER, true),
            submittedNewActive,
            matchingNewActive
        );
        assertIds(
            sut.findByCondition(
                UserFeedbackContext.APPLICATION_SUBMITTED,
                UserFeedbackTargetType.NEW_CHALLENGER,
                true
            ),
            submittedNewActive
        );

        List<UserFeedbackTemplate> unfiltered = sut.findByCondition(null, null, null);
        assertIds(
            unfiltered,
            submittedNewActive,
            submittedExperiencedActive,
            submittedNewInactive,
            matchingNewActive
        );
        assertThat(unfiltered).isSortedAccordingTo(
            Comparator.comparing(UserFeedbackTemplate::getUpdatedAt, Comparator.reverseOrder())
                .thenComparing(UserFeedbackTemplate::getId, Comparator.reverseOrder())
        );
    }

    private UserFeedbackTemplate persist(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType,
        boolean active,
        Long formId
    ) {
        Form form = Form.createDraft("폼 " + formId, 1L);
        entityManager.persist(form);
        UserFeedbackTemplate template = UserFeedbackTemplate.create(context, targetType, form.getId());
        if (!active) {
            template.deactivate();
        }
        entityManager.persist(template);
        return template;
    }

    private void assertIds(List<UserFeedbackTemplate> actual, UserFeedbackTemplate... expected) {
        assertThat(actual)
            .extracting(UserFeedbackTemplate::getId)
            .containsExactlyInAnyOrder(
                List.of(expected).stream()
                    .map(UserFeedbackTemplate::getId)
                    .toArray(Long[]::new)
            );
    }
}
