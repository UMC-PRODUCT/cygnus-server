package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.OAuthTokenLoginResult;
import com.umc.product.authentication.application.port.out.RevokeOAuthTokenPort;
import com.umc.product.authentication.application.port.out.SaveMemberOAuthPort;
import com.umc.product.authentication.application.port.out.VerifyOAuthTokenPort;
import com.umc.product.authentication.domain.MemberOAuth;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.event.adapter.out.persistence.EventOutboxJpaRepository;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.MemberFixture;

@TestPropertySource(properties = "app.event-outbox.relay-enabled=false")
@DisplayName("Apple 로그인 감사 이벤트 트랜잭션")
class OAuthAuthenticationAuditIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OAuthAuthenticationUseCase oAuthAuthenticationUseCase;

    @Autowired
    private MemberFixture memberFixture;

    @Autowired
    private SaveMemberOAuthPort saveMemberOAuthPort;

    @Autowired
    private EventOutboxJpaRepository eventOutboxRepository;

    @MockitoBean
    private VerifyOAuthTokenPort verifyOAuthTokenPort;

    @MockitoBean
    private RevokeOAuthTokenPort revokeOAuthTokenPort;

    @Test
    @DisplayName("기존 Apple 회원의 로그인 결과와 감사 outbox가 함께 정상 처리된다")
    void 기존_Apple_회원_로그인과_감사_outbox를_커밋한다() throws Exception {
        // Given
        Long memberId = memberFixture.일반("applelogin").getId();
        OAuthAttributes attributes = OAuthAttributes.of("apple", Map.of(
            "sub", "existing-apple-user", "email", "apple@example.com"
        ));
        saveMemberOAuthPort.save(MemberOAuth.builder()
            .memberId(memberId)
            .provider(OAuthProvider.APPLE)
            .providerId(attributes.providerId())
            .build());

        // When
        OAuthTokenLoginResult result = oAuthAuthenticationUseCase.loginWithOAuthAttributes(attributes);

        // Then
        assertThat(result).isEqualTo(OAuthTokenLoginResult.existingMember(
            memberId, OAuthProvider.APPLE, attributes.providerId(), attributes.email()
        ));
        assertCommittedLoginAudit(memberId.toString(), true);
    }

    @Test
    @DisplayName("신규 Apple 사용자의 가입 필요 결과와 감사 outbox가 함께 정상 처리된다")
    void 신규_Apple_사용자_결과와_감사_outbox를_커밋한다() throws Exception {
        // Given
        OAuthAttributes attributes = OAuthAttributes.of("apple", Map.of("sub", "new-apple-user"));

        // When
        OAuthTokenLoginResult result = oAuthAuthenticationUseCase.loginWithOAuthAttributes(attributes);

        // Then
        assertThat(result).isEqualTo(OAuthTokenLoginResult.newMember(
            OAuthProvider.APPLE, attributes.providerId(), null
        ));
        assertCommittedLoginAudit(null, false);
    }

    private void assertCommittedLoginAudit(String memberId, boolean existingMember) throws Exception {
        List<EventOutbox> loginOutboxes = eventOutboxRepository.findAll().stream()
            .filter(event -> event.getEventType().equals("audit.log.login"))
            .toList();
        assertThat(loginOutboxes).hasSize(1);
        EventOutbox outbox = loginOutboxes.getFirst();
        assertThat(outbox.getId()).isNotNull();
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        AuditLogEvent event = objectMapper.readValue(outbox.getPayload(), AuditLogEvent.class);
        assertThat(event.domain()).isEqualTo(Domain.AUTHENTICATION);
        assertThat(event.action()).isEqualTo(AuditAction.LOGIN);
        assertThat(event.targetType()).isEqualTo("OAuthAuthentication");
        assertThat(event.targetId()).isEqualTo(memberId);
        assertThat(event.description()).contains("provider=APPLE", "existingMember=" + existingMember);
        verifyNoInteractions(verifyOAuthTokenPort, revokeOAuthTokenPort);
    }
}
