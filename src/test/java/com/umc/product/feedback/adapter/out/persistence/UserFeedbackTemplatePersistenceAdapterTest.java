package com.umc.product.feedback.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.feedback.domain.exception.FeedbackDomainException;

@ExtendWith(MockitoExtension.class)
class UserFeedbackTemplatePersistenceAdapterTest {

    @Mock
    UserFeedbackTemplateJpaRepository repository;

    @Test
    @DisplayName("context와 target type에 맞는 활성 템플릿을 조회한다")
    void context와_target_type에_맞는_활성_템플릿을_조회한다() {
        UserFeedbackTemplate template = template();
        given(repository.findByContextAndTargetTypeAndIsActiveTrue(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER
        )).willReturn(Optional.of(template));

        assertThat(sut().findByContextAndTargetType(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER
        )).contains(template);
    }

    @Test
    @DisplayName("필수 템플릿을 ID로 조회한다")
    void 필수_템플릿을_ID로_조회한다() {
        UserFeedbackTemplate template = template();
        given(repository.findById(1L)).willReturn(Optional.of(template));

        assertThat(sut().getById(1L)).isSameAs(template);
    }

    @Test
    @DisplayName("필수 템플릿이 없으면 domain not-found 예외를 던진다")
    void 필수_템플릿이_없으면_domain_not_found_예외를_던진다() {
        given(repository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut().getById(1L))
            .isInstanceOf(FeedbackDomainException.class);
    }

    private UserFeedbackTemplatePersistenceAdapter sut() {
        return new UserFeedbackTemplatePersistenceAdapter(repository);
    }

    private UserFeedbackTemplate template() {
        return UserFeedbackTemplate.create(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER,
            200L
        );
    }
}
