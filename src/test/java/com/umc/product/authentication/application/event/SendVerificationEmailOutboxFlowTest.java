package com.umc.product.authentication.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.application.port.out.DeleteRefreshTokenPort;
import com.umc.product.authentication.application.port.out.LoadEmailVerificationPort;
import com.umc.product.authentication.application.port.out.LoadRefreshTokenPort;
import com.umc.product.authentication.application.port.out.LoadSsoClientPort;
import com.umc.product.authentication.application.port.out.SaveEmailVerificationPort;
import com.umc.product.authentication.application.service.AuthenticationService;
import com.umc.product.authentication.application.service.AuthenticationTokenIssuer;
import com.umc.product.authentication.domain.EmailVerification;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.adapter.out.OutboxDomainEventPublisher;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.member.application.port.in.query.GetMemberCredentialUseCase;

import io.micrometer.tracing.Tracer;

@DisplayName("SendVerificationEmailEvent outbox flow")
@ExtendWith(MockitoExtension.class)
class SendVerificationEmailOutboxFlowTest {

    private static final String EMAIL = "alice@example.com";

    @Mock
    private LoadEmailVerificationPort loadEmailVerificationPort;

    @Mock
    private SaveEmailVerificationPort saveEmailVerificationPort;

    @Mock
    private LoadRefreshTokenPort loadRefreshTokenPort;

    @Mock
    private DeleteRefreshTokenPort deleteRefreshTokenPort;

    @Mock
    private LoadSsoClientPort loadSsoClientPort;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationTokenIssuer authenticationTokenIssuer;

    @Mock
    private GetMemberCredentialUseCase getMemberCredentialUseCase;

    @Test
    @DisplayName("이메일 인증 세션 생성 시 SendVerificationEmailEvent가 event outbox로 저장된다")
    void send_verification_email_event_outbox_저장() {
        FakeSaveEventOutboxPort saveEventOutboxPort = new FakeSaveEventOutboxPort();
        AuthenticationService service = new AuthenticationService(
            loadEmailVerificationPort,
            saveEmailVerificationPort,
            loadRefreshTokenPort,
            deleteRefreshTokenPort,
            loadSsoClientPort,
            jwtTokenProvider,
            authenticationTokenIssuer,
            getMemberCredentialUseCase,
            new OutboxDomainEventPublisher(
                saveEventOutboxPort,
                saveEventOutboxPort,
                new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules()),
                Tracer.NOOP
            )
        );
        EmailVerification persisted = EmailVerification.builder()
            .email(EMAIL)
            .code("123456")
            .token("token")
            .purpose(EmailVerificationPurpose.REGISTER)
            .build();
        ReflectionTestUtils.setField(persisted, "id", 1L);
        given(getMemberCredentialUseCase.existsByEmail(EMAIL)).willReturn(false);
        given(loadEmailVerificationPort.findLatestSentByEmail(EMAIL)).willReturn(Optional.empty());
        given(saveEmailVerificationPort.save(org.mockito.ArgumentMatchers.any(EmailVerification.class)))
            .willReturn(persisted);

        Long sessionId = service.createEmailVerificationSession(EMAIL, EmailVerificationPurpose.REGISTER);

        assertThat(sessionId).isEqualTo(1L);
        assertThat(saveEventOutboxPort.saved).hasSize(1);
        EventOutbox outbox = saveEventOutboxPort.saved.getFirst();
        assertThat(outbox.getEventType()).isEqualTo("authentication.email.verification.requested");
        assertThat(outbox.getEventClass()).isEqualTo(SendVerificationEmailEvent.class.getName());
        assertThat(outbox.getPayload()).contains("\"email\":\"alice@example.com\"");
    }

    private static class FakeSaveEventOutboxPort implements SaveEventOutboxPort, LoadEventOutboxPort {

        private final List<EventOutbox> saved = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
            saved.add(eventOutbox);
        }

        @Override
        public boolean saveIfAbsent(EventOutbox eventOutbox) {
            if (saved.stream().anyMatch(savedOutbox -> savedOutbox.getEventId().equals(eventOutbox.getEventId()))) {
                return false;
            }
            save(eventOutbox);
            return true;
        }

        @Override
        public void saveAll(Collection<EventOutbox> eventOutboxes) {
            saved.addAll(eventOutboxes);
        }

        @Override
        public Optional<EventOutbox> findByEventId(UUID eventId) {
            return saved.stream()
                .filter(outbox -> outbox.getEventId().equals(eventId))
                .findFirst();
        }

        @Override
        public List<EventOutbox> listPublishable(int limit, Instant now) {
            return saved.stream().limit(limit).toList();
        }
    }
}
