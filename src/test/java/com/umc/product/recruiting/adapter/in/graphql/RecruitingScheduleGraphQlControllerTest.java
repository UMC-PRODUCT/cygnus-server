package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingInterviewSchedulesUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleBoardUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@GraphQlTest(RecruitingScheduleGraphQlController.class)
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
class RecruitingScheduleGraphQlControllerTest {

    private static final Long REQUESTER_ID = 40L;

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;

    @MockitoBean
    GetRecruitingInterviewScheduleUseCase getInterviewScheduleUseCase;

    @MockitoBean
    ManageRecruitingInterviewScheduleUseCase manageInterviewScheduleUseCase;

    @MockitoBean
    ManageRecruitingInterviewSessionUseCase manageInterviewSessionUseCase;

    @MockitoBean
    GetRecruitingInterviewSessionUseCase getInterviewSessionUseCase;

    @MockitoBean
    GetRecruitingInterviewScheduleBoardUseCase getInterviewScheduleBoardUseCase;

    @MockitoBean
    ConfirmRecruitingInterviewSchedulesUseCase confirmInterviewSchedulesUseCase;

    @MockitoBean
    SkipRecruitingInterviewUseCase skipInterviewUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @MockitoBean
    CurrentMemberProvider currentMemberProvider;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("일정 확정 Mutation은 Instant와 CurrentMember를 public UseCase에 전달한다")
    void 일정_확정_Mutation은_Instant와_CurrentMember를_public_UseCase에_전달한다() {
        graphQlTester.document("""
                mutation {
                  confirmRecruitingInterviewSchedule(
                    applicationId: 20,
                    input: {
                      sessionId: 10,
                      startsAt: "2026-08-11T00:00:00Z",
                      endsAt: "2026-08-11T01:00:00Z",
                      location: "회의실 A",
                      contactSnapshot: "운영진 문의"
                    }
                  )
                }
                """)
            .execute()
            .path("confirmRecruitingInterviewSchedule")
            .entity(Boolean.class)
            .isEqualTo(true);

        ArgumentCaptor<ConfirmRecruitingInterviewScheduleCommand> captor =
            ArgumentCaptor.forClass(ConfirmRecruitingInterviewScheduleCommand.class);
        then(manageInterviewScheduleUseCase).should().confirm(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(captor.getValue().sessionId()).isEqualTo(10L);
        assertThat(captor.getValue().startsAt()).isEqualTo(Instant.parse("2026-08-11T00:00:00Z"));
    }

    @Test
    @DisplayName("면접 가능 시간 제출 Mutation은 모든 times와 CurrentMember를 command로 전달한다")
    void 면접_가능_시간_제출_Mutation은_모든_times와_CurrentMember를_command로_전달한다() {
        // Given
        Instant firstTime = Instant.parse("2026-08-11T00:00:00Z");
        Instant secondTime = Instant.parse("2026-08-11T01:00:00Z");

        // When
        graphQlTester.document("""
                mutation {
                  submitRecruitingInterviewAvailability(
                    applicationId: 20,
                    input: { times: ["2026-08-11T00:00:00Z", "2026-08-11T01:00:00Z"] }
                  )
                }
                """)
            .execute()
            .path("submitRecruitingInterviewAvailability")
            .entity(Boolean.class)
            .isEqualTo(true);

        // Then
        ArgumentCaptor<SubmitRecruitingInterviewAvailabilityCommand> captor =
            ArgumentCaptor.forClass(SubmitRecruitingInterviewAvailabilityCommand.class);
        then(manageInterviewScheduleUseCase).should().submitAvailability(captor.capture());
        assertThat(captor.getValue())
            .extracting(
                SubmitRecruitingInterviewAvailabilityCommand::applicationId,
                SubmitRecruitingInterviewAvailabilityCommand::requesterMemberId,
                SubmitRecruitingInterviewAvailabilityCommand::times
            )
            .containsExactly(20L, REQUESTER_ID, List.of(firstTime, secondTime));
    }

    @Test
    @DisplayName("면접 가능 시간 제출 Mutation은 times 누락을 실행 전에 거부한다")
    void 면접_가능_시간_제출_Mutation은_times_누락을_실행_전에_거부한다() {
        // Given
        String mutation = """
            mutation {
              submitRecruitingInterviewAvailability(applicationId: 20, input: {})
            }
            """;

        // When
        GraphQlTester.Response response = graphQlTester.document(mutation).execute();

        // Then
        response.errors().satisfy(errors -> assertThat(errors).hasSize(1));
        then(manageInterviewScheduleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("면접 가능 시간 제출 Mutation은 times의 null 요소를 실행 전에 거부한다")
    void 면접_가능_시간_제출_Mutation은_times의_null_요소를_실행_전에_거부한다() {
        // Given
        String mutation = """
            mutation {
              submitRecruitingInterviewAvailability(
                applicationId: 20,
                input: { times: ["2026-08-11T00:00:00Z", null] }
              )
            }
            """;

        // When
        GraphQlTester.Response response = graphQlTester.document(mutation).execute();

        // Then
        response.errors().satisfy(errors -> assertThat(errors).hasSize(1));
        then(manageInterviewScheduleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("면접 가능 시간 제출 Mutation은 빈 times를 Bean Validation으로 거부한다")
    void 면접_가능_시간_제출_Mutation은_빈_times를_Bean_Validation으로_거부한다() {
        // Given
        String mutation = """
            mutation {
              submitRecruitingInterviewAvailability(applicationId: 20, input: { times: [] })
            }
            """;

        // When
        GraphQlTester.Response response = graphQlTester.document(mutation).execute();

        // Then
        response.errors().satisfy(errors -> assertThat(errors).hasSize(1));
        then(manageInterviewScheduleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("면접 가능 시간 제출 Mutation은 service의 RECRUITING-0413 오류 코드를 extensions에 유지한다")
    void 면접_가능_시간_제출_Mutation은_service의_RECRUITING_0413_오류_코드를_extensions에_유지한다() {
        // Given
        willThrow(new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_PERIOD))
            .given(manageInterviewScheduleUseCase)
            .submitAvailability(any());

        // When
        GraphQlTester.Response response = graphQlTester.document("""
                mutation {
                  submitRecruitingInterviewAvailability(
                    applicationId: 20,
                    input: { times: ["2026-08-11T00:00:00Z"] }
                  )
                }
                """)
            .execute();

        // Then
        response.errors().satisfy(errors -> {
            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().getExtensions())
                .containsEntry("code", RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_PERIOD.getCode());
        });
        then(getInterviewScheduleUseCase).shouldHaveNoInteractions();
    }

}
