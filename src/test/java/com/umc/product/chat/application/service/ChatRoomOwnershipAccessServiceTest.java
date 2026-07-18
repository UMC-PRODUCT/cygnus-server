package com.umc.product.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.policy.ChatRoomOwnerPolicyRegistry;
import com.umc.product.chat.application.policy.ChatStandaloneRoomOwnerPolicy;
import com.umc.product.chat.application.port.out.ChatRoomOwnerPolicy;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomOwnershipPort;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.registry.application.port.in.query.GetRegistryReadinessUseCase;
import com.umc.product.registry.domain.OwnershipEnforcementMode;
import com.umc.product.registry.domain.RegistryName;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomOwnershipAccessService")
class ChatRoomOwnershipAccessServiceTest {

    private static final Long ROOM_ID = 11L;
    private static final Long ACTOR_ID = 101L;

    @Mock
    LoadChatRoomOwnershipPort loadOwnershipPort;
    @Mock
    LoadChatMemberPort loadChatMemberPort;
    @Mock
    ChatRoomOwnerPolicy ownerPolicy;
    @Mock
    GetRegistryReadinessUseCase registryReadiness;
    @Mock
    OperationalMetrics operationalMetrics;

    ChatRoomOwnerReference expectedOwner;
    ChatRoomActorContext actorContext;
    ChatRoomOwnershipAccessService sut;

    @BeforeEach
    void setUp() {
        expectedOwner = ChatRoomOwnerReference.standalone(ROOM_ID);
        actorContext = ChatRoomActorContext.actor(ACTOR_ID);
        given(ownerPolicy.namespace()).willReturn(ChatRoomOwnerReference.STANDALONE_NAMESPACE);
        ChatRoomOwnerPolicyRegistry registry = new ChatRoomOwnerPolicyRegistry(List.of(ownerPolicy));
        sut = new ChatRoomOwnershipAccessService(
            registry,
            loadOwnershipPort,
            loadChatMemberPort,
            registryReadiness,
            operationalMetrics
        );
        Mockito.clearInvocations(ownerPolicy);
    }

    @Test
    @DisplayName("정확한 owner의 READ는 policy 성공 후 기존 Chat membership까지 확인한다")
    void verifyRead_requiresPolicyThenMembership() {
        given(loadOwnershipPort.findByRoomId(ROOM_ID)).willReturn(Optional.of(expectedOwner));
        given(ownerPolicy.allows(expectedOwner, ChatRoomOperation.READ, actorContext)).willReturn(true);
        given(loadChatMemberPort.existsByRoomIdAndMemberId(ROOM_ID, ACTOR_ID)).willReturn(true);

        sut.verify(expectedOwner, ChatRoomOperation.READ, actorContext);

        InOrder order = Mockito.inOrder(loadOwnershipPort, ownerPolicy, loadChatMemberPort);
        order.verify(loadOwnershipPort).findByRoomId(ROOM_ID);
        order.verify(ownerPolicy).allows(expectedOwner, ChatRoomOperation.READ, actorContext);
        order.verify(loadChatMemberPort).existsByRoomIdAndMemberId(ROOM_ID, ACTOR_ID);
    }

    @Test
    @DisplayName("mutation은 ownership binding을 lock 조회한 뒤 policy를 평가한다")
    void verifyForUpdate_locksBindingBeforePolicy() {
        given(loadOwnershipPort.findByRoomIdForUpdate(ROOM_ID)).willReturn(Optional.of(expectedOwner));
        given(ownerPolicy.allows(expectedOwner, ChatRoomOperation.JOIN, actorContext)).willReturn(true);

        sut.verifyForUpdate(expectedOwner, ChatRoomOperation.JOIN, actorContext);

        InOrder order = Mockito.inOrder(loadOwnershipPort, ownerPolicy);
        order.verify(loadOwnershipPort).findByRoomIdForUpdate(ROOM_ID);
        order.verify(ownerPolicy).allows(expectedOwner, ChatRoomOperation.JOIN, actorContext);
    }

    @Test
    @DisplayName("ownership binding이 없으면 policy를 평가하지 않고 거부한다")
    void verify_missingBinding_denied() {
        given(loadOwnershipPort.findByRoomId(ROOM_ID)).willReturn(Optional.empty());

        assertAccessDenied(() -> sut.verify(expectedOwner, ChatRoomOperation.READ, actorContext));

        then(ownerPolicy).shouldHaveNoInteractions();
        then(loadChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("audit은 policy와 membership을 통과한 missing legacy binding만 허용한다")
    void audit_missingBinding_requiresPolicyAndMembership() {
        GetRegistryReadinessUseCase readiness = Mockito.mock(GetRegistryReadinessUseCase.class);
        OperationalMetrics metrics = Mockito.mock(OperationalMetrics.class);
        ChatRoomOwnershipAccessService auditSut = new ChatRoomOwnershipAccessService(
            new ChatRoomOwnerPolicyRegistry(List.of(ownerPolicy)),
            loadOwnershipPort,
            loadChatMemberPort,
            readiness,
            metrics
        );
        given(readiness.ownershipMode(RegistryName.CHAT_OWNERSHIP))
            .willReturn(OwnershipEnforcementMode.AUDIT);
        given(loadOwnershipPort.findByRoomId(ROOM_ID)).willReturn(Optional.empty());
        given(ownerPolicy.allows(expectedOwner, ChatRoomOperation.READ, actorContext)).willReturn(true);
        given(loadChatMemberPort.existsByRoomIdAndMemberId(ROOM_ID, ACTOR_ID)).willReturn(true);

        auditSut.verify(expectedOwner, ChatRoomOperation.READ, actorContext);

        then(metrics).should().recordSecurityEvent(
            "chat", "ownership_missing_binding", "audit_allowed");
    }

    @Test
    @DisplayName("server expected owner가 null이면 evaluator나 binding을 조회하지 않고 거부한다")
    void verify_nullExpectedOwner_denied() {
        assertAccessDenied(() -> sut.verify(null, ChatRoomOperation.READ, actorContext));

        then(ownerPolicy).shouldHaveNoInteractions();
        then(loadOwnershipPort).shouldHaveNoInteractions();
        then(loadChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("namespace policy가 거부하면 membership으로 우회하지 못한다")
    void verify_policyDenied_stopsBeforeMembership() {
        given(loadOwnershipPort.findByRoomId(ROOM_ID)).willReturn(Optional.of(expectedOwner));
        given(ownerPolicy.allows(expectedOwner, ChatRoomOperation.READ, actorContext)).willReturn(false);

        assertAccessDenied(() -> sut.verify(expectedOwner, ChatRoomOperation.READ, actorContext));

        then(loadChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("서버가 기대한 owner와 저장 binding이 다르면 policy를 평가하지 않고 거부한다")
    void verify_wrongOwner_denied() {
        ChatRoomOwnerReference persisted = ChatRoomOwnerReference.of(
            ROOM_ID, ChatRoomOwnerReference.STANDALONE_NAMESPACE, "999", ChatRoomOwnerReference.DEFAULT_SLOT);
        given(loadOwnershipPort.findByRoomId(ROOM_ID)).willReturn(Optional.of(persisted));

        assertAccessDenied(() -> sut.verify(expectedOwner, ChatRoomOperation.READ, actorContext));

        then(ownerPolicy).shouldHaveNoInteractions();
        then(loadChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("등록되지 않은 namespace는 binding 조회 전에 fail-closed한다")
    void verify_unknownNamespace_denied() {
        ChatRoomOwnerReference unknown = expectedOwner.withNamespace("chat.unknown");

        assertAccessDenied(() -> sut.verify(unknown, ChatRoomOperation.READ, actorContext));

        then(loadOwnershipPort).shouldHaveNoInteractions();
        then(loadChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("등록된 다른 namespace로 owner를 위조해도 persisted binding exact compare에서 거부한다")
    void verify_forgedRegisteredNamespace_denied() {
        ChatRoomOwnerPolicy futurePolicy = Mockito.mock(ChatRoomOwnerPolicy.class);
        given(futurePolicy.namespace()).willReturn("chat.future");
        ChatRoomOwnerPolicyRegistry registry = new ChatRoomOwnerPolicyRegistry(List.of(ownerPolicy, futurePolicy));
        sut = new ChatRoomOwnershipAccessService(
            registry,
            loadOwnershipPort,
            loadChatMemberPort,
            registryReadiness,
            operationalMetrics
        );
        Mockito.clearInvocations(ownerPolicy, futurePolicy);
        ChatRoomOwnerReference forged = expectedOwner.withNamespace("chat.future");
        given(loadOwnershipPort.findByRoomId(ROOM_ID)).willReturn(Optional.of(expectedOwner));

        assertAccessDenied(() -> sut.verify(forged, ChatRoomOperation.READ, actorContext));

        then(futurePolicy).shouldHaveNoInteractions();
        then(ownerPolicy).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 namespace evaluator가 중복되면 registry 구성을 거부한다")
    void registry_duplicateEvaluator_rejected() {
        ChatRoomOwnerPolicy duplicate = Mockito.mock(ChatRoomOwnerPolicy.class);
        given(duplicate.namespace()).willReturn(ChatRoomOwnerReference.STANDALONE_NAMESPACE);

        assertThatThrownBy(() -> new ChatRoomOwnerPolicyRegistry(List.of(ownerPolicy, duplicate)))
            .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(value = ChatRoomOperation.class, names = {"READ", "SEND"})
    @DisplayName("standalone member는 READ와 SEND를 policy와 membership 모두 통과한다")
    void standaloneMember_readAndSend_allowed(ChatRoomOperation operation) {
        ChatRoomOwnerPolicyRegistry registry = new ChatRoomOwnerPolicyRegistry(
            List.of(new ChatStandaloneRoomOwnerPolicy()));
        sut = new ChatRoomOwnershipAccessService(
            registry,
            loadOwnershipPort,
            loadChatMemberPort,
            registryReadiness,
            operationalMetrics
        );
        given(loadOwnershipPort.findByRoomId(ROOM_ID)).willReturn(Optional.of(expectedOwner));
        given(loadChatMemberPort.existsByRoomIdAndMemberId(ROOM_ID, ACTOR_ID)).willReturn(true);

        sut.verify(expectedOwner, operation, actorContext);

        then(loadChatMemberPort).should().existsByRoomIdAndMemberId(ROOM_ID, ACTOR_ID);
    }

    @ParameterizedTest
    @EnumSource(value = ChatRoomOperation.class, names = {"READ", "SEND"})
    @DisplayName("standalone nonmember는 policy가 허용해도 READ와 SEND를 거부한다")
    void standaloneNonmember_readAndSend_denied(ChatRoomOperation operation) {
        ChatRoomOwnerPolicyRegistry registry = new ChatRoomOwnerPolicyRegistry(
            List.of(new ChatStandaloneRoomOwnerPolicy()));
        sut = new ChatRoomOwnershipAccessService(
            registry,
            loadOwnershipPort,
            loadChatMemberPort,
            registryReadiness,
            operationalMetrics
        );
        given(loadOwnershipPort.findByRoomId(ROOM_ID)).willReturn(Optional.of(expectedOwner));
        given(loadChatMemberPort.existsByRoomIdAndMemberId(ROOM_ID, ACTOR_ID)).willReturn(false);

        assertAccessDenied(() -> sut.verify(expectedOwner, operation, actorContext));
    }

    @Test
    @DisplayName("standalone policy는 authenticated CREATE와 자기 JOIN/LEAVE를 허용한다")
    void standalone_createAndSelfMembership_allowed() {
        ChatStandaloneRoomOwnerPolicy policy = new ChatStandaloneRoomOwnerPolicy();
        ChatRoomActorContext self = ChatRoomActorContext.actorAndTarget(ACTOR_ID, ACTOR_ID);

        assertThat(policy.allows(expectedOwner, ChatRoomOperation.CREATE, actorContext)).isTrue();
        assertThat(policy.allows(expectedOwner, ChatRoomOperation.JOIN, self)).isTrue();
        assertThat(policy.allows(expectedOwner, ChatRoomOperation.LEAVE, self)).isTrue();
    }

    @Test
    @DisplayName("standalone CREATE는 creator ID를 owner key로 위조한 binding을 거부한다")
    void standalone_creatorIdOwnerKey_denied() {
        ChatRoomOwnerReference inferredFromCreator = ChatRoomOwnerReference.of(
            ROOM_ID, ChatRoomOwnerReference.STANDALONE_NAMESPACE, ACTOR_ID.toString(),
            ChatRoomOwnerReference.DEFAULT_SLOT);

        assertThat(new ChatStandaloneRoomOwnerPolicy().allows(
            inferredFromCreator, ChatRoomOperation.CREATE, actorContext)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatRoomOperation.class, names = {"PIN", "UNPIN", "DELETE", "MEMBERSHIP_MANAGE"})
    @DisplayName("standalone policy는 모든 management operation을 거부한다")
    void standalone_management_denied(ChatRoomOperation operation) {
        ChatStandaloneRoomOwnerPolicy policy = new ChatStandaloneRoomOwnerPolicy();

        assertThat(policy.allows(expectedOwner, operation, actorContext)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatRoomOperation.class, names = {"JOIN", "LEAVE"})
    @DisplayName("standalone policy는 requester와 target이 다른 membership mutation을 거부한다")
    void standalone_otherMembership_denied(ChatRoomOperation operation) {
        ChatStandaloneRoomOwnerPolicy policy = new ChatStandaloneRoomOwnerPolicy();
        ChatRoomActorContext other = ChatRoomActorContext.actorAndTarget(ACTOR_ID, 202L);

        assertThat(policy.allows(expectedOwner, operation, other)).isFalse();
    }

    private void assertAccessDenied(Runnable invocation) {
        assertThatThrownBy(invocation::run)
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }
}
