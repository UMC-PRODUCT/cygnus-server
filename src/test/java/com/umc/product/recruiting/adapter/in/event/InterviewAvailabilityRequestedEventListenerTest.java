package com.umc.product.recruiting.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.config.notification.NotificationTransport;
import com.umc.product.global.config.notification.NotificationTransportProperties;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.notification.application.port.in.SendEmailUseCase;
import com.umc.product.notification.application.port.in.dto.SendHtmlEmailCommand;
import com.umc.product.recruiting.application.event.InterviewAvailabilityRequestedEvent;
import com.umc.product.recruiting.application.event.RecruitingInterviewEmailRequestedIntegrationEvent;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewMailDeliveryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewMailDeliveryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewRequestMailInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.enums.RecruitingMailDeliveryStatus;

@ExtendWith(MockitoExtension.class)
class InterviewAvailabilityRequestedEventListenerTest {

    @Mock
    SendEmailUseCase sendEmailUseCase;
    @Mock
    ManageRecruitingInterviewMailDeliveryUseCase mailDeliveryUseCase;
    @Mock
    GetRecruitingInterviewMailDeliveryUseCase getMailDeliveryUseCase;
    @Mock
    DomainEventPublisher eventPublisher;

    InterviewAvailabilityRequestedEventListener sut;

    @BeforeEach
    void setUp() {
        sut = new InterviewAvailabilityRequestedEventListener(
            sendEmailUseCase,
            getMailDeliveryUseCase,
            mailDeliveryUseCase,
            Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Outbox 이벤트는 Thymeleaf HTML 메일을 보내고 성공 상태를 기록한다")
    void 이벤트는_HTML_메일을_보내고_성공_상태를_기록한다() {
        InterviewAvailabilityRequestedEvent event = event();
        given(getMailDeliveryUseCase.getRequestMail(40L)).willReturn(mailInfo(RecruitingMailDeliveryStatus.PENDING));

        sut.handle(event);

        ArgumentCaptor<SendHtmlEmailCommand> captor = ArgumentCaptor.forClass(SendHtmlEmailCommand.class);
        then(sendEmailUseCase).should().sendHtmlEmail(captor.capture());
        assertThat(captor.getValue().templateName()).isEqualTo("email/recruiting-interview-availability");
        assertThat(captor.getValue().variables()).containsAllEntriesOf(Map.of(
            "applicantName", "지원자",
            "availabilityFormId", 300L,
            "contactText", "문의 채널"
        ));
        then(mailDeliveryUseCase).should().markRequestMailSent(any(), any());
    }

    @Test
    @DisplayName("메일 실패는 민감정보를 제거한 오류를 기록하고 Outbox 재시도를 위해 예외를 전달한다")
    void 메일_실패는_오류를_기록하고_예외를_전달한다() {
        InterviewAvailabilityRequestedEvent event = event();
        RuntimeException failure = new RuntimeException("recipient=applicant@example.com");
        given(getMailDeliveryUseCase.getRequestMail(40L)).willReturn(mailInfo(RecruitingMailDeliveryStatus.PENDING));
        doThrow(failure).when(sendEmailUseCase).sendHtmlEmail(any());

        assertThatThrownBy(() -> sut.handle(event)).isSameAs(failure);

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        then(mailDeliveryUseCase).should().markRequestMailFailed(org.mockito.ArgumentMatchers.eq(40L), errorCaptor.capture());
        assertThat(errorCaptor.getValue()).doesNotContain("applicant@example.com").contains("[REDACTED]");
    }

    @Test
    @DisplayName("이미 발송된 요청 이벤트는 멱등하게 무시한다")
    void 이미_발송된_이벤트는_무시한다() {
        given(getMailDeliveryUseCase.getRequestMail(40L)).willReturn(mailInfo(RecruitingMailDeliveryStatus.SENT));

        sut.handle(event());

        then(sendEmailUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("취소된 일정의 대기 중 Outbox 이벤트는 메일을 보내지 않는다")
    void 취소된_일정의_이벤트는_무시한다() {
        given(getMailDeliveryUseCase.getRequestMail(40L)).willReturn(mailInfo(
            RecruitingMailDeliveryStatus.PENDING,
            RecruitingInterviewScheduleStatus.CANCELLED
        ));

        sut.handle(event());

        then(sendEmailUseCase).shouldHaveNoInteractions();
        then(mailDeliveryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("external 모드는 SES를 직접 호출하지 않고 채용 메일 integration event를 기록한다")
    void external_채용_메일_event_발행() {
        InterviewAvailabilityRequestedEventListener externalListener =
            new InterviewAvailabilityRequestedEventListener(
                sendEmailUseCase,
                getMailDeliveryUseCase,
                mailDeliveryUseCase,
                Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC),
                eventPublisher,
                new NotificationTransportProperties(NotificationTransport.EXTERNAL)
            );
        given(getMailDeliveryUseCase.getRequestMail(40L))
            .willReturn(mailInfo(RecruitingMailDeliveryStatus.PENDING));

        externalListener.handle(event());

        then(sendEmailUseCase).shouldHaveNoInteractions();
        then(mailDeliveryUseCase).shouldHaveNoInteractions();
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        then(eventPublisher).should().publish(captor.capture());
        assertThat(captor.getValue())
            .isInstanceOfSatisfying(RecruitingInterviewEmailRequestedIntegrationEvent.class, integrationEvent -> {
                assertThat(integrationEvent.detail().applicationId()).isEqualTo(40L);
                assertThat(integrationEvent.detail().availabilityFormId()).isEqualTo(300L);
            });
    }

    private InterviewAvailabilityRequestedEvent event() {
        return InterviewAvailabilityRequestedEvent.of(40L);
    }

    private RecruitingInterviewRequestMailInfo mailInfo(RecruitingMailDeliveryStatus status) {
        return mailInfo(status, RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED);
    }

    private RecruitingInterviewRequestMailInfo mailInfo(
        RecruitingMailDeliveryStatus status,
        RecruitingInterviewScheduleStatus scheduleStatus
    ) {
        return RecruitingInterviewRequestMailInfo.builder()
            .applicationId(40L)
            .recipientEmail("applicant@example.com")
            .applicantName("지원자")
            .availabilityFormId(300L)
            .contactText("문의 채널")
            .scheduleStatus(scheduleStatus)
            .deliveryStatus(status)
            .build();
    }
}
