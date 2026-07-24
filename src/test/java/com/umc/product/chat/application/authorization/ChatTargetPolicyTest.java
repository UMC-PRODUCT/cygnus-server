package com.umc.product.chat.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.PolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.application.service.policy.RegisteredPolicyEvaluationService;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

class ChatTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private TargetChatAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new ChatPolicyBundleContributor()));
        target = new TargetChatAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("방 멤버는 membership 기반 Chat action을 수행한다")
    void allowsMembershipActions() {
        for (ChatPolicyAction action : List.of(
            ChatPolicyAction.ROOM_READ,
            ChatPolicyAction.ROOM_SUMMARY_READ,
            ChatPolicyAction.MESSAGE_CREATE,
            ChatPolicyAction.MESSAGE_READ,
            ChatPolicyAction.REACTION_UPDATE,
            ChatPolicyAction.READ_UPDATE,
            ChatPolicyAction.READ_STATUS)) {
            assertThat(target.evaluate(context(action, true, false, false)))
                .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        }
    }

    @Test
    @DisplayName("메시지 수정은 방 멤버이면서 작성자인 경우에만 허용한다")
    void updateRequiresMembershipAndAuthor() {
        assertThat(target.evaluate(context(ChatPolicyAction.MESSAGE_UPDATE, true, true, false)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(ChatPolicyAction.MESSAGE_UPDATE, true, false, true)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
        assertThat(target.evaluate(context(ChatPolicyAction.MESSAGE_UPDATE, false, true, false)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("메시지 삭제는 방 멤버인 작성자 또는 moderator에게 허용한다")
    void deleteRequiresMembershipAndAuthorOrModerator() {
        assertThat(target.evaluate(context(ChatPolicyAction.MESSAGE_DELETE, true, true, false)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(ChatPolicyAction.MESSAGE_DELETE, true, false, true)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(ChatPolicyAction.MESSAGE_DELETE, false, true, true)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
        assertThat(target.evaluate(context(ChatPolicyAction.MESSAGE_DELETE, true, false, false)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("방 멤버가 아니면 membership 기반 Chat action은 기본 거부한다")
    void deniesNonMember() {
        for (ChatPolicyAction action : ChatPolicyAction.values()) {
            assertThat(target.evaluate(context(action, false, false, false)))
                .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
        }
    }

    private ChatAuthorizationContext context(
        ChatPolicyAction action,
        boolean roomMember,
        boolean messageAuthor,
        boolean moderator
    ) {
        return new ChatAuthorizationContext(
            action,
            roomMember,
            messageAuthor,
            moderator,
            EVALUATED_AT);
    }
}
