package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;

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
        assertThat(captor.getValue().startsAt()).isEqualTo(Instant.parse("2026-08-11T00:00:00Z"));
    }

    @Test
    @DisplayName("면접 생략 Mutation은 input 생략 시 reason 없는 command를 전달한다")
    void 면접_생략_Mutation은_기본_input을_사용한다() {
        given(getApplicationQueryUseCase.isApplicationBelongsToSeason(20L, 10L)).willReturn(true);

        graphQlTester.document("""
                mutation {
                  skipRecruitingInterview(seasonId: 10, applicationId: 20)
                }
                """)
            .execute()
            .path("skipRecruitingInterview")
            .entity(Boolean.class)
            .isEqualTo(true);

        then(skipInterviewUseCase).should().skip(org.mockito.ArgumentMatchers.argThat(command ->
            command.applicationId().equals(20L)
                && command.skippedByMemberId().equals(REQUESTER_ID)
                && command.reason() == null
        ));
    }
}
